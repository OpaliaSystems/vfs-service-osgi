package systems.opalia.service.vfs.frontend.impl

import java.io._
import java.nio.file.Path
import java.time.Instant
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import play.api.libs.json._
import systems.opalia.commons.codec.Hex
import systems.opalia.commons.crypto._
import systems.opalia.commons.identifier.ObjectId
import systems.opalia.commons.io.FileUtils
import systems.opalia.interfaces.logging.Logger
import systems.opalia.interfaces.vfs.{Checksum, DataCorruptionException, FileContent}
import systems.opalia.service.vfs.backend.api.SimpleFileSystem


final class FileContentImpl(config: BundleConfig,
                            logger: Logger,
                            cipher: Cipher,
                            fs: SimpleFileSystem,
                            rootFilePath: Path,
                            objectId: ObjectId,
                            delete: () => Boolean)
  extends FileContent {

  private val metaFilePath = rootFilePath.resolve("meta")
  private val lobFilePath = rootFilePath.resolve("lob")

  private val contentNode =
    FileUtils.using(fs.read(metaFilePath)) {
      inputStream =>

        Json.parse(inputStream)
    }

  def id: Seq[Byte] =
    objectId

  def fileName: String =
    (contentNode \ "properties" \ "file_name").as[String]

  def fileSize: Long =
    (contentNode \ "properties" \ "file_size").as[Long]

  def objectSize: Long =
    fs.size(metaFilePath) + fs.size(lobFilePath)

  def contentType: String =
    (contentNode \ "properties" \ "content_type").as[String]

  def timestamp: Instant =
    Instant.ofEpochMilli((contentNode \ "properties" \ "timestamp").as[Long])

  def checksum: Checksum = {

    val useEncryption = (contentNode \ "properties" \ "encryption").as[Boolean]
    val algorithm = (contentNode \ "properties" \ "checksum_algorithm").as[String]
    val checksumValue1 = Hex.decode((contentNode \ "properties" \ "checksum_value").as[String])

    val checksumValue2 =
      if (useEncryption)
        cipher.decrypt(checksumValue1)
      else
        checksumValue1

    Checksum(algorithm, checksumValue2)
  }

  def read(checkIntegrity: Boolean): InputStream = {

    createInputStream(checkIntegrity, validationOnly = false)
  }

  def validate(failIfFalse: Boolean): Boolean = {

    val signature = Hex.decode((contentNode \ "signature").as[String])
    val propertyNodeBytes = Json.toBytes((contentNode \ "properties").as[JsValue])

    try {

      FileUtils.using(createInputStream(checkIntegrity = true, validationOnly = true)) {
        inputStream =>

          Iterator.continually(inputStream.read()).takeWhile(x => x != -1).length
      }

      if (!Digest(Digest.Algorithm.SHA256, config.macSecret).sign(propertyNodeBytes).sameElements(signature))
        throw new DataCorruptionException("Cannot validate meta data integrity with signature.")

      true

    } catch {

      case e: DataCorruptionException =>

        if (failIfFalse)
          throw e

        false
    }
  }

  private def createInputStream(checkIntegrity: Boolean, validationOnly: Boolean): InputStream = {

    val disposable = (contentNode \ "properties" \ "disposable").as[Boolean]
    val encryption = (contentNode \ "properties" \ "encryption").as[Boolean]
    val compression = (contentNode \ "properties" \ "compression").as[Boolean]

    logger.debug(s"Start stream from: $rootFilePath")

    val inputStream1 =
      fs.read(lobFilePath)

    val inputStream2 =
      if (encryption)
        cipher.decrypt(inputStream1)
      else
        inputStream1

    val inputStream3 =
      if (compression)
        new BZip2CompressorInputStream(inputStream2)
      else
        inputStream2

    if (!checkIntegrity)
      inputStream3
    else {

      val currentChecksum = checksum
      val digesterAlgorithm = Digest(Digest.Algorithm.withName(currentChecksum.algorithm))

      val pipedInputStream = new PipedInputStream()
      val pipedOutputStream = new PipedOutputStream(pipedInputStream)
      val inputStreamDigester = digesterAlgorithm.sign(pipedInputStream)

      new InputStream {

        override def read(): Int = {

          val result = inputStream3.read()

          if (result >= 0) {

            pipedOutputStream.write(result)
            inputStreamDigester.readAll(onlyIfAvailable = true)
          }

          result
        }

        override def read(bytes: Array[Byte]): Int = {

          val result = inputStream3.read(bytes)

          if (result > 0) {

            pipedOutputStream.write(bytes, 0, result)
            inputStreamDigester.readAll(onlyIfAvailable = true)
          }

          result
        }

        override def read(bytes: Array[Byte], offset: Int, length: Int): Int = {

          val result = inputStream3.read(bytes, offset, length)

          if (result > 0) {

            pipedOutputStream.write(bytes, offset, result)
            inputStreamDigester.readAll(onlyIfAvailable = true)
          }

          result
        }

        override def available(): Int = {

          inputStream3.available()
        }

        override def close(): Unit = {

          inputStream3.close()
          pipedOutputStream.flush()
          pipedOutputStream.close()

          inputStreamDigester.readAll(onlyIfAvailable = false)

          doFinal()
        }

        private def doFinal(): Unit = {

          val checksumValue = inputStreamDigester.doFinal()

          if (!checksum.value.sameElements(checksumValue))
            throw new DataCorruptionException("Cannot validate data integrity with checksum after reading.")

          if (!validationOnly && disposable) {

            if (!delete())
              throw new IOException("Could not delete disposable file.")
          }
        }
      }
    }
  }
}
