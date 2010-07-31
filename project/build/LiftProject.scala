/*
import sbt._

class LiftProject(info: ProjectInfo) extends DefaultWebProject(info) {
  val scalatools_release = "Scala Tools Snapshot" at
    "http://scala-tools.org/repo-releases/"

  val liftVersion = "2.0"

  override def libraryDependencies = Set(
    "net.liftweb" % "lift-mapper" % liftVersion % "compile->default",
    "net.liftweb" % "lift-wizard" % liftVersion % "compile->default",
    "org.mortbay.jetty" % "jetty" % "6.1.24" % "test->default",
    "junit" % "junit" % "4.5" % "test->default",
    "org.scala-tools.testing" % "specs" % "1.6.2.1" % "test->default",
    "mysql" % "mysql-connector-java" % "5.1.13",
    "commons-codec" % "commons-codec" % "1.4"
    //"com.h2database" % "h2" % "1.2.121"
  ) ++ super.libraryDependencies
}
*/

import sbt._

class LiftProject(info: ProjectInfo) extends DefaultWebProject(info) {
  //val mavenLocal = "Local Maven Repository" at
  //"file://"+Path.userHome+"/.m2/repository"

  val scalatools_release = "Scala Tools Snapshot" at
  "http://scala-tools.org/repo-snapshots/"

  val liftVersion = "2.1-SNAPSHOT"

  //override def scanDirectories = Nil

  //override val jettyPort = 8081 

  override def libraryDependencies = Set(
    "net.liftweb" %% "lift-webkit" % liftVersion % "compile->default",
    "net.liftweb" %% "lift-mapper" % liftVersion % "compile->default",
    "org.mortbay.jetty" % "jetty" % "6.1.22" % "test->default",
    "junit" % "junit" % "4.5" % "test->default",
    "org.scala-tools.testing" % "specs" % "1.6.2.1" % "test->default",
    "mysql" % "mysql-connector-java" % "5.1.13",
    "commons-codec" % "commons-codec" % "1.4"
    //"com.h2database" % "h2" % "1.2.121"
  ) ++ super.libraryDependencies
}


