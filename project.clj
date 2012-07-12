(defproject pallet-hadoop-example "0.0.3-beta.2-SNAPSHOT"
  :description "Example project for running Hadoop on Pallet."
  :repositories {"sonatype"
                 "http://oss.sonatype.org/content/repositories/releases/"}
  :dependencies [[org.clojure/clojure "1.2.1"]
                 [org.clojure/clojure-contrib "1.2.0"]]
  :dev-dependencies [[org.cloudhoist/pallet-hadoop "0.3.3-beta.3"]
                     [org.jclouds/jclouds-all "1.2.2"]
                     [org.jclouds.driver/jclouds-jsch "1.2.2"]
                     [org.jclouds.driver/jclouds-slf4j "1.2.2"]
                     [ch.qos.logback/logback-classic "1.0.1"]
                     [ch.qos.logback/logback-core "1.0.1"]])
