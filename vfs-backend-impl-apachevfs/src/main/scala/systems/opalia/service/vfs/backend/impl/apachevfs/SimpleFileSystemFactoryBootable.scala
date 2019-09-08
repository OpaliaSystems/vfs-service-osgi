package systems.opalia.service.vfs.backend.impl.apachevfs

import org.apache.commons.vfs2.VFS
import systems.opalia.interfaces.logging.LoggingService
import systems.opalia.interfaces.soa.Bootable
import systems.opalia.service.vfs.backend.api.{SimpleFileSystem, SimpleFileSystemFactory}


final class SimpleFileSystemFactoryBootable(config: BundleConfig,
                                            loggingService: LoggingService)
  extends SimpleFileSystemFactory
    with Bootable[Unit, Unit] {

  private val logger = loggingService.newLogger(classOf[SimpleFileSystemImpl].getName)
  private val fsManager = VFS.getManager

  def newSimpleFileSystem(uri: String): SimpleFileSystem = {

    new SimpleFileSystemImpl(logger, fsManager.resolveFile(uri))
  }

  protected def setupTask(): Unit = {
  }

  protected def shutdownTask(): Unit = {
  }
}
