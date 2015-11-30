lazy val root = (project in file(".")).
  settings(
    name := "SparkHack",
    version := "1.0",
    scalaVersion := "2.10.4"
  )

libraryDependencies ++= {
  Seq(
    "org.apache.spark" % "spark-core_2.10"  %  "1.5.2" % Provided,
    "org.apache.spark" % "spark-sql_2.10"  %  "1.5.2" % Provided,
    "org.apache.spark" % "spark-mllib_2.10"  %  "1.5.2" % Provided,
    "joda-time"  %  "joda-time"  % "2.8.2",
    "org.joda" % "joda-convert" % "1.7"
  )
}

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", _*) => MergeStrategy.discard
  case _ => MergeStrategy.first
}