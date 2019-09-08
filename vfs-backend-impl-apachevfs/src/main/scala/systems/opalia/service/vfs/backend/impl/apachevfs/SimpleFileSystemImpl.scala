package systems.opalia.service.vfs.backend.impl.apachevfs

import java.io.{InputStream, OutputStream}
import java.nio.file.Path
import org.apache.commons.vfs2.FileObject
import systems.opalia.interfaces.logging.Logger
import systems.opalia.service.vfs.backend.api.SimpleFileSystem


class SimpleFileSystemImpl(logger: Logger, rootFileObject: FileObject)
  extends SimpleFileSystem {

  def exists(path: Path): Boolean = {

    val fileObject = rootFileObject.resolveFile(path.toString)

    fileObject.exists()
  }

  def delete(path: Path): Boolean = {

    val fileObject = rootFileObject.resolveFile(path.toString)

    logger.debug(s"Delete file: ${fileObject.getName.getFriendlyURI}")

    fileObject.delete()
  }

  def create(path: Path): OutputStream = {

    val fileObject = rootFileObject.resolveFile(path.toString)

    logger.debug(s"Create stream to: ${fileObject.getName.getFriendlyURI}")

    fileObject.getContent.getOutputStream()
  }

  def read(path: Path): InputStream = {

    val fileObject = rootFileObject.resolveFile(path.toString)

    logger.debug(s"Create stream from: ${fileObject.getName}")

    fileObject.getContent.getInputStream()
  }

  def size(path: Path): Long = {

    val fileObject = rootFileObject.resolveFile(path.toString)

    fileObject.getContent.getSize
  }
}
