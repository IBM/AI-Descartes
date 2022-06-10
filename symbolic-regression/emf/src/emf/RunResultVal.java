// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package emf;

import static utils.UList.*;
import static utils.VRAUtils.*;

import java.io.PrintStream;

import utils.Pair;
import utils.UList;
import utils.UMap;

public class RunResultVal extends RunResult {
	public final boolean optimal;
	public final double val;
	public final double wallClockTime;

	// if a boolean is assigned a value larger than this, it is considered 1.
	public final double ZERO_THRESH = 1e-2;

	public final UMap<Integer, Boolean> use_c_map;
	public final UMap<Integer, Double> c_map;
	public final UMap<MLInstance, Double> ci_map;
	public final UMap<Integer, String> c_map_strs;
	public final UMap<Integer, UMap<String, Boolean>> use_i_map;
	public final UMap<Integer, UList<Double>> var_exp_map;

	public final UMap<ExpInstance, Double> expn_map;

	public final UMap<ExpInstance, Double> Fexp_map;

	public final ResForm raw_resform;
	public final ResForm resform;
	public final PrintStream dbgout;

	public String toString() {
		return baronJob.toString();
	}

	public RunResultVal(BaronJob baronJob, UList<String> output, UList<String> resout, UList<String> timout) {
		super(baronJob, output);

		//		printf("RESVAL %s%n", baronJob.expr);

		this.dbgout = orElse(baronJob.dbgout, System_out);
		//		this.dbgout = System_out;

		boolean normal_completion = output.some(s -> s.contains("Normal completion"));

		// p12
		//		"Best solution found at node"

		//		if ((normal_completion || heuristic_completion || max_time_exceeded) && !infeasible) {
		// presumably this means optimal???
		//			printf("NORMAL COMPLETION %s%n", baronJob.index);

		this.optimal = !(max_time_exceeded || heuristic_completion);
		int st = someSolutionFound(resout);
		myassert(st >= 0);

		final Input inpx = baronJob.inpx;
		/*
			The best solution found is:
		
		  variable		xlo			xbest			xup
		  USE_C0			0			0			1
			...
		 */
		UList<String> prefixes = mkUList1(ExpInstance.expmul_prefix, "dadiv", "damul");
		UMap<String, String> raw_map0x = resout.subList(st)
				.filter(s -> prefixes.some(pre -> s.startsWith("  " + pre)) || s.startsWith("  USE")
						|| s.startsWith("  use") || s.startsWith("  addxi") || s.startsWith("  exp")
						|| s.startsWith("  sub") || s.startsWith("  C"))
				.mkmap2(s -> {
					UList<String> ss = split(s.trim(), "\\s+");
					dbgout.printf("ss %s %s%n", s, ss);
					return new Pair<>(ss.get(0), ss.get(2));
				});
		UList<String> varlst = resout.allAfter(s -> s.startsWith("  variable")).allBefore(s -> s.trim().isEmpty())
				.map(s -> split(s.trim(), "\\s+").get(0));
		//				raw_map0x.dump("--0x");
		//				varlst.dump("--vl");
		myassertEqual("varlst", varlst, raw_map0x.keySet(), baronJob.expr);

		int vvind = resout.findLastIndex(s -> s.startsWith(" >>> Variable no.              Value"));
		myassert(vvind >= 0);
		UList<String> vartbl = resout.subList(vvind + 1).allBefore(s -> s.trim().isEmpty());
		myassert(vartbl.size() == varlst.size());
		UMap<String, String> raw_map0 = rangeOpen(varlst.size())
				.mkmap2(i -> new Pair<>(varlst.get(i), split(vartbl.get(i).trim(), " ").last()));
		//				for (String s : varlst)
		//					printf("XX %s %s %s%n", s, raw_map0x.get(s), raw_map0.get(s));

		final UList<Integer> mls = rangeOpen(0, baronJob.expr.numML);
		final UList<Integer> mlsConst = baronJob.multiLeavesNotFixedWithConst.map(x -> x.nodeNo);

		final UList<IntExpSplit> isplits = baronJob.splits.filterSubType(IntExpSplit.class);

		UMap<String, String> raw_map = isplits.isEmpty() ? raw_map0
				: raw_map0.putAll(isplits.mkmap2(sp -> newPair("exp" + sp.mlsplit.nodeNo + sp.var, "" + sp.expval)));

		final boolean limit_consts = !nullOrZero(inpx.max_num_consts)
				&& baronJob.nodeInstance.mlInstances().size() > baronJob.inpx.max_num_consts;
		UMap<Integer, Integer> sub_map = mls
				.mkmap(nodeNo -> limit_consts && inpx.subConstInObj ? (int) toDouble(raw_map.get("sub" + nodeNo)) : 0);
		final UList<MLInstance> mlsx = baronJob.nodeInstance.mlInstances();

		//				printf("bni %s %s%n", baronJob.expr, baronJob.nodeInstance);
		//				printf("%nexpr%s %s%n", baronJob.index, raw_map);
		this.use_c_map = inpx.multiply_use_c && limit_consts
				? mls.mkmap(nodeNo -> toDouble(raw_map.get("USE_C" + nodeNo)) > ZERO_THRESH)
				: mls.mkmap(nodeNo -> true);

		this.c_map_strs = mlsConst.mkmap(nodeNo -> raw_map.get("C" + nodeNo));
		this.c_map = c_map_strs.mapValues((i, sval) -> use_c_map.get(i) ? toDouble(sval) - 2 * sub_map.get(i) : 1.0);
		this.use_i_map = inpx.drop_usex ? UMap.empty()
				: mls.mkmap(nodeNo -> inpx.invarnms
						.mkmap(nm -> 1 == (int) Math.round(toDouble(raw_map.get("use" + nodeNo + nm)))));

		this.ci_map = mlsx.filter(x -> raw_map.containsKey(x.Cinm()))
				//.mkmap(x -> checkToInt( raw_map.get(x.Cinm()), x.Cinm()));
				.mkmap(x -> toDouble(raw_map.get(x.Cinm()), x.Cinm()));

		//				final UList<INode> psums = baronJob.nodeInstance.allSubNodes().filter(x -> x.isPsum);

		//		printf("NODEINSTS %s %s %s%n", baronJob.expr, baronJob.nodeInstance, baronJob.nodeInstance.allSubNodes());
		UMap<Integer, INode> mlmap = baronJob.nodeInstance.allSubNodes().filter(x -> x.isML)
				.mkrevmap(x -> x.thisAsMLI.nodeNo);
		final int instno = baronJob.expr.nodeNum.get();

		//this.Fexp_map = mls.mkmap(nodeNo -> toDouble(raw_map.getOrElse(ExpInstance.expmul_prefix + nodeNo, "1")));
		this.Fexp_map = baronJob.nodeInstance.allSubNodes(x -> x.expInstance != null && !x.expInstance.powIsFixed)
				.map(x -> x.expInstance).mkmap(expinst -> toDouble(raw_map.getOrElse(expinst.nm, "1")));

		this.var_exp_map = mls.mkmap(nodeNo -> inpx.invarnms.map(nm -> {
			double e = toDouble(raw_map.get("exp" + nodeNo + nm));
			int addxi = (int) Math.round(toDouble(raw_map.getOrElse("addxi" + nodeNo + nm, "0")));
			INode mli = mlmap.get(nodeNo);
			//			if (mli.thisAsMLI.fractional) {
			//				//double exm = toDouble(raw_map.get(mli.thisAsMLI.relaxed_expmul()));
			//				double exm = toDouble(raw_map.get(mli.expInstance.nm));
			//				myassert(addxi == 0); // ??
			//				printf("FRAC %s %s%n", mli.thisAsMLI.nodeNo, exm);
			//				return e * exm + addxi;
			//			}

			// doesn't work for all_base_e
			// hmmm.. yes it does
			// don't use anyway, for rounding
			if (false)
				if (mli.expInstance != null) {
					double f = toDouble(raw_map.get(mli.expInstance.nm));
					//							if (inpx.all_base_e)
					//								f = Math. f;...
					//				printf("EXP %s %s: %s (%s %s)%n", instno, nodeNo, f * e, f, e);
					return f * e + addxi;
				}
			//					printf("EXP %s %s: %s (%s %s)%n", instno, nodeNo, (e - en), e, en);
			return e + addxi;
		}));

		this.expn_map = baronJob.nodeInstance.allSubNodes().filter(x -> x.op == Operator.POW).map(x -> x.expInstance)
				.mkmap(x -> toDouble(raw_map0.get(x.nm)));

		dbgout.printf("raw_map   %s%n", raw_map);
		if (use_c_map.isNotEmpty())
			dbgout.printf("use_c_map %s%n", use_c_map);
		if (limit_consts && inpx.subConstInObj)
			dbgout.printf("    sub_map %s%n", sub_map);
		//		dbgout.printf("    c_map %s%n", c_map_strs);
		c_map.forEachEntry((nodeNo, vl) -> dbgout.printf("$C%s = %s;%n", nodeNo, vl));
		dbgout.printf("use_i_map %s%n", use_i_map);
		//		dbgout.printf("var_exp_map %s%n", var_exp_map);
		var_exp_map.forEachEntry((nodeNo, vls) -> vls
				.forEachI((i, vl) -> dbgout.printf("$exp%s%s = %s;%n", nodeNo, inpx.invarnms.get(i), vl)));

		String lastline = resout.last();
		String tag = "The above solution has an objective value of:";
		myassert(lastline.startsWith(tag));
		this.val = toDouble(lastline.replace(tag, "").trim());

		if (heuristic_completion) {
			if (timout.isEmpty()) {
				printf("BAD TIMOUT FILE! %s%n", timout);
				this.wallClockTime = -1;
			} else {
				//[problem, 0, 6, 1291, 54, 0.00000000000, 72.5299661210, 12, 4, 0, 1350, 862, 430, 4.03, 4.15]
				//					printf("TIMFILE: %s%n", split(timout.first(), " +"));
				UList<String> info = split(timout.first(), " +");

				// just checking that the val given agrees
				if (Math.abs(val) < 1e5)
					// really large values still fail
					myassert(Math.abs(toDouble(info.get(6)) - val) < 1e-4,
							mkUList1(timout, "" + val, baronJob.baronInputName));
				this.wallClockTime = toDouble(info.last());
			}
		} else
			this.wallClockTime = -1;

		RunResultVal result = this;
		int index = baronJob.index;
		NormNode expr = baronJob.expr;
		dbgout.print("RX ");
		runresult_expr_for_debugging = expr;
		this.raw_resform = baronJob.nodeInstance.resultForm(result);
		if (result.infeasible)
			dbgout.printf("%4s: %3s %s", index, "INF", raw_resform);
		else
			dbgout.printf("%4s: %3s %6.3s %s", index, optimal ? "OPT" : "", val, raw_resform);
		dbgout.println("   " + expr);

		myassert(max_time_exceeded || infeasible || normal_completion || heuristic_completion,
				"Unknown result for BARON run " + baronJob.baronInputName + "\n" + output.join("\n   "));

		this.resform = raw_resform.simpform();

		double xv = evalObjective(baronJob.inpx2.input);
		double absdiff = Math.abs(xv - val);
		double reldiff = absdiff / Math.min(xv, val);
		if (absdiff > 1e-6 && reldiff > 1e-3) {
			ProdVal.raw_hack = true;
			double raw_xv = evalObjective(baronJob.inpx2.input);
			ProdVal.raw_hack = false;
			double raw_absdiff = Math.abs(raw_xv - val);
			double raw_reldiff = absdiff / Math.min(raw_xv, val);
			if (!(raw_absdiff > 1e-6 && raw_reldiff > 1e-3))
				printf("(small eval diff, due to int var rounding: %s %s %s %s)%n", baronJob.expr.nodeNum.get(),
						resform, val, raw_xv);
			else {
				printf("EVAL DIFF! %s %s %s %s%n", baronJob.expr.nodeNum.get(), resform, val, xv);
				if (dbgout == System_out)
					die();
			}
		}
	}

	private static int checkToInt(String str, String nm) {
		double x = toDouble(str);
		if (Math.abs(x - Math.round(x)) > 1e-3)
			die("The value for %s isn't an int: %s", nm, str);
		return (int) Math.round(x);
	}

	public static NormNode runresult_expr_for_debugging;

	//	PVALS: [{P0=-6.193070960915512, P1=0.008928541856452692, P2=16.407931339174603, P3=-0.008952054701627911}, {P0=-18.579373107349056, P1=0.8890384392400772, P2=40.24554658303474, P3=-0.8907928598519628}, {P0=-55.7386, P1=88.5239, P2=98.7147, P3=-88.6402}, {P0=-167.2172420462944, P1=8814.557982339193, P2=242.12845453558262, P3=-8820.327833954278}, {P0=-501.65605231507436, P1=877688.7645485523, P2=593.8952202234287, P3=-877685.1033552337}]
	//			VALS: [10.214836865413917, 21.664419055073797, 42.85980000000001, 69.14136087420411, 95.90036122710444]
	//			EVALLED 4 ((-55.7386*x^0.477125)+(88.5239*x^1.99814)+(98.7147*x^0.389664)+(-88.6402*x^1.997854)) 7.258827098310169E-7 112.93686093569703

	// This may be sum-of-squares or a plain diff
	public double evalObjective(UList<DataLine> data) {
		INode expr = baronJob.nodeInstance;
		UMap<MLInstance, ProdVal> pvals = expr.mlInstances().mkmap(mli -> ProdVal.getProdVal(this, mli));
		UList<Double> vals = data.map(line -> expr.eval(pvals.mapValues((mli, pv) -> pv.eval(line, mli)), expn_map));
		//		UList<Double> vals = data.map(line -> resform.eval(line));

		dbgout.printf("NMI %s %s%n", expr.mlInstances().size(), pvals.size());
		dbgout.printf("PVALS:%n");
		data.forEach(line -> pvals.forEachEntry((mli, pv) -> dbgout.printf(" %s: %s%n", mli, pv.eval(line, mli))));
		dbgout.printf("VALS: %s%n", vals);
		UList<Double> errs = data
				.mapI((i, line) -> baronJob.inpx.relative_squared_error ? vals.get(i) / line.outval - 1.0
						: line.outval - vals.get(i));
		dbgout.printf("DIFFS: %s%n", errs);
		//		printf("%s%n", data.map(line -> resform.eval(line)));

		return sumd(baronJob.inpx.diff_in_obj ? baronJob.inpx.diff_in_obj_y_sub_fx ? errs : errs.map(d -> -d)
				: errs.map(d -> d * d));
	}
}
