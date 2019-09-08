package systems.opalia.service.vfs.backend.api

import java.io.{InputStream, OutputStream}
import java.nio.file.Path


trait SimpleFileSystem {

  def exists(path: Path): Boolean

  def delete(path: Path): Boolean

  def create(path: Path): OutputStream

  def read(path: Path): InputStream

  def size(path: Path): Long
}
