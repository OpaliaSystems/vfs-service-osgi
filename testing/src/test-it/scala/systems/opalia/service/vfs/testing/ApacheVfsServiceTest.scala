package systems.opalia.service.vfs.testing

import systems.opalia.bootloader.ArtifactNameBuilder._
import systems.opalia.bootloader.BootloaderBuilder
import systems.opalia.service.vfs.testing.helper.VfsServiceTest


class ApacheVfsServiceTest
  extends VfsServiceTest {

  def testName: String =
    "vfs-apachevfs-service-test"

  def configure(bootBuilder: BootloaderBuilder): BootloaderBuilder = {

    bootBuilder
      .withBundle("systems.opalia" %% "logging-impl-logback" % "1.0.0")
      .withBundle("systems.opalia" %% "vfs-backend-api" % "1.0.0")
      .withBundle("systems.opalia" %% "vfs-backend-impl-apachevfs" % "1.0.0")
      .withBundle("systems.opalia" %% "vfs-impl-frontend" % "1.0.0")
  }
}
