package systems.opalia.service.vfs.backend.impl.hdfs

import java.io.{InputStream, OutputStream}
import java.nio.file.Path
import org.apache.hadoop.fs.{FileSystem, Path => HdfsPath}
import systems.opalia.interfaces.logging.Logger
import systems.opalia.service.vfs.backend.api.SimpleFileSystem


final class SimpleFileSystemImpl(logger: Logger, hdfs: FileSystem)
  extends SimpleFileSystem {

  def exists(path: Path): Boolean = {

    val fileObject = hdfs.resolvePath(new HdfsPath(path.toString))

    hdfs.exists(fileObject)
  }

  def delete(path: Path): Boolean = {

    val fileObject = hdfs.resolvePath(new HdfsPath(path.toString))

    hdfs.delete(fileObject, false)
  }

  def create(path: Path): OutputStream = {

    val fileObject = hdfs.resolvePath(new HdfsPath(path.toString))

    hdfs.create(fileObject)
  }

  def read(path: Path): InputStream = {

    val fileObject = hdfs.resolvePath(new HdfsPath(path.toString))

    hdfs.open(fileObject)
  }

  def size(path: Path): Long = {

    val fileObject = hdfs.resolvePath(new HdfsPath(path.toString))

    hdfs.getContentSummary(fileObject).getLength
  }
}
