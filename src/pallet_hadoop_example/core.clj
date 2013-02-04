(ns pallet-hadoop-example.core
  (:use pallet-hadoop.node
        [pallet.crate.hadoop :only (hadoop-user)]
        [pallet.extensions :only (def-phase-fn)]
        [pallet.phase :only (phase-fn)]
        [pallet.stevedore :only (script)]
        [pallet.script.lib :only (download-file mkdir)])
  (:require [pallet.core :as core]
            [pallet.resource.directory :as d]
            [pallet.resource.remote-file :as rf]))

(defn bootstrap []
  (use 'pallet.compute)
  (use '[pallet-hadoop.node :only (jobtracker-ip)]))

(def remote-env
  {:algorithms {:lift-fn pallet.core/parallel-lift
                :converge-fn pallet.core/parallel-adjust-node-counts}})

(def-phase-fn authorize-mnt
  "Authorizes the `/mnt` volume for use by the default hadoop user;
  Necessary to take advantage of instance storage on EC2."
  []
  (d/directory "/mnt"
               :owner hadoop-user
               :group hadoop-user
               :mode "0755"))

(def-phase-fn download-data
  []
  (d/directory "/tmp/books"
               :owner hadoop-user
               :group hadoop-user
               :mode "0755")
  (rf/remote-file "/tmp/download-books.sh"
                  :content
                  (script
                   (~mkdir "/tmp/books")
                   (cd "/tmp/books")
                   (doseq [f ["pg20417.txt" "pg5000.txt" "pg4300.txt"
                              "pg132.txt" "pg1661.txt" "pg972.txt" "pg19699.txt"]]
                     (println @f)
                     (~download-file
                      (str "https://hadoopbooks.s3.amazonaws.com/" @f)
                      (str "/tmp/books/" @f) )))
                  :owner hadoop-user
                  :group hadoop-user
                  :mode "0755"
                  :literal true))

(defn create-cluster
  [cluster compute-service]
  (do (boot-cluster cluster
                    :compute compute-service
                    :environment remote-env)
      (lift-cluster cluster
                    :phase (phase-fn
                                     authorize-mnt
                                     download-data)
                    :compute compute-service
                    :environment remote-env)
      (start-cluster cluster
                     :compute compute-service
                     :environment remote-env)))

(defn destroy-cluster
  [cluster compute-service]
  (kill-cluster cluster
                :compute compute-service
                :environment remote-env))

(defn make-example-cluster
  [slave-count ram-size-in-mb]
  (cluster-spec :private
                {:jobtracker (node-group [:jobtracker :namenode])
                 :slaves     (slave-group slave-count)}
                :base-machine-spec {:os-family :ubuntu
                                    :os-version-matches "12.04"
                                    :os-64-bit true
                                    :min-ram ram-size-in-mb}
                :base-props {:hdfs-site {:dfs.data.dir "/mnt/dfs/data"
                                         :dfs.name.dir "/mnt/dfs/name"}
                             :mapred-site {:mapred.local.dir "/mnt/hadoop/mapred/local"
                                           :mapred.task.timeout 300000
                                           :mapred.reduce.tasks 3
                                           :mapred.tasktracker.map.tasks.maximum 3
                                           :mapred.tasktracker.reduce.tasks.maximum 3
                                           :mapred.child.java.opts "-Xms1024m"}}))

(def example-cluster (make-example-cluster 2 (* 4 1024)))

(comment
  (use 'pallet-hadoop-example.core)
  (bootstrap)

  ;; We can define our compute service here...
  (def ec2-service
    (compute-service "aws-ec2"
                     :identity "ec2-access-key-id"
                     :credential "ec2-secret-access-key"))

  ;; Or, we can get this from a config file, in
  ;; `~/.pallet/config.clj`.
  (def ec2-service
    (service :aws))

  ;; Booting the cluster is as simple as the following:
  (create-cluster example-cluster ec2-service)

  ;; And we can kill it like so:
  (destroy-cluster example-cluster ec2-service))


