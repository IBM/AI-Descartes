// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package emf;

import static emf.Operator.*;
import static utils.UList.*;
import static utils.VRAUtils.*;

import java.util.LinkedHashMap;

import utils.SingleAssign;
import utils.UList;

public class NormNode extends Node0DoNotUse implements Comparable<NormNode> {

	//	public final INode inode;

	public final UList<NormNode> nodes;//, mlNodes;

	@SuppressWarnings("unchecked")
	protected <T extends Node0DoNotUse> UList<T> getNodes() {
		return (UList<T>) nodes;
	}

	public final UList<NormNode> oneList;

	public final SingleAssign<Integer> nodeNum = new SingleAssign<>();

	public final NormNode base() {
		myassert(op == POW);
		return nodes.get(0);
	}

	private NormNode(Operator op, UList<NormNode> nodes) {
		super(op, nodes);

		this.nodes = nodes;
		this.oneList = mkUList1(this);
	}

	protected String monoToString() {
		return "P";
	}

	@Override
	public int compareTo(NormNode that) {
		//		return chainCompare(this.op, that.op, UList.chainCompare(this.nodes, that.nodes, 0));
		int cmp = chainCompare(this.op, that.op);
		if (cmp == 0) {
			return chainCompare(this.nodes, that.nodes);
		}
		return cmp;
	}

	public UList<NormNode> allSubNodes() {
		if (isLeaf)
			return oneList;
		return oneList.concat(nodes.flatMap(n -> n.allSubNodes()));
	}

	public boolean isRedundant() {
		// special exception:  P/Psum ok at top level
		if (op == DIV && nodes.last().isPsum && (nodes.first().isML))
			return false;

		boolean rval = isRedundantAux();
		//				printf("RED? %s %s%n", this, rval);
		return rval;
	}

	//private
	public boolean isRedundantAux() {
		//		if (!Main.mms_hack)
		//		return false; // what was this about????

		//		if (true)
		//			return false;
		//		printf("RED? %s %s%n", this, Main.mms_hack);

		// P/P  P/Psum) Psum/P  all redundant with neg exponents
		if (op == DIV && (nodes.all(m -> m.isML) || (nodes.some(m -> m.isML) && nodes.some(m -> m.isPsum)
		// hmmm... don't drop P/Psum for now
				&& right().isML)))
			return true;

		// hmmm... not P/Psum
		// but... if we enable this, we get lots of odd things like (P*((P/P)+(P/P)))    
		// instead, just view (P+P)/(P+P) as subsuming P/(P+P)
		//		if (op == DIV && nodes.first().isPsum && (nodes.last().isML))
		//			return true;

		// solving P+P+P+P is easier than (P+P)*(P+P) (perhaps mostly due to constand and exponent limitations).
		// based on that one example, just ignore any such expressions
		//		if (subnodes.some(n -> n.op == MUL && (n.nodes.count(m -> m.op == ADD) >= 2
		//				|| (n.nodes.some(m -> m.op == ADD) && n.nodes.some(m -> m.isML)))))
		if (op == MUL && nodes.count(m -> m.isML || m.isPsum) >= 2)
			return true;
		//		if (op == MUL)
		//			printf("ISR: %s%n", this);

		if (nodes.some(n -> n.isRedundantAux()))
			return true;

		// expn(expn(...))
		if (op == POW && nodes.first().op == POW)
			return true;

		// pow(M) - builtin to P   pow(M+M)
		if (op == POW && nodes.first().isML)
			return true;

		if (op == DIV || op == MUL)
			// expn(X)*expn(Y) ==> expn(X*Y) 
			// expn(X)/expn(Y) ==> expn(X/Y) 
			if (nodes.count(m -> m.op == POW) >= 2)
				return true;

		return false;
	}

	public boolean hasExpP() {
		// exp(P) is a primitive
		return allSubNodes().some(n -> (n.op == POW && n.nodes.first().isML));
	}

	public static final int addexp_limit = 5; // how many add exps in total?
	public static final int addends_limit = 4; // how many addends in a given ADD?

	// (P+P+P)*...
	public static final int addends_prod_limit = 2; // how many addends in a given ADD that is multiplied by something?

	public boolean isLikely() {
		//UMap<Operator, Integer> ops = allSubNodes().filter(x -> !(x instanceof MultiNode)).map(x -> x.op).popCount();
		if (nodes.some(n -> !n.isLikely()))
			return false;
		UList<NormNode> subnodes = allSubNodes();
		UList<NormNode> addexps = subnodes.filter(x -> x.op == ADD); // && x != this);
		if (addexps.size() > addexp_limit) // || concatULists(addexps.map(x -> x.nodes)).size() > 4)
			return false;

		if (addexps.some(x -> x.nodes.size() > addends_limit))
			return false;

		if (op == MUL && nodes.some(x -> x.isPsum && x.nodes.size() > addends_prod_limit))
			return false;

		// if we drop products of sums, we need large sums...
		if (false)
			if (op == ADD && nodes.size() > 3)
				return false;

		//		if (subnodes.count(x -> x.op == LN) > 1)
		//			return false;

		return true;
	}

	public INode mkinstance(Input inpx) {
		//		printf("mkInstance xx--- %s%n", this);
		if (inpx.pow_values.isNotEmpty())
			myassert(inpx.pow_values.size() == 1);
		INode rval = new INode.MkInstance(inpx).mkinstanceAux(this);
		return rval;
	}

	private NormNode _arithNormDontUse;

	//	public final static UList<NormNode> leaves = mkUList1(new ) 

	//	private static LinkedHashMap<ExpPower, LinkedHashMap<NormNode, NormNode>> pow_univ;

	//	public static NormNode invleaf = new NormNode(INVLEAF, true, UList.empty());

	//getNormNode(Operator.ONE, UList.empty());
	//	private static NormNode invleafNoConst = new NormNode(INVLEAF, false, UList.empty());

	// C*C*C ...
	//	public boolean isLeafProd() {
	//		return op == MUL || isMultiLeaf;
	//	}
	//
	//	// (C*C*C...)/(C*C*C ...)
	//	public boolean isLeafProdDiv() {
	//		return //op == MUL || 
	//		(op == DIV && left().isLeafProd() && right().isLeafProd());
	//	}

	//	private boolean invLeaf() {
	//		return op == Operator.INVLEAF;
	//	}

	//	public NormNode inv() {
	//		myassert(op == LEAF);
	//		myassert(allowConst);
	//		return invl
	//	}

	//	public static NormNode getNormNode(Operator op, UList<NormNode> nodes) {
	//		return getNormNode(op, true, nodes);
	//	}

	//	private static NormNode getNormNode(Operator op, boolean allowConst, UList<NormNode> nodes) {
	//
	//	public static NormNode getNormNode(Operator op, UList<NormNode> nodes) {
	//		INode n = getINode(op, nodes.map(x -> x));
	//		myassert(n.isNormNode);
	//		return (NormNode) n;
	//	}
	//	private static LinkedHashMap<Operator, NormNode> leaf_univ = new LinkedHashMap<>();
	//
	//	public static NormNode getNormNode(Operator op) {
	//		return leaf_univ.computeIfAbsent(op, op2 -> new NormNode(op2, UList.empty()));
	//	}

	// used to create initial P
	public static NormNode getNormML() {
		return getNormNode(IS_LEAF, UList.empty());
	}

	private static LinkedHashMap<UList<Object>, NormNode> univ = new LinkedHashMap<>();

	// used to gather up */+ subterms, implements associativity
	private static UList<NormNode> getOpNodes(Operator bop, NormNode node) {
		if (node.op != bop)
			return mkUList1(node);
		return node.nodes.flatMap(n -> getOpNodes(bop, n));
	}

	private static UList<NormNode> getOpNodes(Operator bop, UList<NormNode> nodes) {
		return nodes.flatMap(n -> getOpNodes(bop, n));
	}

	public static NormNode getNormNode(Operator op, UList<NormNode> nodesIn) {
		UList<NormNode> nodes = op.addOrMul ? sort(getOpNodes(op, nodesIn)) : nodesIn;
		return univ.computeIfAbsent(consx(op, nodes), xyz -> new NormNode(op, nodes));
	}

	public static NormNode getNormNode(Operator op, NormNode left, NormNode right) {
		return getNormNode(op, mkUList1(left, right));
	}

	//	public static NormNode getNormNodePow(NormNode base, ExpPower pow) {
	//		LinkedHashMap<NormNode, NormNode> ns = pow_univ.get(pow);
	//		if (ns == null) {
	//			myassert(pow.isLiteral);
	//			pow_univ.put(pow, new LinkedHashMap<>());
	//
	//			myassert(pow.isLiteral);
	//			ns = pow_univ.get(pow);
	//		}
	//
	//		if (ns.containsKey(base))
	//			return ns.get(base);
	//		de n = new NormNode(base, pow);
	//		ns.put(base, n);
	//		return n;
	//	}

	//	private NormNode(NormNode base, ExpPower exp) {
	//		super();
	//		this.op = POW;
	//		this.allowConst = false;
	//		this.isLeaf = false;
	//		this.isLit = op.isLit;
	//		this.isMultiLeaf = false;
	//		this.nodes = base.oneList;
	//		this.leafNodes = nodes.filter(x -> x.isLeaf);
	//
	//		this.oneList = mkUList1(this);
	//
	//		this.exp_dont_use = exp;
	//
	//		this.depth = op.arity == 0 ? 0 : 1 + max(nodes.map(n -> n.depth));
	//		this.complexity = op.arity == 0 ? 1 : 1 + sum(nodes.map(n -> n.complexity));
	//	}
	//
	//	@Override
	//	public int compareTo(NormNode that) {
	//		//		return chainCompare(this.op, that.op, UList.chainCompare(this.nodes, that.nodes, 0));
	//		int cmp = chainCompare(this.op, that.op);
	//		if (cmp == 0) {
	//			if (this.isML) {
	//				myassert(that.isML);
	//				return this.thisAsMLI.nodeNm.compareTo(that.thisAsMLI.nodeNm);
	//			}
	//			return chainCompare(this.nodes, that.nodes);
	//		}
	//		return cmp;
	//	}

	//	private boolean leafNoConst() {
	//		//return isLeaf && !allowConst;
	//		return isML && !inode.thisAsMLI.allowConst;
	//	}
	//
	//	public boolean containsLeafC() {
	//		if (isLeaf)
	//			return !this.leafNoConst();
	//		return nodes.some(n -> n.containsLeafC());
	//	}
	//
	//	public boolean containsLeafCOp(Operator op2) {
	//		if (isLeaf)
	//			return !this.leafNoConst();
	//		if (op == op2)
	//			return nodes.some(n -> n.containsLeafCOp(op2));
	//		return false;
	//	}
	//
	public boolean containsSubDiv() {
		if (op == DIV)
			return true;
		if (op == MUL)
			return nodes.some(n -> n.containsSubDiv());
		return false;
	}

	public final NormNode left() {
		myassert(nodes.size() == 2);
		return nodes.get(0);
	}

	public final NormNode right() {
		myassert(nodes.size() == 2, nodes);
		return nodes.get(1);
	}

	//	public NormNode rewrite() {
	//		if (op.arity == 0)
	//			return this;
	//
	//		UList<NormNode> nodes = this.nodes.map(n -> n.rewrite());
	//
	//		UList<NormNode> ps = nodes.filter(n -> n.isML);
	//		UList<NormNode> psums = nodes.filter(n -> n.op == ADD && n.nodes.all(m -> m.isML));
	//		if (ps.size() + psums.size() == nodes.size()) {
	//			if (op == MUL)
	//				if (psums.isEmpty()) {
	//					myassert(ps.isNotEmpty());
	//					// P*P*...*P ==> P
	//					return ps.any();
	//				} else {
	//					// P*(P+...+P) ==> (P+...+P)
	//					// (P+...+P)*(P+...+P) ==> (P+...+...+P) (m*n summands)
	//					return getINode(op, psums.reduce(UList.empty(), (ns, n) -> n.nodes.concat(ns)));
	//				}
	//			else if (op == DIV)
	//				if (nodes.get(0).isML)
	//					// P/(P+...+P) ==> 1/(P+...+P)
	//					return getINode(op, ONE_NODE, nodes.get(1));
	//				else if (nodes.get(1).isML)
	//					// (P+...+P)/P ==> (P+...+P)  IGNORES POSSIBLE NEG EXPONENTS
	//					return nodes.get(0);
	//		}
	//		return getINode(op, nodes);
	//	}

	// (P+...)/P ==> P+(...)/P
	public final NormNode mergeDivP() {
		NormNode rval = this;

		if (op == DIV && right().isML && left().op == ADD && left().nodes.some(x -> x.isML)) {
			rval = getNormNode(ADD, left().nodes.filter(x -> x.isML)
					.concat(left().nodes.filter(x -> !x.isML).map(x -> getNormNode(DIV, mkUList1(x, right())))));
			printf("MERGING %s -> %s%n", this, rval);
		}
		return rval;
	}

	public final NormNode arithNorm() {
		if (_arithNormDontUse != null)
			return _arithNormDontUse;

		NormNode rval = this;

		if (isML)
			return this;

		//		if (op == MUL) {
		//			UMap<NormNode, Integer> powNodes = nodes.filter(n -> n.op.isPow).popCount().filterByValues(num -> num > 1);
		//			if (!powNodes.isEmpty()) {
		//				// cbrt(XYZ)*cbrt(XYZ) => cbrt(XYZ*XYZ)			
		//				UList<NormNode> rest = nodes.diff(powNodes.keySet());
		//				if (rest.isEmpty() && powNodes.size() == 1) {
		//					NormNode base = powNodes.keySet().first();
		//					int ncopies = powNodes.get(base);
		//					return getNormNodePow(getNormNode(MUL, nCopies(ncopies, base)), exponent().litval()).arithNorm();
		//				} else {
		//					die("FIX THIS");
		//					//return getNormNode(CBRT, getNormNode(MUL, xyz1.concat(xyz1)).oneList).arithNorm();
		//					return null;
		//				}
		//			}
		//
		//			//			return getNormNode(op, sort(nodes.map(sn -> sn.arithNorm())));
		//		}
		if (op == DIV && nodes.some(n -> n.containsSubDiv()))
			rval = normDivs(nodes.get(0).getOpNodes(MUL), nodes.get(1).getOpNodes(MUL)).arithNorm();

		else if (op == MUL && nodes.some(n -> n.op == DIV))
			rval = getNormNode(DIV,
					getNormNode(op,
							nodes.filter(n -> n.op != DIV).concat(nodes.filter(n -> n.op == DIV).map(n -> n.left()))),
					normRed(MUL, nodes.filter(n -> n.op == DIV).map(n -> n.right()))).arithNorm();

		//		else if (Input.group_addmul && op.addOrMul && leafNodes.size() > 1 && leafNodes.size() < nodes.size()) {
		//			// more than one leaf, group together
		//			rval = getNormNode(op, nodes.filter(n -> !n.isLeaf).add(getNormNode(op, nodes.filter(n -> n.isLeaf))))
		//					.arithNorm();

		//				myassert(rval.nodes.all(n -> n.op != op), this);
		//		}
		//		else if (op.isPow) {
		//			//			if (base().op == MUL)
		//			// cbrt(L * L) => cbrt(L) * cbrt(L)
		//			// allows us to kill consts, but not combine leaves
		//			//				return getNormNode(MUL, base().nodes.map(n -> getNormNodePow(n, exponent()))).arithNorm();
		//			rval = getNormNodePow(base().arithNorm(), exponent());
		//	}
		else
			//		printf("NORM %s%n", this);
			rval = getNormNode(op, nodes.map(sn -> sn.arithNorm()));

		myassert(rval.leaves().size() == this.leaves().size());
		_arithNormDontUse = rval;
		return rval;
	}

	// returns prod(left) / prod(right)
	// st no child of left or right is DIV or MUL
	private static NormNode normDivs(//Operator op,
			UList<NormNode> left, UList<NormNode> right) {
		//		printf("NORMDIVS %s %s%n", left, right);
		// A/(B/C) ==> (A*C)/B
		//		myassert(nodes.size()==2);
		//		NormNode left = nodes.get(0);
		//		NormNode right = nodes.get(1);

		UList<NormNode> left_divs = left.filter(n -> n.op == DIV);
		UList<NormNode> left_nondivs = left.filter(n -> n.op != DIV).flatMap(n -> n.getOpNodes(MUL));
		UList<NormNode> right_divs = right.filter(n -> n.op == DIV);
		UList<NormNode> right_nondivs = right.filter(n -> n.op != DIV).flatMap(n -> n.getOpNodes(MUL));
		if (left_divs.isNotEmpty() || right_divs.isNotEmpty())
			return normDivs(left_nondivs.concat(left_divs.map(n -> n.left())).concat(right_divs.map(n -> n.right())),
					right_nondivs.concat(left_divs.map(n -> n.right())).concat(right_divs.map(n -> n.left())));

		//		myassert(left.isNotEmpty());
		//		myassert(right.isNotEmpty());

		if (right_nondivs.size() == 0)
			return NormNode.normRed(MUL, left_nondivs);
		if (left_nondivs.size() == 0)
			die();

		myassert(MUL != null); // assumes MUL is a valid op - fix
		return getNormNode(DIV, mkUList1(NormNode.normRed(MUL, left_nondivs), NormNode.normRed(MUL, right_nondivs)));

		//		if (left.isEmpty())
		//			if (right.isEmpty()) {
		//				die();
		//				return null;
		//			}
		//			else 
		//(mkUList1(NormNode.getNormNode(MUL, nodes.get(0), nodes.get(1).nodes.get(), 

	}

	//	private final NormNode dropConsts() {
	//		if (isLeaf) {
	//			return leafNoConst;
	//			//return (op == LEAF) ? leafNoConst : invleafNoConst;
	//		}
	//		//		if (op.isPow)
	//		//			return getNormNodePow(base().dropConsts(), exponent());
	//
	//		return getNormNode(op, nodes.map(n -> n.dropConsts()));
	//	}

	public final NormNode maybeDropConsts() {
		return this;
		//		if (isLeaf) {
		//			return this;
		//			//return (op == LEAF) ? leafNoConst : invleafNoConst;
		//		}
		//		
		//		//		if (op.isPow)
		//		//			return getNormNodePow(base().dropConsts(), exponent());
		//		if (op==DIV && left().isML && right().op==MUL && right().nodes.all(n->n.isML))
		//			// P/(P + P + ...) ==> P/(Pnc + P + P)
		//			return 
		//		return getNormNode(op, nodes.map(n -> n.maybeDropConsts()));
	}

	// MUL/ADD terms
	//	private static UList<NormNode> maybeDropConsts(UList<NormNode> ns) {
	//
	//	}

	//	private static UList<NormNode> maybeDropConstsList(UList<NormNode> ns, int mconsts) {
	//		if (mconsts == 1)
	//			return concatULists(ns.popCount()
	//					.bimap((n, i) -> cons(n.maybeDropConsts(mconsts), UList.nCopies(i - 1, n.dropConsts()))));
	//		return ns.map(n -> n.maybeDropConsts(mconsts));
	//	}
	//
	//	private final NormNode maybeDropConstsAddMul(boolean noLeafConsts, int mconsts) {
	//		if (noLeafConsts && isLeaf)
	//			return leafNoConst;
	//
	//		myassert(op.addOrMul);
	//		// This is true regardless of the max number of allowed consts (so long as > 0)
	//		//if (isLeafProdDiv() && 
	//
	//		if (noLeafConsts && leafNodes.isNotEmpty())
	//			return getNormNode(op, UList.nCopies(leafNodes.size(), leafNoConst)
	//					.concat(nodes.filter(n -> !n.isLeaf).map(n -> n.maybeDropConsts(mconsts))));
	//
	//		// C + sqrt(C) ==> C + sqrt(L)
	//		// C * sqrt(C) ==> C * sqrt(L)
	//		if (nodes.some(n -> n.containsLeafCOp(op)) && nodes.some(n -> n.op.isPow && n.containsLeafC())) {
	//			// probably should be able to drop this tree completely
	//			printf_once(">>>>> DROPPING ROOT CONST - implicitly deriving from leaf const, BUT NOT COMPUTING YET!%n");
	//			UList<NormNode> nonpows = nodes.filter(n -> !(n.op.isPow && n.containsLeafC()));
	//			UList<NormNode> pows = nodes.filter(n -> n.op.isPow && n.containsLeafC()).map(n -> n.dropConsts());
	//
	//			return getNormNode(op, nonpows.concat(pows)).maybeDropConsts(mconsts);
	//
	//		}
	//
	//		// group MUL/ADD children by structure
	//		// if only one const, then
	//		// for each group, only one member may have a const
	//		// with more than one const (say n),
	//		// only n leaves need to const; don't know about non-leaves
	//		if (mconsts == 1 || nonMulLeaves().isLit) {
	//			UMap<NormNode, Integer> powNodes = nodes.popCount();
	//			//			printf("DR %s %s%n", powNodes,
	//			//					concatULists(powNodes.bimap((n, i) -> cons(n, UList.nCopies(i - 1, n.dropConsts())))));
	//			return getNormNode(op, concatULists(
	//					powNodes.bimap((n, i) -> cons(n.maybeDropConsts(mconsts), UList.nCopies(i - 1, n.dropConsts())))));
	//		}
	//
	//		if (leafNodes.size() > 0)
	//			return getNormNode(op, cons(leafNodes.first(), UList.nCopies(leafNodes.rest().size(), leafNoConst))
	//					// can probably do better, don't know how 
	//					.concat(nodes.filter(n -> !n.isLeaf)));
	//
	//		return getNormNode(op, nodes.map(n -> n.maybeDropConsts(mconsts)));
	//	}

	private boolean containsMulLeaves() {
		// mulLeaves
		return isLeaf || (op == MUL && numML > 0);
	}

	//	public UList<NormNode> mulLeaves() {
	//		if (isLeaf)
	//			return oneList;
	//		if (op == MUL)
	//			return leafNodes;
	//		return UList.empty();
	//	}

	// everything EXCEPT the mulLeaves()
	//	public NormNode nonMulLeaves() {
	//		if (isLeaf)
	//			return ONE_NODE;
	//		if (op == MUL)
	//			return normRed(MUL, nodes.filter(n -> !n.isLeaf));
	//		else
	//			return this;
	//		}
	//		private static final NormNode leafNoConst = new NormNode(IS_LEAF, false, UList.empty());
	private static final NormNode ONE_NODE = new NormNode(ONE, UList.empty());

	public static NormNode normRed(Operator op, UList<NormNode> nodes) {
		if (op.addOrMul)
			if (nodes.isEmpty()) {
				return ONE_NODE;
				//				die();
				//				return null;
			} else if (nodes.size() == 1)
				return nodes.get(0);
			else
				return getNormNode(op, nodes);
		die();
		return null;
	}

	public final UList<NormNode> leaves() {
		return isLeaf ? mkUList1(this) : nodes.flatMap(n -> n.leaves());
	}

	// used to gather up */+ subterms, implements associativity
	public UList<NormNode> getOpNodes(Operator bop) {
		if (op != bop)
			return oneList;
		return nodes.flatMap(n -> n.getOpNodes(bop));
	}

	//
	//	public static NormNode normalizePow(Operator op, BNode base, ExpPower exp_pow) {
	//		if (op.isPow)
	//			return NormNode.getNormNodePow(base.norm, exp_pow);
	//		die();
	//		return null;
	//	}
	//
	//	public static NormNode normalize(Operator op, UList<BNode> nodes) {
	//		if (!op.addOrMul)
	//
	//			return NormNode.getNormNode(op, nodes.map(n -> n.norm));
	//		else
	//			return NormNode.getNormNode(op, sort(getOpNodes(op).map(n -> n.norm)));
	//	}

}
