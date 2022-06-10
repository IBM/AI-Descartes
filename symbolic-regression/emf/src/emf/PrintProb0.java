// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package emf;

import java.io.PrintStream;

import utils.UList;

public class PrintProb0 {
	public final Input inpx;
	public final UList<DataLine> input;
	//	public final int numInVars;

	public final NormNode node0;
	public final INode nodeInstance;
	public final UList<Split> splits;
	public final PrintStream out;
	public final int redo; // really just for obj_variations

	public PrintProb0(Input2 inpx, NormNode node, INode nodeInstance, UList<Split> splits, int redo, PrintStream out) {
		super();
		//		this.inpx = inpx2.inpx;
		this.inpx = inpx.inpx;
		this.input = inpx.input;
		//		this.numInVars = inpx.numInVars;

		this.node0 = node;
		this.nodeInstance = this.inpx.using_expn_op ? nodeInstance : nodeInstance.tightedPsumBndst();
		this.splits = splits;
		this.redo = redo;
		this.out = out;
	}
}
