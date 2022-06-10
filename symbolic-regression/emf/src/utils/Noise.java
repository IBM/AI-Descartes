// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package utils;

import java.util.Random;

public class Noise {
	private final Random rand;
	public final long seed;

	public final double mean;
	public final double stdev;
	//	mean should be 0, stdev should be: epsilon*rms(y), where you let epsilon be 10^{-2} or 10^{-1}.

	//	public Noise() {
	//		this(0);
	//	}

	public Noise(long seed, double mean, double stdev) {
		this.seed = seed;
		this.rand = new Random(seed);
		this.mean = mean;
		this.stdev = stdev;
		//		printf("NOISE %s %s %s%n", seed, mean, stdev);
	}

	public static double root_mean_square(UList<Double> xs) {
		double x = 0;
		int n = xs.size();
		for (int i = 0; i < n; ++i)
			x += xs.get(i) * xs.get(i);
		double rms = Math.sqrt(x / n);
		//		printf("RMS %s %s%n", rms, xs);
		return rms;
	}

	// translated from C code written by Sanjeeb Dash for Bunge.
	//	returns a sample from any normal N( mean , stdev ) distribution. Uses the generator for standard normals above. 
	//	public double get_normal_sample(double mean, double stdev) {
	public double get_normal_sample() {
		double ns = get_std_normal_sample();
		//		printf("GNS %s (%s %s %s)%n", ns * stdev + mean, ns, stdev, mean);
		return ns * stdev + mean;
	}

	private int is_cache_filled = 0;

	private double cached_sample;

	/* function to return a point in a U(0,1), i.e., from the uniform random normal distribution */
	private double get_random() {
		double p = (double) (rand.nextInt() % 479001599) / (double) 479001599;
		if (p <= 0.0)
			p = 1e-09;
		if (p >= 1.0)
			p = 1.0 - 1e-09;
		return p;
	}

	/*
	 Uses polar Box-Muller transformation.
	 returns a sample from the standard normal N( mean = 0, stdev = 1.0) distribution 
	*/
	private double get_std_normal_sample() {
		double x, y, r, z;

		if (is_cache_filled != 0) {
			is_cache_filled = 0;
			return cached_sample;
		}

		do {
			/* @FIXME rng_raw() is a call to a random generator to get a value in interval (0,1) */
			x = 2.0 * get_random() - 1.0;
			y = 2.0 * get_random() - 1.0;
			r = x * x + y * y;
		} while (r >= 1.0);

		z = Math.sqrt(-2.0 * Math.log(r) / r);

		cached_sample = x * z;
		is_cache_filled = 1;
		return y * z;
	}
}
