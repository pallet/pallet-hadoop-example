(ns pallet-hadoop-example.core
  (:use pallet-hadoop.node
        [pallet.crate.hadoop :only (hadoop-user)]
        [pallet.extensions :only (def-phase-fn)])
  (:require [pallet.core :as core]
            [pallet.compute :as compute]
            [pallet.resource.directory :as d]))

(def remote-env
  {:algorithms {:lift-fn pallet.core/parallel-lift
                :converge-fn pallet.core/parallel-adjust-node-counts}})

(def ec2-service
  (compute/compute-service-from-config-file :aws))

(def test-cluster
  (cluster-spec :private
                {:jobtracker (hadoop-node [:jobtracker :namenode])
                 :slaves (slave-node 3)}
                :base-machine-spec {
                                    :os-family :ubuntu
                                    :os-version-matches "10.10"
                                    :os-64-bit true
                                    :fastest true
                                    :spot-price (float 1.60)
                                    }
                :base-props {:hdfs-site {:dfs.data.dir "/mnt/dfs/data"
                                         :dfs.name.dir "/mnt/dfs/name"}
                             :mapred-site {:mapred.task.timeout 300000
                                           :mapred.reduce.tasks (int (* 1.2 15 nodecount))
                                           :mapred.tasktracker.map.tasks.maximum 15
                                           :mapred.tasktracker.reduce.tasks.maximum 15
                                           :mapred.child.java.opts "-Xms1024m -Xmx1024m"}}))

(def-phase-fn authorize-mnt
  "Changes the permissions on /mnt, for ec2 systems. DOC!"
  []
  (d/directory "/mnt"
               :owner hadoop-user
               :group hadoop-user
               :mode "0755"))

(defn forma-boot []
  (do (boot-cluster test-cluster :compute ec2-service :environment remote-env)
      (lift-cluster test-cluster authorize-mnt :compute ec2-service :environment remote-env)
      (start-cluster test-cluster :compute ec2-service :environment remote-env)))

(defn forma-kill []
  (kill-cluster forma-cluster :compute env/ec2-service :environment env/remote-env))
