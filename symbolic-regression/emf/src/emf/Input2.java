// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package emf;

import utils.UList;

// This is a kludge for dealing with sampled input
public class Input2 {
	public final Input inpx;
	public final UList<DataLine> input;

	public Input2(Input inpx, UList<DataLine> input) {
		this.inpx = inpx;
		this.input = input;
	}

	public Input2 input2(int nsamples, int seed) {
		// preserve original order
		return new Input2(inpx, input.intersection(input.randElems(nsamples, seed)));
	}
}
