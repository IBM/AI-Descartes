// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package emf;

import static emf.Operator.*;
import static utils.UList.*;
import static utils.UList.sum;
import static utils.VRAUtils.*;

import utils.SingleAssign;
import utils.UList;

//Do NOT use this class!  It is just a base class, objects should not be compared for equality.
public abstract class Node0DoNotUse {
	protected final Operator op;

	public final boolean isLeaf;
	public final boolean isML;
	public final boolean isLit;
	public final boolean isPsum;
	public final boolean isPsumorP;

	public final int depth, complexity;
	public final int numML;

	public final static SingleAssign<Integer> M_complexity = new SingleAssign<>();

	protected abstract <T extends Node0DoNotUse> UList<T> getNodes();

	protected <T extends Node0DoNotUse> Node0DoNotUse(Operator op, UList<T> nodes) {
		this.op = op;
		//		this.allowConst = true;
		this.isLeaf = op.arity == 0;
		this.isML = (op == IS_LEAF);
		this.isLit = op.isLit;
		//		this.isMultiLeaf = (op.addOrMul && nodes.all(n -> n.isLeaf));
		//		this.leafNodes = nodes.filter(x -> x.isLeaf);

		this.numML = isML ? 1 : sum(nodes.map(n -> n.numML));
		this.isPsum = op == ADD && nodes.all(n -> n.isML);
		this.isPsumorP = isPsum || isML;
		//		this.fracnm = isPsum ? "F" + nodes.get(0).thisAsMLI.nodeNo : null;

		//		this.exp_dont_use = null;
		//		if (op.arity == 0 && nodes.isEmpty())
		//		printf("NODE %s %s%n", op, nodes);
		this.depth = op.arity == 0 ? 0 : 1 + max(nodes.map(n -> n.depth));

		myassert((op.arity == 0) == (isML || isLit), this);
		Globals gx = Globals.gx;
		this.complexity = isML ? M_complexity.get()
				: isLit ? 1 : op.complexity.get() + sum(nodes.map(n -> n.complexity));
	}

	protected abstract String monoToString();

	public String toString() {
		if (isML)
			//			return isNormNode ? "P" : thisAsMLI.toString();
			return monoToString();

		if (op.arity == 0) {
			return op.toString();
		}

		if (op == Operator.DIV || op.addOrMul)
			return "(" + getNodes().join(op.toString()) + ")";

		return op + "(" + getNodes().join(",") + ")";
	}

	public String toYamlString() {
		if (isML)
			return monoToString();

		if (op.arity == 0) {
			return op.toString();
		}

		if (op == Operator.DIV || op.addOrMul)
			return "['" + (op == MUL ? "'*'" : op) + "'," + getNodes().map(n -> n.toYamlString()).join(",") + "]";

		if (op == POW || op == INTERNAL_EXP)
			return op + "(" + getNodes().join(",") + ")";

		die();
		return op + "(" + getNodes().join(",") + ")";
	}

}
