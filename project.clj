(defproject pallet-hadoop-example "0.0.3-beta.5"
  :description "Example project for running Hadoop on Pallet."
  :repositories {"sonatype"
                 "http://oss.sonatype.org/content/repositories/releases/"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.cloudhoist/pallet-hadoop "0.3.3"]
                 [org.apache.jclouds/jclouds-all "1.7.1"]
                 [org.apache.jclouds.driver/jclouds-sshj "1.7.1"]
                 [org.apache.jclouds.driver/jclouds-slf4j "1.7.1"]
                 [com.palletops/pallet-jclouds "1.7.0"]
                 [ch.qos.logback/logback-classic "1.0.1"]
                 [ch.qos.logback/logback-core "1.0.1"]])
