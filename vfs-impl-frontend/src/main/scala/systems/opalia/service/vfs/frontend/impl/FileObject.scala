package systems.opalia.service.vfs.frontend.impl

import java.io._
import java.nio.file.Path
import java.time.Instant
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import play.api.libs.json.Json
import scala.collection.immutable.ListMap
import systems.opalia.commons.codec.Hex
import systems.opalia.commons.crypto._
import systems.opalia.commons.identifier.ObjectId
import systems.opalia.commons.io.FileUtils
import systems.opalia.commons.json.JsonAstTransformer
import systems.opalia.interfaces.json.JsonAst
import systems.opalia.interfaces.logging.Logger
import systems.opalia.interfaces.rendering.Renderer
import systems.opalia.interfaces.vfs.{Checksum, DataCorruptionException, FileContent}
import systems.opalia.service.vfs.backend.api.SimpleFileSystem


final class FileObject(config: BundleConfig,
                       logger: Logger,
                       cipher: Cipher,
                       fsSetting: BundleConfig.FileSystemSetting,
                       fs: SimpleFileSystem,
                       rootFilePath: Path,
                       id: ObjectId) {

  private val metaFilePath = rootFilePath.resolve("meta")
  private val lobFilePath = rootFilePath.resolve("lob")
  private val readyFilePath = rootFilePath.resolve("ready")
  private val commitFilePath = rootFilePath.resolve("commit")

  def commit(): Unit = {

    if (!exists())
      throw new IOException("Cannot commit a non existing file object.")

    createStatusFile(commitFilePath)
  }

  def committed(): Boolean = {

    fs.exists(readyFilePath) && fs.exists(commitFilePath)
  }

  def exists(): Boolean = {

    fs.exists(readyFilePath)
  }

  def delete(): Boolean = {

    exists() && {

      val result =
        fs.delete(readyFilePath) & fs.delete(metaFilePath) & fs.delete(lobFilePath)

      fs.delete(commitFilePath)
      fs.delete(rootFilePath)

      result
    }
  }

  def create(fileName: String,
             contentType: String,
             checksum: Option[Checksum]): OutputStream = {

    createOutputStream(fileName, contentType, checksum)
  }

  def fetchContent(): FileContent = {

    if (!exists())
      throw new IOException(s"File object with path $rootFilePath does not exist.")

    new FileContentImpl(config, logger, cipher, fs, rootFilePath, id, () => delete())
  }

  private def createStatusFile(path: Path): Unit = {

    if (!fs.exists(path)) {

      FileUtils.using(fs.create(path)) {
        outputStream =>

          outputStream.write(Instant.now.toEpochMilli.toString.getBytes(Renderer.appDefaultCharset))
          outputStream.flush()
      }
    }
  }

  private def createOutputStream(fileName: String,
                                 contentType: String,
                                 checksum: Option[Checksum]): OutputStream = {

    if (exists())
      throw new IOException("File object already exists.")

    val digesterAlgorithm =
      checksum
        .map(x => Digest(Digest.Algorithm.withName(x.algorithm)))
        .getOrElse(Digest(Digest.Algorithm.SHA256))

    val pipedInputStream = new PipedInputStream()
    val pipedOutputStream = new PipedOutputStream(pipedInputStream)
    val inputStreamDigester = digesterAlgorithm.sign(pipedInputStream)

    logger.debug(s"Start stream to: $rootFilePath")

    val outputStream1 =
      fs.create(lobFilePath)

    val outputStream2 =
      if (fsSetting.compression)
        new BZip2CompressorOutputStream(outputStream1)
      else
        outputStream1

    val outputStream3 =
      if (fsSetting.encryption)
        cipher.encrypt(outputStream2)
      else
        outputStream2

    var size = 0L

    new OutputStream {

      override def write(value: Int): Unit = {

        outputStream3.write(value)
        pipedOutputStream.write(value)

        inputStreamDigester.readAll(onlyIfAvailable = true)

        if (value >= 0)
          size += 1
      }

      override def write(bytes: Array[Byte]): Unit = {

        outputStream3.write(bytes)
        pipedOutputStream.write(bytes)

        inputStreamDigester.readAll(onlyIfAvailable = true)

        size += bytes.length
      }

      override def write(bytes: Array[Byte], offset: Int, length: Int): Unit = {

        outputStream3.write(bytes, offset, length)
        pipedOutputStream.write(bytes, offset, length)

        inputStreamDigester.readAll(onlyIfAvailable = true)

        size += length
      }

      override def flush(): Unit = {

        outputStream3.flush()
        pipedOutputStream.flush()

        inputStreamDigester.readAll(onlyIfAvailable = true)
      }

      override def close(): Unit = {

        outputStream3.flush()
        pipedOutputStream.flush()

        outputStream3.close()
        pipedOutputStream.close()

        inputStreamDigester.readAll(onlyIfAvailable = false)

        doFinal()
      }

      private def doFinal(): Unit = {

        val checksumValue1 = inputStreamDigester.doFinal()

        if (checksum.exists(x => !x.value.sameElements(checksumValue1)))
          throw new DataCorruptionException("Cannot validate data integrity with checksum after writing.")

        val checksumValue2 =
          if (fsSetting.encryption)
            cipher.encrypt(checksumValue1)
          else
            checksumValue1.toIndexedSeq

        val propertyNode =
          JsonAst.JsonObject(ListMap(
            "timestamp" -> JsonAst.JsonNumberLong(Instant.now().toEpochMilli),
            "file_name" -> JsonAst.JsonString(fileName),
            "file_size" -> JsonAst.JsonNumberLong(size),
            "content_type" -> JsonAst.JsonString(contentType),
            "checksum_algorithm" -> JsonAst.JsonString(digesterAlgorithm.algorithm.toString),
            "checksum_value" -> JsonAst.JsonString(Hex.encode(checksumValue2)),
            "disposable" -> JsonAst.JsonBoolean(fsSetting.disposable),
            "encryption" -> JsonAst.JsonBoolean(fsSetting.encryption),
            "compression" -> JsonAst.JsonBoolean(fsSetting.compression)
          ))

        val propertyNodeBytes = Json.toBytes(JsonAstTransformer.toPlayJson(propertyNode))
        val signature = Digest(Digest.Algorithm.SHA256, config.macSecret).sign(propertyNodeBytes)

        val contentNode =
          JsonAst.JsonObject(ListMap(
            "properties" -> propertyNode,
            "signature" -> JsonAst.JsonString(Hex.encode(signature))
          ))

        val contentNodeBytes = Json.toBytes(JsonAstTransformer.toPlayJson(contentNode))

        FileUtils.using(fs.create(metaFilePath)) {
          outputStream =>

            outputStream.write(contentNodeBytes)
            outputStream.flush()
        }

        createStatusFile(readyFilePath)
      }
    }
  }
}
