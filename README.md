# Pallet-Hadoop #

This project serves as an example to get you started using [Pallet-Hadoop](https://github.com/pallet/pallet-hadoop), a layer over [Pallet](https://github.com/pallet/pallet) that translates data descriptions of Hadoop clusters into full configured, running machines. For a more detailed discussion of Pallet-Hadoop's design, see the [project wiki](https://github.com/pallet/pallet-hadoop/wiki).

## Background ##

Hadoop is an Apache java framework that allows for distributed processing of enormous datasets across large clusters. It combines a computation engine based on [MapReduce](http://en.wikipedia.org/wiki/MapReduce) with [HDFS](http://hadoop.apache.org/hdfs/docs/current/hdfs_design.html), a distributed filesystem based on the [Google File System](http://en.wikipedia.org/wiki/Google_File_System).

Abstraction layers such as [Cascading](https://github.com/cwensel/cascading) (for Java) and [Cascalog](https://github.com/nathanmarz/cascalog) (for [Clojure](http://clojure.org/)) make writing MapReduce queries quite nice. Indeed, running hadoop locally with cascalog [couldn't be easier](http://nathanmarz.com/blog/introducing-cascalog-a-clojure-based-query-language-for-hado.html).

Unfortunately, graduating one's MapReduce jobs to the cluster level isn't so easy. Amazon's [Elastic MapReduce](http://aws.amazon.com/elasticmapreduce/) is a great option for getting up and running fast; but what to do if you want to configure your own cluster?

After surveying existing tools, I decided to write my own layer over [Pallet](https://github.com/pallet/pallet), a wonderful cloud provisioning library written in Clojure. Pallet runs on top of [jclouds](https://github.com/jclouds/jclouds), which allows pallet to define its operations independent of any one cloud provider. Switching between clouds involves a change of login credentials, nothing more.

## Setting Up ##

Before you get your first cluster running, you'll need to [create an AWS account](https://aws-portal.amazon.com/gp/aws/developer/registration/index.html). Once you've done this, navigate to [your account page](http://aws.amazon.com/account/) and follow the "Security Credentials" link. Under "Access Credentials", you should see a tab called "Access Keys". Note down your Access Key ID and Secret Access Key for future reference.

I'm going to assume that you have some basic knowledge of clojure, and know how to get a project running using [leiningen](https://github.com/technomancy/leiningen) or [cake](https://github.com/ninjudd/cake). Go ahead and download [the example project](https://github.com/pallet/pallet-hadoop-example) to follow along:

    $ git clone git://github.com/pallet/pallet-hadoop-example.git
    $ cd pallet-hadoop-example

Open up `./src/pallet-hadoop-example/core.clj` with your favorite text editor. `example-cluster` contains a data description of a full hadoop cluster with:

* One master node functioning as jobtracker and namenode
* Two slave nodes (`(slave-group 2)`), each acting as datanode and tasktracker.

Start a repl:

      $ lein repl
      user=> (use 'pallet-hadoop-example.core) (bootstrap)
<br/>
### Compute Service ###

Pallet abstracts away details about specific cloud providers through the idea of a "compute service". The combination of our cluster definition and our compute service will be enough to get our cluster running. We define a compute service at our REPL like so:

{% highlight clojure %}
user=> (def ec2-service
           (compute-service "aws-ec2"
                            :identity "ec2-access-key-id"         ;; Swap in your access key ID
                            :credential "ec2-secret-access-key")) ;; Swap in your secret key
#'pallet-hadoop-example.core/ec2-service
{% endhighlight %}

Alternatively, if you want to keep these out of your code base, save the following to `~/.pallet/config.clj`:

{% highlight clojure %}
(defpallet
  :services {:aws {:provider "aws-ec2"
                   :identity "your-ec2-access-key-id"
                   :credential "your-ec2-secret-access-key"}})
{% endhighlight %}

and define `ec2-service` with:

{% highlight clojure %}
user=> (def ec2-service (compute-service-from-config-file :aws))
#'pallet-hadoop-example.core/ec2-service
{% endhighlight %}

### Booting the Cluster ###

Now that we have our compute service and our cluster defined, booting the cluster is as simple as the following:

{% highlight clojure %}
=> (create-cluster example-cluster ec2-service)
{% endhighlight %}

The logs you see flying by are Pallet's SSH communications with the nodes in the cluster. On node startup, Pallet uses your local SSH key to gain passwordless access to each node, and coordinates all configuration using streams of SSH commands.

Once `create-cluster` returns, we're done! We now have a fully configured, multi-node Hadoop cluster at our disposal.

### Running Word Count ###

To test our new cluster, we're going log in and run a word counting MapReduce job on a number of books from [Project Gutenberg](http://www.gutenberg.org/wiki/Main_Page).

Point your browser to the [EC2 Console](https://console.aws.amazon.com/ec2/), log in, and click "Instances" on the left.

You should see three nodes running; click on the node whose security group contains "jobtracker", and scroll the lower pane down to retrieve the public DNS address for the node. It'll look something like

    ec2-50-17-103-174.compute-1.amazonaws.com

I'll refer to this address as `jobtracker.com`.

Point your browser to `jobtracker.com:50030`, and you'll see the JobTracker web console. (Keep this open, as it will allow us to watch our MapReduce job in action.).`jobtracker.com:50070` points to the NameNode console, with information about HDFS.

Next, we'll SSH into the jobtracker, and operate as the hadoop user. Head to your terminal and run the following commands:

     $ ssh jobtracker.com (insert actual address, enter yes to continue connecting)
     $ sudo su - hadoop

### Copy Data to HDFS ###

At this point, we're ready to begin following along with Michael Noll's excellent [Hadoop configuration tutorial](http://goo.gl/aALr9). (I'll cover some of the same ground for clarity.)

Our first step will be to collect a bunch of text to process. We start by downloading the following seven books to a temp directory:

* [The Outline of Science, Vol. 1 (of 4) by J. Arthur Thomson](http://www.gutenberg.org/cache/epub/20417/pg20417.txt)
* [The Notebooks of Leonardo Da Vinci](http://www.gutenberg.org/cache/epub/5000/pg5000.txt)
* [Ulysses by James Joyce](http://www.gutenberg.org/cache/epub/4300/pg4300.txt)
* [The Art of War by 6th cent. B.C. Sunzi](http://www.gutenberg.org/cache/epub/132/pg132.txt)
* [The Adventures of Sherlock Holmes by Sir Arthur Conan Doyle](http://www.gutenberg.org/cache/epub/1661/pg1661.txt)
* [The Devil’s Dictionary by Ambrose Bierce](http://www.gutenberg.org/cache/epub/972/pg972.txt)
* [Encyclopaedia Britannica, 11th Edition, Volume 4, Part 3](http://www.gutenberg.org/cache/epub/19699/pg19699.txt)

Running the following commands at the remote shell should do the trick.

    $ mkdir /tmp/books
    $ cd /tmp/books
    $ curl -O http://www.gutenberg.org/cache/epub/20417/pg20417.txt
    $ curl -O http://www.gutenberg.org/cache/epub/5000/pg5000.txt
    $ curl -O http://www.gutenberg.org/cache/epub/4300/pg4300.txt
    $ curl -O http://www.gutenberg.org/cache/epub/132/pg132.txt
    $ curl -O http://www.gutenberg.org/cache/epub/1661/pg1661.txt
    $ curl -O http://www.gutenberg.org/cache/epub/972/pg972.txt
    $ curl -O http://www.gutenberg.org/cache/epub/19699/pg19699.txt

Next, navigate to the Hadoop directory:

    $ cd /usr/local/hadoop-0.20.2/

And copy the books over to the distributed filesystem:

    /usr/local/hadoop-0.20.2$ hadoop dfs -copyFromLocal /tmp/books books
    /usr/local/hadoop-0.20.2$ hadoop dfs -ls
    Found 1 items
    drwxr-xr-x   - hadoop supergroup          0 2011-06-01 06:12:21 /user/hadoop/books
    /usr/local/hadoop-0.20.2$ 

### Running MapReduce ###

We're ready to run the MapReduce job. `wordcount` takes an input path within HDFS, processes all items within, and saves the output to HDFS -- to `books-output`, in this case. Run this command:

    /usr/local/hadoop-0.20.2$ hadoop jar hadoop-examples-0.20.2-cdh3u0.jar wordcount books/ books-output/

And you should see something very similar to this:

    11/06/01 06:14:30 INFO input.FileInputFormat: Total input paths to process : 7
    11/06/01 06:14:30 INFO mapred.JobClient: Running job: job_201106010554_0002
    11/06/01 06:14:31 INFO mapred.JobClient:  map 0% reduce 0%
    11/06/01 06:14:44 INFO mapred.JobClient:  map 57% reduce 0%
    11/06/01 06:14:45 INFO mapred.JobClient:  map 71% reduce 0%
    11/06/01 06:14:46 INFO mapred.JobClient:  map 85% reduce 0%
    11/06/01 06:14:48 INFO mapred.JobClient:  map 100% reduce 0%
    11/06/01 06:14:57 INFO mapred.JobClient:  map 100% reduce 33%
    11/06/01 06:15:00 INFO mapred.JobClient:  map 100% reduce 66%
    11/06/01 06:15:01 INFO mapred.JobClient:  map 100% reduce 100%
    11/06/01 06:15:02 INFO mapred.JobClient: Job complete: job_201106010554_0002
    11/06/01 06:15:02 INFO mapred.JobClient: Counters: 22
    11/06/01 06:15:02 INFO mapred.JobClient:   Job Counters 
    11/06/01 06:15:02 INFO mapred.JobClient:     Launched reduce tasks=3
    11/06/01 06:15:02 INFO mapred.JobClient:     SLOTS_MILLIS_MAPS=74992
    11/06/01 06:15:02 INFO mapred.JobClient:     Total time spent by all reduces waiting after reserving slots (ms)=0
    11/06/01 06:15:02 INFO mapred.JobClient:     Total time spent by all maps waiting after reserving slots (ms)=0
    11/06/01 06:15:02 INFO mapred.JobClient:     Launched map tasks=7
    11/06/01 06:15:02 INFO mapred.JobClient:     Data-local map tasks=7
    11/06/01 06:15:02 INFO mapred.JobClient:     SLOTS_MILLIS_REDUCES=46600
    11/06/01 06:15:02 INFO mapred.JobClient:   FileSystemCounters
    11/06/01 06:15:02 INFO mapred.JobClient:     FILE_BYTES_READ=1610042
    11/06/01 06:15:02 INFO mapred.JobClient:     HDFS_BYTES_READ=6557336
    11/06/01 06:15:02 INFO mapred.JobClient:     FILE_BYTES_WRITTEN=2753014
    11/06/01 06:15:02 INFO mapred.JobClient:     HDFS_BYTES_WRITTEN=1334919
    11/06/01 06:15:02 INFO mapred.JobClient:   Map-Reduce Framework
    11/06/01 06:15:02 INFO mapred.JobClient:     Reduce input groups=121791
    11/06/01 06:15:02 INFO mapred.JobClient:     Combine output records=183601
    11/06/01 06:15:02 INFO mapred.JobClient:     Map input records=127602
    11/06/01 06:15:02 INFO mapred.JobClient:     Reduce shuffle bytes=958780
    11/06/01 06:15:02 INFO mapred.JobClient:     Reduce output records=121791
    11/06/01 06:15:02 INFO mapred.JobClient:     Spilled Records=473035
    11/06/01 06:15:02 INFO mapred.JobClient:     Map output bytes=10812590
    11/06/01 06:15:02 INFO mapred.JobClient:     Combine input records=1111905
    11/06/01 06:15:02 INFO mapred.JobClient:     Map output records=1111905
    11/06/01 06:15:02 INFO mapred.JobClient:     SPLIT_RAW_BYTES=931
    11/06/01 06:15:02 INFO mapred.JobClient:     Reduce input records=183601
    /usr/local/hadoop-0.20.2$ 
<br/>

### Retrieving Output ###

Now that the MapReduce job has completed successfully, all that remains is to extract the results from HDFS and take a look.

    $ mkdir /tmp/books-output
    $ hadoop dfs -getmerge books-output /tmp/books-output
    $ head /tmp/books-output/books-output

You should see something very close to:

    "'Ah!'	2
    "'Ample.'	1
    "'At	1
    "'But,	1
    "'But,'	1
    "'Come!	1
    "'December	1
    "'For	1
    "'Hampshire.	1
    "'Have	1

Success!

### Killing the Cluster ###

When we're finished, we can kill our cluster with this command, back at the REPL:

{% highlight clojure %}
=> (destroy-cluster example-cluster ec2-service)
{% endhighlight %}
