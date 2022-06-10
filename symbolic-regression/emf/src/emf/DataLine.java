// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package emf;

import utils.Noise;
import utils.UList;

public class DataLine {
	//* which line in the file is it?
	public final int index;
	public final UList<Double> invals;
	public final double outval;

	public String toString() {
		return "DL(" + invals + " => " + outval + ")";
	}

	public double getx(int i) {
		return invals.get(i);
	}

	public int sizex() {
		return invals.size();
	}

	public UList<Double> subListx(int fromIndex, int toIndex) {
		return invals.subList(fromIndex, toIndex);
	}

	public DataLine(int index, UList<Double> invals, double outval) {
		this.index = index;
		this.invals = invals;
		this.outval = outval;
	}

	//	public DataLine addNoise(Random rand, double add_noise_to_input) {
	//		return new DataLine(invals, outval * 0.01 * (100 + add_noise_to_input * (2 * rand.nextDouble() - 1.0)));
	//	}

	public DataLine addNoise(Noise noise) {
		double ns = noise.get_normal_sample();
		//		printf("NOISE %s%n", ns);
		return new DataLine(index, invals, outval + ns);
	}

}
