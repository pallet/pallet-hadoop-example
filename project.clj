(defproject pallet-hadoop-example "0.0.1-SNAPSHOT"
  :description "Example project for running Hadoop on Pallet."
  :repositories {"conjars" "http://conjars.org/repo/"
                 "sonatype"
                 "http://oss.sonatype.org/content/repositories/releases/"}
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]]
  :dev-dependencies [[pallet-hadoop "0.1.0"]
                     [org.jclouds/jclouds-all "1.0-beta-9c"]
                     [org.jclouds.driver/jclouds-jsch "1.0-beta-9c"]
                     [org.jclouds.driver/jclouds-log4j "1.0-beta-9c"]
                     [log4j/log4j "1.2.14"]
                     [swank-clojure "1.4.0-SNAPSHOT"]
                     [clojure-source "1.2.0"]])
