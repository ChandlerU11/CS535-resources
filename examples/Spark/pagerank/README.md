
Build
=====

Build the jar file directly in Eclipse (just include all Spark jar files from
~/spark-install/spark/jars as External jars for the project.

Run
===

spark-submit --class "JavaPageRank" --master local[4] pagerank.jar data_file.txt iterations

where local[4] says to use 4 threads on local machine. You can change that to higher or lower
or replace by * to use as many threads as the number of logical threads on your local system.

For input files on HDFS, make sure Hadoop is up and running. Then use the following 

spark-submit --class "JavaPageRank" --master local[4] pagerank.jar hdfs://localhost:9000/user/amit/data iterations


Logging
=======

By default, spark generates a lot of info messages. You can redirect them to a file as follows:

spark-submit --class "JavaPageRank" --master local[4]  pagerank.jar data_file.txt iterations  2> log

Or you can control the logging level from your program. Here is the relevant code:

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

. . .

Logger log = LogManager.getRootLogger();
log.setLevel(Level.WARN);

