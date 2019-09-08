package systems.opalia.service.vfs.testing.helper

import com.typesafe.config.Config
import java.io.IOException
import scala.io.Source
import systems.opalia.commons.identifier.ObjectId
import systems.opalia.commons.io.FileUtils
import systems.opalia.interfaces.rendering.Renderer
import systems.opalia.interfaces.vfs.VfsService


abstract class VfsServiceTest
  extends AbstractTest {

  var vfsService: VfsService = _

  val fileName = "index.html"
  val contentType = "text/html; charset=utf-8"
  val content = """<!DOCTYPE html><html><head><meta charset="utf-8"></head><body>Hello World!</body></html>"""

  def init(config: Config): Unit = {

    vfsService = serviceManager.getService(bundleContext, classOf[VfsService])
  }

  it should "be able to handle file objects correctly" in {

    val fs = vfsService.getFileSystem("asset")
    val id = new Array[Byte](ObjectId.length)

    FileUtils.using(fs.create(id, fileName, contentType)) {
      outputStream =>

        outputStream.write(content.getBytes(Renderer.appDefaultCharset))
    }

    fs.name shouldBe "asset"
    fs.committed(id) shouldBe false
    fs.exists(id) shouldBe true

    fs.commit(id)

    fs.committed(id) shouldBe true

    an[IOException] should be thrownBy fs.fetchFileContent(ObjectId.getNew)

    val fileContent = fs.fetchFileContent(id)

    val result =
      FileUtils.using(fileContent.read(checkIntegrity = true)) {
        inputStream =>

          Source.fromInputStream(inputStream, Renderer.appDefaultCharset.toString).mkString
      }

    result shouldBe content

    fileContent.fileName shouldBe fileName
    fileContent.fileSize shouldBe content.getBytes(Renderer.appDefaultCharset).length
    fileContent.contentType shouldBe contentType

    fileContent.validate(failIfFalse = false) shouldBe true

    fs.delete(id) shouldBe true
    fs.delete(id) shouldBe false
    fs.exists(id) shouldBe false
  }

  it should "be able to handle disposable file objects correctly" in {

    val fs = vfsService.getFileSystem("cache")
    val id = new Array[Byte](ObjectId.length)

    FileUtils.using(fs.create(id, fileName, contentType)) {
      outputStream =>

        outputStream.write(content.getBytes(Renderer.appDefaultCharset))
    }

    fs.name shouldBe "cache"
    fs.exists(id) shouldBe true

    val fileContent = fs.fetchFileContent(id)

    fileContent.fileName shouldBe fileName
    fileContent.fileSize shouldBe content.getBytes(Renderer.appDefaultCharset).length
    fileContent.contentType shouldBe contentType

    fileContent.validate(failIfFalse = false) shouldBe true

    val result =
      FileUtils.using(fileContent.read(checkIntegrity = true)) {
        inputStream =>

          Source.fromInputStream(inputStream, Renderer.appDefaultCharset.toString).mkString
      }

    result shouldBe content

    fs.exists(id) shouldBe false
    fs.delete(id) shouldBe false
  }
}
