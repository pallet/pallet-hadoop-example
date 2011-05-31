;; ## Pallet and Hadoop
;;
;; Let's talk about how to run hadoop clusters on pallet.
;;
;; (We assume basic knowledge of how to create a clojure project,
;;import dependencies, etc.)
;;
;; Go into..
;;
;; What pallet is -- let's give a little introduction.
;; 
;; How we're riding on top of jclouds, we get to do whatever we want.
;;
;; How do we set ourselves up to use pallet-hadoop? Talk about how to
;; modify the project.clj file, including sonatype. Tell how we've
;;included jclouds 9c.
;;
;; What's a compute service? We need to authorize with a cloud
;;provider. Today we'll be working on Amazon's EC2.
;;
;; How to get an account on EC2.
;;
;; How to get your secret key and identification.
;;
;; Now, start up a clojure project with the following dependencies:
;; pallet-hadoop, really, is the only one.
;;
;; use all required stuff...
;;
;; Talk about the cluster, and what we're allowing people to do with
;; it. Basically, a cluster is defined through an ip-type, a map of
;; node-tags-> node definitions, and base node-spec and property
;; map. Each node, then, can have its own node-spec and property map,
;; which get merged in. Properties knock out properties below. Machine
;; specs merge, as of now, since the only required machine spec for
;; hadoop involves ports. (I don't have the ability to do custom
;; phases, yet, which makes this a really basic thing -- but we'll do
;; some examples.)
;;
;; Also, talk about master nodes vs slave nodes.
;;
;; Start the cluster!
;;
;; Go to EC2 console, get the public DNS address of the jobtracker. Go
;; to address:50030, you'll see hadoop running.
;;
;; Show how to run a test job in MapReduce.
;;
;; Shut the cluster DOWN.
;;
;; Next installment -- let's talk about how to test these sorts of
;; clusters in a local virtual machine environment.
;;
;; Then, we'll talk about how to get a cascalog query working on
;; Hadoop.

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

(def ec2-service (compute/compute-service "aws-ec2"
                                          :identity "ec2-access-key-id"
                                          :credential "ec2-secret-access-key"))

;; Or, we can get this from a config file, in ~/.pallet/config.clj.
(def ec2-service
  (compute/compute-service-from-config-file :aws))

;; Let's define a cluster for EC2, with four nodes -- one's a
;; jobtracker and namenode, the other three will be slavenodes. We
;; could just as easily have done either of the following:
;;
;;   (hadoop-node [:jobtracker :slavenode])
;;   (hadoop-node [:namenode :slavenode])
;;   (slave-node 3)

;;   (hadoop-node [:jobtracker :namenode :slavenode])
;;   (slave-node 3)

(def test-cluster
  (cluster-spec :private
                {:jobtracker (hadoop-node [:jobtracker :namenode])
                 :slaves (slave-node 3)}
                :base-machine-spec {:os-family :ubuntu
                                    :os-version-matches "10.10"
                                    :os-64-bit true
                                    :fastest true}
                :base-props {:hdfs-site {:dfs.data.dir "/mnt/dfs/data"
                                         :dfs.name.dir "/mnt/dfs/name"}
                             :mapred-site {:mapred.task.timeout 300000
                                           :mapred.reduce.tasks 60
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

(defn create-cluster []
  (do (boot-cluster test-cluster
                    :compute ec2-service
                    :environment remote-env)
      (lift-cluster test-cluster
                    authorize-mnt
                    :compute ec2-service
                    :environment remote-env)
      (start-cluster test-cluster
                     :compute ec2-service
                     :environment remote-env)))

(defn destroy-cluster []
  (kill-cluster test-cluster
                :compute ec2-service
                :environment remote-env))
