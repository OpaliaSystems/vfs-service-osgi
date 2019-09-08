package systems.opalia.service.vfs.backend.impl.hdfs

import com.typesafe.config.Config
import java.nio.file.Path
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{Path => HdfsPath}
import systems.opalia.commons.configuration.ConfigHelper._
import systems.opalia.commons.configuration.Reader._


final class BundleConfig(config: Config) {

  val hadoopHomeDir: Path = config.as[Path]("vfs.hdfs.hadoop-home-dir").toAbsolutePath.normalize()

  val hdfsConfiguration: Configuration = {

    val cnf = new Configuration()

    config.as[List[Path]]("vfs.hdfs.config-resources")
      .foreach(x => cnf.addResource(new HdfsPath(x.toString)))

    // TODO: fix bug (java.lang.ClassNotFoundException: org.apache.hadoop.fs.LocalFileSystem)
    //cnf.set("fs.hdfs.impl", classOf[DistributedFileSystem].getName)
    //cnf.set("fs.file.impl", classOf[LocalFileSystem].getName)

    cnf
  }
}
