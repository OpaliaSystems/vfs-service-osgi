package systems.opalia.service.vfs.backend.api


trait SimpleFileSystemFactory {

  def newSimpleFileSystem(uri: String): SimpleFileSystem
}
