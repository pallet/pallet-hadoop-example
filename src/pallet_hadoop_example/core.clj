(ns pallet-hadoop-example.core
  (:use pallet-hadoop.node
        [pallet.crate.hadoop :only (hadoop-user)]
        [pallet.extensions :only (def-phase-fn)])
  (:require [pallet.core :as core]
            [pallet.resource.directory :as d]))

(defn bootstrap []
  (use 'pallet.compute))

(def remote-env
  {:algorithms {:lift-fn pallet.core/parallel-lift
                :converge-fn pallet.core/parallel-adjust-node-counts}})

(def-phase-fn authorize-mnt
  "Authorizes the `/mnt` volume for use by the default hadoop user;
  Necessary to take advantage of space Changes the permissions on
  /mnt, for ec2 systems."
  []
  (d/directory "/mnt"
               :owner hadoop-user
               :group hadoop-user
               :mode "0755"))

(defn create-cluster
  [cluster compute-service]
  (do (boot-cluster cluster
                    :compute compute-service
                    :environment remote-env)
      (lift-cluster cluster
                    :phase authorize-mnt
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

(def example-cluster
  (cluster-spec :private
                {:jobtracker (node-group [:jobtracker :namenode])
                 :slaves     (slave-group 2)}
                :base-machine-spec {:os-family :ubuntu
                                    :os-version-matches "10.10"
                                    :os-64-bit true
                                    :min-ram (* 4 1024)}
                :base-props {:hdfs-site {:dfs.data.dir "/mnt/dfs/data"
                                         :dfs.name.dir "/mnt/dfs/name"}
                             :mapred-site {:mapred.task.timeout 300000
                                           :mapred.reduce.tasks 3
                                           :mapred.tasktracker.map.tasks.maximum 3
                                           :mapred.tasktracker.reduce.tasks.maximum 3
                                           :mapred.child.java.opts "-Xms1024m"}}))

(comment
  ;; We can define our compute service here...
  (def ec2-service
    (compute-service "aws-ec2"
                     :identity "ec2-access-key-id"
                     :credential "ec2-secret-access-key"))

  ;; Or, we can get this from a config file, in
  ;; `~/.pallet/config.clj`.
  (def ec2-service
    (compute-service-from-config-file :aws))

  ;; Booting the cluster is as simple as the following:
  (create-cluster example-cluster ec2-service)

  ;; And we can kill it like so:
  (destroy-cluster example-cluster ec2-service))


