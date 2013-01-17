(defproject pallet-hadoop-example "0.0.3-beta.5"
  :description "Example project for running Hadoop on Pallet."
  :repositories {"sonatype"
                 "http://oss.sonatype.org/content/repositories/releases/"}
  :dependencies [[org.clojure/clojure "1.4.0"]
	         [org.cloudhoist/pallet "0.7.2"]
                 [org.cloudhoist/pallet-hadoop "0.3.3-beta.4"]
                 [org.jclouds/jclouds-all "1.4.1"]
                 [org.jclouds.driver/jclouds-sshj "1.4.1"]
                 [org.jclouds.driver/jclouds-slf4j "1.4.1"]
                 [org.cloudhoist/pallet-jclouds "1.4.0-beta.1"]
                 [ch.qos.logback/logback-classic "1.0.1"]
                 [ch.qos.logback/logback-core "1.0.1"]])
