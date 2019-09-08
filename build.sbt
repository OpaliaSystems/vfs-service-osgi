
val mScalaVersion = "2.12.13"
val mInterfacesVersion = "1.0.0"
val mCommonsVersion = "1.0.0"
val mBootloaderVersion = "1.0.0"
val mCrossVersion = """^(\d+\.\d+)""".r.findFirstIn(mScalaVersion).get

val exclusionRules = Seq(
  ExclusionRule("org.scala-lang", "scala-library"),
  ExclusionRule("org.scala-lang", "scala-reflect"),
  ExclusionRule("org.scala-lang", "scala-compiler"),
  ExclusionRule("com.typesafe", "config"),
  ExclusionRule("systems.opalia", s"interfaces_$mCrossVersion"),
  ExclusionRule("org.osgi", "org.osgi.core"),
  ExclusionRule("org.osgi", "org.osgi.service.component"),
  ExclusionRule("org.osgi", "org.osgi.compendium")
)

def commonSettings: Seq[Setting[_]] = {

  Seq(
    organizationName := "Opalia Systems",
    organizationHomepage := Some(url("https://opalia.systems")),
    organization := "systems.opalia",
    homepage := Some(url("https://github.com/OpaliaSystems/opalia-service-vfs")),
    version := "1.0.0",
    scalaVersion := mScalaVersion
  )
}

lazy val `testing` =
  (project in file("testing"))
    .settings(

      name := "testing",

      commonSettings,

      parallelExecution in ThisBuild := false,

      libraryDependencies ++= Seq(
        "systems.opalia" %% "interfaces" % mInterfacesVersion,
        "systems.opalia" %% "commons" % mCommonsVersion,
        "systems.opalia" %% "bootloader" % mBootloaderVersion,
        "org.scalatest" %% "scalatest" % "3.2.5" % "test"
      )
    )

lazy val `vfs-backend-api` =
  (project in file("vfs-backend-api"))
    .settings(

      name := "vfs-backend-api",

      description := "The project provides an API for virtual file system backends.",

      commonSettings,

      bundleSettings,

      OsgiKeys.importPackage ++= Seq(
        "scala.*",
        "com.typesafe.config.*",
        "systems.opalia.interfaces.*"
      ),

      OsgiKeys.exportPackage ++= Seq(
        "systems.opalia.service.vfs.backend.api.*"
      ),

      libraryDependencies ++= Seq(
        "systems.opalia" %% "interfaces" % mInterfacesVersion % "provided"
      )
    )

lazy val `vfs-impl-frontend` =
  (project in file("vfs-impl-frontend"))
    .dependsOn(`vfs-backend-api`)
    .settings(

      name := "vfs-impl-frontend",

      description := "The project provides a virtual file system implementation with exchangeable backends.",

      commonSettings,

      bundleSettings,

      OsgiKeys.privatePackage ++= Seq(
        "systems.opalia.service.vfs.frontend.impl.*"
      ),

      OsgiKeys.importPackage ++= Seq(
        "scala.*",
        "com.typesafe.config.*",
        "systems.opalia.interfaces.*",
        "systems.opalia.service.vfs.backend.api.*"
      ),

      libraryDependencies ++= Seq(
        "org.osgi" % "osgi.core" % "8.0.0" % "provided",
        "org.osgi" % "org.osgi.service.component.annotations" % "1.4.0",
        "systems.opalia" %% "interfaces" % mInterfacesVersion % "provided",
        "systems.opalia" %% "commons" % mCommonsVersion excludeAll (exclusionRules: _*),
        "org.apache.commons" % "commons-compress" % "1.20"
      )
    )

lazy val `vfs-backend-impl-apachevfs` =
  (project in file("vfs-backend-impl-apachevfs"))
    .dependsOn(`vfs-backend-api`)
    .settings(

      name := "vfs-backend-impl-apachevfs",

      description := "The project provides a virtual file system implementation based on Apache Commons VFS.",

      commonSettings,

      bundleSettings,

      OsgiKeys.privatePackage ++= Seq(
        "systems.opalia.service.vfs.backend.impl.*"
      ),

      OsgiKeys.importPackage ++= Seq(
        "scala.*",
        "com.typesafe.config.*",
        "systems.opalia.interfaces.*",
        "systems.opalia.service.vfs.backend.api.*"
      ),

      libraryDependencies ++= Seq(
        "org.osgi" % "osgi.core" % "8.0.0" % "provided",
        "org.osgi" % "org.osgi.service.component.annotations" % "1.4.0",
        "systems.opalia" %% "interfaces" % mInterfacesVersion % "provided",
        "systems.opalia" %% "commons" % mCommonsVersion excludeAll (exclusionRules: _*),
        "org.apache.commons" % "commons-vfs2" % "2.7.0",
        "org.apache.httpcomponents" % "httpclient" % "4.5.13"
      )
    )

lazy val `vfs-backend-impl-hdfs` =
  (project in file("vfs-backend-impl-hdfs"))
    .dependsOn(`vfs-backend-api`)
    .settings(

      name := "vfs-backend-impl-hdfs",

      description := "The project provides a virtual file system implementation based on Apache HDFS.",

      commonSettings,

      bundleSettings,

      OsgiKeys.privatePackage ++= Seq(
        "systems.opalia.service.vfs.backend.impl.*"
      ),

      OsgiKeys.importPackage ++= Seq(
        "scala.*",
        "com.typesafe.config.*",
        "systems.opalia.interfaces.*",
        "systems.opalia.service.vfs.backend.api.*"
      ),

      libraryDependencies ++= Seq(
        "org.osgi" % "osgi.core" % "8.0.0" % "provided",
        "org.osgi" % "org.osgi.service.component.annotations" % "1.4.0",
        "systems.opalia" %% "interfaces" % mInterfacesVersion % "provided",
        "systems.opalia" %% "commons" % mCommonsVersion excludeAll (exclusionRules: _*),
        "org.apache.hadoop" % "hadoop-common" % "3.3.0",
        "org.apache.hadoop" % "hadoop-hdfs" % "3.3.0"
      )
    )
