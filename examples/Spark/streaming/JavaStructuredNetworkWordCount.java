
import java.util.Arrays;
import java.util.regex.Pattern;

import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.Trigger;

/**
 * Counts words in UTF8 encoded, '\n' delimited text received from the network
 * every second.
 *
 * Usage: JavaStructuredNetworkWordCount <hostname> <port> <hostname> and <port>
 * describe the TCP server that Spark Streaming would connect to receive data.
 *
 * To run this on your local machine, you need to first run a Netcat server `$
 * nc -lk 9999` and then run the example `$ bin/run-example
 * org.apache.spark.examples.streaming.JavaNetworkWordCount localhost 9999`
 */
public final class JavaStructuredNetworkWordCount
{
    private static final Pattern SPACE = Pattern.compile(" ");


    public static void main(String[] args) throws Exception {
	if (args.length < 2) {
	    System.err.println("Usage: JavaStructuredNetworkWordCount <hostname> <port>");
	    System.exit(1);
	}

	SparkSession spark = SparkSession.builder().appName("JavaStructuredNetworkWordCount").getOrCreate();

	// Create DataFrame representing the stream of input lines from connection to
	// localhost:9999
	Dataset<Row> lines = spark.readStream()
		.format("socket")
		.option("host", "localhost")
		.option("port", 9999)
	        .load();
	
	// Split the lines into words
	Dataset<String> words = lines.as(Encoders.STRING()).flatMap(
	        (FlatMapFunction<String, String>) x -> Arrays.asList(x.split(" ")).iterator(), Encoders.STRING());

	// Generate running word count
	Dataset<Row> wordCounts = words.groupBy("value").count();

	// Start running the query that prints the running counts to the console
//	StreamingQuery query = wordCounts.writeStream().outputMode("complete")
//	                       .format("console").start();
	StreamingQuery query = wordCounts.writeStream().outputMode("complete")
		               .format("console")
		               .trigger(Trigger.ProcessingTime(5000))
		               .start();

// Requires watermark 
//	StreamingQuery query = wordCounts.writeStream().outputMode("append")
//	               .format("console")
//	               .trigger(Trigger.Continuous(100))
//	               .start();

	query.awaitTermination();

    }
}
