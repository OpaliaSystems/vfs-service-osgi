package systems.opalia.service.vfs.frontend.impl

import com.typesafe.config.Config
import scala.concurrent.duration.FiniteDuration
import systems.opalia.commons.configuration.ConfigHelper._
import systems.opalia.commons.configuration.Reader._
import systems.opalia.commons.net.Uri


final class BundleConfig(config: Config) {

  val cipherSecret: String = config.as[String]("vfs.cipher-secret")
  val macSecret: String = config.as[String]("vfs.mac-secret")

  val cleanupInterval: FiniteDuration = config.as[FiniteDuration]("vfs.cleanup-interval")

  val fsSettings: List[BundleConfig.FileSystemSetting] =
    config.as[List[Config]]("vfs.file-systems")
      .map {
        source =>

          val name = source.as[String]("name")
          val uri1 = source.as[Uri]("uri")
          val disposable = source.as[Boolean]("disposable")
          val encryption = source.as[Boolean]("encryption")
          val compression = source.as[Boolean]("compression")

          val uri2 =
            if (uri1.scheme == "file") {

              val path = uri1.path.toNioPath.toAbsolutePath.normalize

              uri1.withPath(Uri.Path(path.toString, Uri.Path.Type.Regular))

            } else
              uri1

          BundleConfig.FileSystemSetting(
            name,
            uri2,
            disposable,
            encryption,
            compression
          )
      }
}

object BundleConfig {

  case class FileSystemSetting(name: String, uri: Uri, disposable: Boolean, encryption: Boolean, compression: Boolean)

}
