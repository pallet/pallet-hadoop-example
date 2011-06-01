# Pallet-Hadoop #

This project serves as an example to get you started using [Pallet-Hadoop](https://github.com/pallet/pallet-hadoop), a layer over [Pallet](https://github.com/pallet/pallet) that translates data descriptions of Hadoop clusters into full configured, running machines. For a more detailed introduction, see [this blog post](http://sritchie.github.com/2011/05/31/hadoop-on-pallet.html).

## Requirements ##

Before you get your first cluster running, you'll need to [create an AWS account](https://aws-portal.amazon.com/gp/aws/developer/registration/index.html). Once you've done this, navigate to [your account page](http://aws.amazon.com/account/) and follow the "Security Credentials" link. Under "Access Credentials", you should see a tab called "Access Keys". Note down your Access Key ID and Secret Access Key for future reference.

To get started  Pallet-Hadoop, clone this project to your local machine:

    $ git clone https://github.com/pallet/pallet-hadoop-example
    $ cd pallet-hadoop-example

Open up `./src/pallet-hadoop-example/core.clj` with your favorite text editor. `example-cluster` contains a data description of a full hadoop cluster with:

* One master node functioning as jobtracker and namenode
* Three slave nodes (`(slave-group 3)`), each acting as datanode and tasktracker.

Start a repl:

      $ lein repl

This will get you to a REPL in `pallet-hadoop-example.core`.

## Compute Service ##

Pallet abstracts away details about specific cloud providers through the idea of a "compute service". The combination of our cluster definition and our compute service will be enough to get our cluster running. We define a compute service at our REPL like so:

    => (def ec2-service
           (compute-service "aws-ec2"
                            :identity "ec2-access-key-id"
                            :credential "ec2-secret-access-key"))
    #'pallet-hadoop-example.core/ec2-service

Alternatively, if you want to keep these out of your code base, save the following to `~/.pallet/config.clj`:

    (defpallet
      :services {:aws {:provider "aws-ec2"
                       :identity "ec2-access-key-id"
                       :credential "ec2-secret-access-key"}})

and define `ec2-service` with:

    => (def ec2-service (compute-service-from-config-file :aws))
    #'pallet-hadoop-example.core/ec2-service

## Boot! ##

    => (create-cluster example-cluster ec2-service)

Once `create-cluster` returns, it's time to log in and run a MapReduce job. Head over to the [EC2 Console](https://console.aws.amazon.com/ec2/), log in, and click "Instances" on the left. You should see four nodes running; click on the node whose security group contains "jobtracker", and scroll the lower pane down to retrieve the public DNS address for the node. It'll look something like

    ec2-50-17-103-174.compute-1.amazonaws.com

We'll refer to this address as `jobtracker.com`. Point your browser to `jobtracker.com:50030`, and you'll see jobtracker console for mapreduce jobs. `jobtracker.com:50070` points to the namenode console, with information about HDFS.

Head into a terminal and run the following commands:

     $ ssh jobtracker.com (insert actual address, enter yes to continue connecting)
     $ sudo su - hadoop
