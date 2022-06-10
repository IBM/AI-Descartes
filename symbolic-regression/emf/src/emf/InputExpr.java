// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package emf;

import static utils.VRAUtils.*;

import java.io.IOException;

import net.sourceforge.yamlbeans.YamlException;
import utils.UList;
import utils.YamlOpts.NestedList;
import utils.YamlOpts.NestedListLeaf;
import utils.YamlOpts.NestedListList;

public class InputExpr {

	public static void main(String[] args) throws Exception {
		new InputExpr(args).main1();
	}

	public final Input inpx;

	public InputExpr(String[] args) throws YamlException, IOException {
		//		super(args);

		this.inpx = new Input(args, false);

		printf("OUTPUT DIR %s%n", inpx.outputDir);
		myassert(!inpx.printBaronFilesUpfront);

	}

	private NormNode normNodeFromSpec(NestedList tree) {
		if (tree instanceof NestedListLeaf) {
			String leaf = ((NestedListLeaf) tree).leaf;
			myassertEqual(leaf, "P");
			return inpx.mlprod;
		}
		UList<NestedList> nodes = ((NestedListList) tree).nodes;
		myassert(nodes.get(0) instanceof NestedListLeaf);
		String opstr = ((NestedListLeaf) nodes.get(0)).leaf;
		Operator op = Operator.lookupOperator(opstr);
		return NormNode.getNormNode(op, nodes.subList(1).map(n -> normNodeFromSpec(n)));
	}

	private INode instNodeFromSpec(NestedList tree) {
		if (tree instanceof NestedListLeaf) {
			String leaf = ((NestedListLeaf) tree).leaf;
			return INode.getMLInstanceFromString(inpx, leaf);
		}
		UList<NestedList> nodes = ((NestedListList) tree).nodes;
		myassert(nodes.get(0) instanceof NestedListLeaf);
		String opstr = ((NestedListLeaf) nodes.get(0)).leaf;
		Operator op = Operator.lookupOperator(opstr);
		//		return INode.getINode(op, nodes.subList(1).map(n -> instNodeFromSpec(n)));
		die();
		return null;
	}

	//	private INode normNodeFromSpec(NestedList tree) {
	//		if (tree instanceof NestedListLeaf)
	//			return INode.getMLInstanceFromString(inpx, ((NestedListLeaf)tree).leaf);
	//		UTreeOp
	//		return 
	//	}

	private void main1() throws Exception {
		final long starttm = System.currentTimeMillis();

		//		Runtime.getRuntime().addShutdownHook(new Thread() {
		//			@Override
		//			public void run() {
		//				System.out.println("Shutdown hook ran!");
		//			}
		//		});

		BaronJob.showOutputOnScreen = true;

		printf("expr: %s%n", inpx.normExprSpec);

		Input2 inpx2 = new Input2(inpx, inpx.unsampled_input);
		NormNode n = normNodeFromSpec(inpx.normExprSpec);
		INode instn = inpx.instExprSpec == null ? n.mkinstance(inpx) : instNodeFromSpec(inpx.instExprSpec);
		n.nodeNum.set(inpx.normExprNum);
		printf("iexpr: %s%n", instn.toYamlString());

		BaronJob job = new BaronJob(inpx2, n, instn, n.depth, UList.empty(), 0);

		job.start(inpx.infinity);
		//		printf("done %s%n", job.isRunning());
	}
}
