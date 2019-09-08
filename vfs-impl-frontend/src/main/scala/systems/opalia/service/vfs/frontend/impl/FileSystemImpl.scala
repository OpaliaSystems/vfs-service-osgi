package systems.opalia.service.vfs.frontend.impl

import java.io.OutputStream
import java.nio.file.Paths
import java.util.concurrent.CopyOnWriteArrayList
import systems.opalia.commons.crypto.Cipher
import systems.opalia.commons.identifier.ObjectId
import systems.opalia.interfaces.logging.Logger
import systems.opalia.interfaces.vfs._
import systems.opalia.service.vfs.backend.api.SimpleFileSystemFactory


class FileSystemImpl(config: BundleConfig,
                     logger: Logger,
                     cipher: Cipher,
                     fsSetting: BundleConfig.FileSystemSetting,
                     fsFactory: SimpleFileSystemFactory,
                     temporaryCache: CopyOnWriteArrayList[FileObject])
  extends FileSystem {

  private val fs = fsFactory.newSimpleFileSystem(fsSetting.uri.toString)

  def name: String =
    fsSetting.name

  private def createFileObject(id: Seq[Byte]): FileObject = {

    val oid = ObjectId.getFrom(id)
    val path = Paths.get(oid.toString)

    new FileObject(config, logger, cipher, fsSetting, fs, path, oid)
  }

  def commit(id: Seq[Byte]): Unit = {

    createFileObject(id).commit()
  }

  def committed(id: Seq[Byte]): Boolean = {

    createFileObject(id).committed()
  }

  def exists(id: Seq[Byte]): Boolean = {

    createFileObject(id).exists()
  }

  def delete(id: Seq[Byte]): Boolean = {

    createFileObject(id).delete()
  }

  def create(id: Array[Byte],
             fileName: String,
             contentType: String,
             checksum: Option[Checksum]): OutputStream = {

    val oid = ObjectId.getNew
    val fileObject = createFileObject(oid)
    val outputStream = fileObject.create(fileName, contentType, checksum)

    temporaryCache.add(fileObject)
    oid.zipWithIndex.foreach(x => id(x._2) = x._1)

    outputStream
  }

  def fetchFileContent(id: Seq[Byte]): FileContent = {

    createFileObject(id).fetchContent()
  }
}
