(defproject pallet-hadoop-example "0.0.1-SNAPSHOT"
  :description "Example project for running Hadoop on Pallet."
  :repositories {"conjars" "http://conjars.org/repo/"
                 "sonatype-release"
                 "http://oss.sonatype.org/content/repositories/releases/"
                 "sonatype-snap"
                 "http://oss.sonatype.org/content/repositories/snapshots/"}
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]]
  :dev-dependencies [[pallet-hadoop "0.0.2-SNAPSHOT"]
                     [swank-clojure "1.4.0-SNAPSHOT"]
                     [clojure-source "1.2.0"]])
