package systems.opalia.service.vfs.frontend.impl

import java.time.Instant
import java.util.concurrent.{CopyOnWriteArrayList, Executors, TimeUnit}
import scala.collection.JavaConverters._
import systems.opalia.commons.crypto._
import systems.opalia.interfaces.logging.LoggingService
import systems.opalia.interfaces.soa.Bootable
import systems.opalia.interfaces.vfs._
import systems.opalia.service.vfs.backend.api.SimpleFileSystemFactory


final class VfsServiceBootable(config: BundleConfig,
                               loggingService: LoggingService,
                               fsFactory: SimpleFileSystemFactory)
  extends VfsService
    with Bootable[Unit, Unit] {

  private val logger = loggingService.newLogger(classOf[VfsService].getName)
  private val scheduler = Executors.newScheduledThreadPool(4)
  private val cipher = Cipher(CipherSettings.AES_EAX, config.cipherSecret)
  private val temporaryCache: CopyOnWriteArrayList[FileObject] = new CopyOnWriteArrayList[FileObject]()

  def getFileSystem(fsName: String): FileSystem = {

    val fsSetting =
      config.fsSettings.find(_.name == fsName)
        .getOrElse(throw new IllegalArgumentException(s"Cannot find file system setting $fsName in configuration."))

    new FileSystemImpl(config, logger, cipher, fsSetting, fsFactory, temporaryCache)
  }

  protected def setupTask(): Unit = {

    scheduler.scheduleAtFixedRate(
      () => {

        val now = Instant.now.toEpochMilli

        temporaryCache.asScala.foreach {
          fileObject =>

            if (fileObject.exists() && !fileObject.committed()) {

              val timestamp = fileObject.fetchContent().timestamp.toEpochMilli

              if (now - timestamp >= config.cleanupInterval.toMillis)
                fileObject.delete()
            }

            temporaryCache.remove(fileObject)
        }
      },
      1,
      1,
      TimeUnit.MINUTES)
  }

  protected def shutdownTask(): Unit = {

    scheduler.shutdown()

    temporaryCache.asScala.foreach {
      fileObject =>

        if (fileObject.exists() && !fileObject.committed())
          fileObject.delete()
    }

    temporaryCache.clear()
  }
}
