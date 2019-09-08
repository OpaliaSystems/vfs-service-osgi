package systems.opalia.service.vfs.backend.impl.apachevfs

import org.osgi.framework.BundleContext
import org.osgi.service.component.annotations._
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import systems.opalia.interfaces.logging.LoggingService
import systems.opalia.interfaces.soa.ConfigurationService
import systems.opalia.interfaces.soa.osgi.ServiceManager
import systems.opalia.service.vfs.backend.api.{SimpleFileSystem, SimpleFileSystemFactory}


@Component(service = Array(classOf[SimpleFileSystemFactory]), immediate = false)
class SimpleFileSystemFactoryImpl
  extends SimpleFileSystemFactory {

  private val serviceManager: ServiceManager = new ServiceManager()
  private var bootable: SimpleFileSystemFactoryBootable = _

  @Reference
  private var loggingService: LoggingService = _

  @Activate
  def start(bundleContext: BundleContext): Unit = {

    val configurationService = serviceManager.getService(bundleContext, classOf[ConfigurationService])
    val config = new BundleConfig(configurationService.getConfiguration)

    bootable =
      new SimpleFileSystemFactoryBootable(
        config,
        loggingService
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

  def newSimpleFileSystem(uri: String): SimpleFileSystem =
    bootable.newSimpleFileSystem(uri)
}
