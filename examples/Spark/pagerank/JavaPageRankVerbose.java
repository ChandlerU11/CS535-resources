/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* modified by amit */

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import scala.Tuple2;

import com.google.common.collect.Iterables;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.sql.SparkSession;

/**
 * Computes the PageRank of URLs from an input file. Input file should be in
 * format of: URL neighbor URL URL neighbor URL URL neighbor URL ... where URL
 * and their neighbors are separated by space(s).
 *
 * This is an example implementation for learning how to use Spark. For more
 * conventional use, please refer to org.apache.spark.graphx.lib.PageRank
 *
 */
public final class JavaPageRankVerbose
{
    private static final Pattern SPACES = Pattern.compile("\\s+");


    static void showWarning() {
	String warning = "WARN: This is a naive implementation of PageRank " + "and is given as an example! \n"
	        + "Please use the PageRank implementation found in "
	        + "org.apache.spark.graphx.lib.PageRank for more conventional use.";
	System.err.println(warning);
    }

    @SuppressWarnings("serial")
    private static class Sum implements Function2<Double, Double, Double>
    {
	@Override
	public Double call(Double a, Double b) {
	    return a + b;
	}
    }


    public static void main(String[] args) throws Exception {
	if (args.length < 2) {
	    System.err.println("Usage: JavaPageRank <file> <number_of_iterations>");
	    System.exit(1);
	}

	showWarning();

	SparkSession spark = SparkSession.builder().appName("JavaPageRank").getOrCreate();

	// Loads in input file. It should be in format of:
	// URL neighbor URL
	// URL neighbor URL
	// URL neighbor URL
	// ...
	JavaRDD<String> lines = spark.read().textFile(args[0]).javaRDD();

	// Loads all URLs from input file and initialize their neighbors.
	JavaPairRDD<String, Iterable<String>> links = lines.mapToPair(s -> {
	    String[] parts = SPACES.split(s);
	    return new Tuple2<>(parts[0], parts[1]);
	}).distinct().groupByKey().cache();

	// Show the output from previous step
	List<Tuple2<String, Iterable<String>>> linksOutput = links.collect();
	System.out.println();
	for (Tuple2<?, ?> tuple : linksOutput) {
	    System.out.println(tuple._1() + " has neighbors: " + tuple._2());
	}
	System.out.println();

	// Loads all URLs with other URL(s) link to from input file and initialize ranks
	// of them to one.
	JavaPairRDD<String, Double> ranks = links.mapValues(rs -> 1.0);

	// Display initial ranks
	List<Tuple2<String, Double>> ranksOutput = ranks.collect();
	System.out.println();
	for (Tuple2<?, ?> tuple : ranksOutput) {
	    System.out.println(tuple._1() + " has initial rank: " + tuple._2());
	}
	System.out.println();

	// Calculates and updates URL ranks continuously using PageRank algorithm.
	for (int current = 0; current < Integer.parseInt(args[1]); current++) {
	    JavaPairRDD<String, Tuple2<Iterable<String>, Double>> test1 = links.join(ranks);
	    List<Tuple2<String, Tuple2<Iterable<String>, Double>>> out1 = test1.collect();
	    System.out.println();
	    for (Tuple2<?, ?> tuple : out1) {
		System.out.println(tuple._1() + " has joined value: " + tuple._2());
	    }
	    System.out.println();
	    
	    
	    // Calculates URL contributions to the rank of other URLs.
	    JavaPairRDD<String, Double> contribs = links.join(ranks).values().flatMapToPair(s -> {
		int urlCount = Iterables.size(s._1());
		List<Tuple2<String, Double>> results = new ArrayList<>();
		for (String n : s._1) {
		    results.add(new Tuple2<>(n, s._2() / urlCount));
		}
		return results.iterator();
	    });
	    
	    List<Tuple2<String, Double>> contribsOutput = contribs.collect();
	    System.out.println();
	    for (Tuple2<?, ?> tuple : contribsOutput) {
		System.out.println(tuple._1() + " has new rank in iteration " + current + ": " + tuple._2());
	    }
	    System.out.println();

	    // Re-calculates URL ranks based on neighbor contributions.
	    ranks = contribs.reduceByKey(new Sum()).mapValues(sum -> 0.15 + sum * 0.85);
	}

	// Collects all URL ranks and dump them to console.
	List<Tuple2<String, Double>> output = ranks.collect();
	for (Tuple2<?, ?> tuple : output) {
	    System.out.println(tuple._1() + " has rank: " + tuple._2() + ".");
	}

	spark.stop();
    }
}
