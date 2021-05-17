package systems.opalia.service.vfs.testing.helper

import com.typesafe.config._
import java.nio.file.{Files, Paths}
import org.osgi.framework.BundleContext
import org.scalatest.flatspec._
import org.scalatest.matchers.should._
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import systems.opalia.bootloader.ArtifactNameBuilder._
import systems.opalia.bootloader.{Bootloader, BootloaderBuilder}
import systems.opalia.commons.io.FileUtils
import systems.opalia.interfaces.soa.osgi.ServiceManager


abstract class AbstractTest
  extends AnyFlatSpec
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with Matchers {

  private var bootloader: Bootloader = _
  protected val serviceManager = new ServiceManager()

  def testName: String

  protected def bundleContext: BundleContext =
    bootloader.bundleContext

  protected def configure(bootBuilder: BootloaderBuilder): BootloaderBuilder

  protected def init(config: Config): Unit

  override final def beforeAll(): Unit = {

    val testPath = Paths.get("./tmp").resolve(testName)

    if (Files.exists(testPath))
      FileUtils.deleteRecursively(testPath)

    val config =
      ConfigFactory.load(
        s"$testName.conf",
        ConfigParseOptions.defaults(),
        ConfigResolveOptions.defaults().setAllowUnresolved(true)
      )
        .resolveWith(ConfigFactory.parseString(
          s"""
             |base-path = $testPath
           """.stripMargin))

    val builder =
      BootloaderBuilder.newBootloaderBuilder(config)
        .withCacheDirectory(testPath.resolve("felix-cache").normalize())
        .withBundle("org.osgi" % "org.osgi.service.log" % "1.5.0")
        .withBundle("org.osgi" % "org.osgi.util.tracker" % "1.5.2")
        .withBundle("org.osgi" % "org.osgi.util.promise" % "1.1.1")
        .withBundle("org.osgi" % "org.osgi.util.function" % "1.1.0")
        .withBundle("org.osgi" % "org.osgi.util.pushstream" % "1.0.1")
        .withBundle("org.osgi" % "org.osgi.service.component" % "1.4.0")
        .withBundle("org.apache.felix" % "org.apache.felix.scr" % "2.1.16")

    bootloader = configure(builder).newBootloader()

    bootloader.setup()

    Await.result(bootloader.awaitUp(), Duration.Inf)

    init(config)
  }

  override final def afterAll(): Unit = {

    serviceManager.unregisterServices()
    serviceManager.ungetServices(bootloader.bundleContext)

    bootloader.shutdown()

    Await.result(bootloader.awaitDown(), Duration.Inf)
  }
}
