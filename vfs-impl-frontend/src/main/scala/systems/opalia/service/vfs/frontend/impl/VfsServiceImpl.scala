package systems.opalia.service.vfs.frontend.impl

import org.osgi.framework.BundleContext
import org.osgi.service.component.annotations._
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import systems.opalia.interfaces.logging.LoggingService
import systems.opalia.interfaces.soa.ConfigurationService
import systems.opalia.interfaces.soa.osgi.ServiceManager
import systems.opalia.interfaces.vfs.{FileSystem, VfsService}
import systems.opalia.service.vfs.backend.api.SimpleFileSystemFactory


@Component(service = Array(classOf[VfsService]), immediate = true)
class VfsServiceImpl
  extends VfsService {

  private val serviceManager: ServiceManager = new ServiceManager()
  private var bootable: VfsServiceBootable = _

  @Reference
  private var loggingService: LoggingService = _

  @Reference
  private var fileSystemFactory: SimpleFileSystemFactory = _

  @Activate
  def start(bundleContext: BundleContext): Unit = {

    val configurationService = serviceManager.getService(bundleContext, classOf[ConfigurationService])
    val config = new BundleConfig(configurationService.getConfiguration)

    bootable =
      new VfsServiceBootable(
        config,
        loggingService,
        fileSystemFactory
      )

    bootable.setup()
    Await.result(bootable.awaitUp(), Duration.Inf)
  }

  @Deactivate
  def stop(bundleContext: BundleContext): Unit = {

    bootable.shutdown()
    Await.result(bootable.awaitUp(), Duration.Inf)

    serviceManager.ungetServices(bundleContext)

    bootable = null
  }

  def getFileSystem(fsName: String): FileSystem =
    bootable.getFileSystem(fsName)
}
