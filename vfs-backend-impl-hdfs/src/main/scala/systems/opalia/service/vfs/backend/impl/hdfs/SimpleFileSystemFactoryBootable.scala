package systems.opalia.service.vfs.backend.impl.hdfs

import org.apache.hadoop.fs.FileSystem
import systems.opalia.interfaces.logging.LoggingService
import systems.opalia.interfaces.soa.Bootable
import systems.opalia.service.vfs.backend.api.{SimpleFileSystem, SimpleFileSystemFactory}


final class SimpleFileSystemFactoryBootable(config: BundleConfig,
                                            loggingService: LoggingService)
  extends SimpleFileSystemFactory
    with Bootable[Unit, Unit] {

  sys.props("hadoop.home.dir") = config.hadoopHomeDir.toString

  private val logger = loggingService.newLogger(classOf[SimpleFileSystemImpl].getName)
  private val hdfs = FileSystem.get(config.hdfsConfiguration)

  def newSimpleFileSystem(uri: String): SimpleFileSystem = {

    new SimpleFileSystemImpl(logger, hdfs)
  }

  protected def setupTask(): Unit = {
  }

  protected def shutdownTask(): Unit = {

    hdfs.close()
  }
}
