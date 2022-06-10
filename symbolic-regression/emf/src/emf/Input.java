// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package emf;

import static emf.Operator.*;
import static utils.DRange.*;
import static utils.UList.*;
import static utils.VRAUtils.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Optional;
import java.util.Random;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import net.sourceforge.yamlbeans.YamlException;
import utils.DRange;
import utils.Noise;
import utils.OList;
import utils.UList;
import utils.UMap;
import utils.VRAFileUtils;

public class Input extends YamlInput {

	public final double infinity = 1e50;

	public final UList<DataLine> unsampled_input_without_noise;
	public final UList<DataLine> unsampled_input;
	public final UList<String> invarnms;
	public final UList<InVar> invars;
	public final String outvarnm;
	public final int numInVars;
	public final OList<Integer> inVarRng;
	public final UList<Integer> invarSeq;

	public final NormNode mlprod;
	public final UList<NormNode> mlleaves;
	public final UList<NormNode> twoprods;

	public final UMap<InVar, UList<Integer>> dimensionVars;
	public final UList<DimUnit> dimensionUnits; // overrides InputYaml.dimensionUnits
	public final int ndimunits;
	public final UMap<InVar, UMap<DimUnit, Integer>> dimensionVarsByDimUnit;
	public final UMap<DimUnit, Integer> outvarDimUnits;

	//	public final UList<Integer> invarDimMask;
	//	public final int outvarDimMask;
	public final long[] invar_dim_masks;
	public final long outvar_dims;

	public final MConst mconst = MConst.getMConst(min_const_val, max_const_val);

	public final DRange crng = newDRange(min_const_val, max_const_val);

	public final PrintStream dbg;

	public final static int dim_mask_sz = 8; // 4 bits can represent up to [-7,7], plus overflow bit
	public final static int dim_mask_bits = (1 << dim_mask_sz) - 1;
	public final static long used_mask = dim_mask_bits | ((long) dim_mask_bits << 2 * 8)
			| ((long) dim_mask_bits << 4 * 8) | ((long) dim_mask_bits << 6 * 8);

	//	public long dim_mask(UList<Integer> dims) {
	//		final int ndimunits = this.ndimunits;
	//		long msk = 0;
	//		for (int di = 0; di < ndimunits; ++di) {
	//			msk |= ((dims.get(di) & dim_mask_bits) << (dim_mask_sz * 2 * di));
	//
	//			printf("MSK %s %s %s %s%n", di, msk, (dims.get(di) & dim_mask_bits), dims.get(di));
	//			//			dump(msk << (Integer.BYTES * 8 - (dim_mask_sz * di)));
	//			//			dump((msk << (Integer.BYTES * 8 - (dim_mask_sz * di))) & dim_mask_bits);
	//			myassertEqual(dims.get(di), (int) (byte) ((msk >> (dim_mask_sz * 2 * di)) & dim_mask_bits));
	//		}
	//		myassertEqual(msk, msk & used_mask);
	//		myassert(msk >= 0);
	//		return msk;
	//	}
	//
	//	public UList<Integer> mask_dims(long msk) {
	//		final int ndimunits = this.ndimunits;
	//		printf("MDIM %s%n", msk);
	//		GrowList<Integer> lst = new GrowList<>();
	//		for (int di = 0; di < ndimunits; ++di)
	//			lst.add((int) (byte) ((msk >> (dim_mask_sz * 2 * di)) & dim_mask_bits));
	//		myassertEqual(dim_mask(lst.elems()), msk);
	//		return lst.elems();
	//	}

	public UMap<InVar, Double> invarMap(UList<Double> vls) {
		return invars.mkmapI((i, nm) -> vls.get(i));
	}

	public Input(String[] args, boolean readDataFromFile) throws YamlException, IOException {
		super(args);

		if (false) {
			printf("NOW%n");
			int x = -2;
			long msk = 0;
			msk |= (x & dim_mask_bits) << dim_mask_sz;
			dump(msk);
			System.out.println(Integer.toBinaryString(-2));
			System.out.println(Long.toBinaryString(msk));
			System.out.println(Long.toBinaryString((msk << (Long.BYTES - 2) * 8)));
			System.out.println(Long.toBinaryString(((msk << (Long.BYTES - 2) * 8) >> (Long.BYTES - 1) * 8)));
			System.out.println(Long.toBinaryString((byte) (msk >> dim_mask_sz)));
			dump((byte) (msk >> dim_mask_sz));
			dump(((msk << (Long.BYTES - 2) * 8) >> (Long.BYTES - 1) * 8));
			die();
		}
		printf("outputDir: %s%n", outputDir);

		ADD.complexity.set(addsub_complexity);
		MUL.complexity.set(mul_complexity);
		DIV.complexity.set(div_complexity);
		POW.complexity.set(pow_complexity);
		INTERNAL_EXP.complexity.set(exp_complexity);

		SIN.complexity.set(sin_complexity);
		COS.complexity.set(sin_complexity);
		TAN.complexity.set(sin_complexity);

		ADD1.complexity.set(addsub_complexity); // for now...

		Node0DoNotUse.M_complexity.set(M_complexity);

		UList<String> inlines = (!genDataFromExpr.isEmpty() ? genData()
				: readDataFromFile ? VRAFileUtils.readFile(infile).map(str -> str.trim())
						// ** causes yaml parsing problems
						: dataVars.isEmpty() ? dataLines : cons("** " + dataVars, dataLines))
								.map(str -> dropNonASCII(str))// .replace(",", " "))
		;

		//		if (!genDataFromExpr.isEmpty()) {
		//			printf("GENERATED DATA:%n");
		//			inlines.forEach(line -> println(line));
		//			println();
		//		}

		final String sep = infile.endsWith(".dat") ? " " : ",";
		{
			//		input = readInput(inputDir + "input_Sol-scaled.dat");
			//this.input = readInput(inputDir + infile);
			//printf("INP%n%s%n", inlines.mapEx(x -> x.getBytes("UTF-8")));
			//			printf("INP%n%s%n", inlines.mapEx(x -> dropNonASCII(x)));
			UList<UList<Double>> input0 = inlines
					.filter(str -> !str.startsWith("**") && !str.startsWith("#") && !str.trim().isEmpty())
					// first convert all whitespace chars to space, then replace sequences of more than one space to just one
					.map(str -> split1(str.replaceAll("#.*", "").replaceAll("\\s", " ").replaceAll("  *", " ")
			//.replaceAll("\\\\n", "")
							, sep).map(s -> toDouble(s, str)));

			this.numInVars = input0.get(0).size() - 1;
			this.inVarRng = rangeOpen(numInVars);

			input0.forEachI((i, inLine) -> {
				if (inLine.size() != numInVars + 1)
					die("Input entry %s has %s cols, should have %s: %s%n%s", i, inLine.size(), numInVars + 1, inLine,
							input0);
			});

			UList<UList<Double>> input1 = null;
			if (drop0inputs) {
				input0.forEachI((i, inLine) -> {
					UList<Double> inVars = inLine.subList(0, inLine.size() - 1);

					if (inVars.contains(0.0))
						printf(">>> LINE %s CONTAINS A ZERO VALUE - we are dropping this because the drop0inputs flag is on.%n",
								i);
				});
				input1 = input0.filter(inLine -> {
					UList<Double> inVars = inLine.subList(0, inLine.size() - 1);
					return !inVars.contains(0.0);
				});
			} else
				input1 = input0;

			this.unsampled_input_without_noise = input1.mapI((i, line) -> new DataLine(i, line.subList(0, numInVars),
					square_output_hack ? line.last() * line.last() : line.last()));

			if (!add_noise_to_input)
				this.unsampled_input = unsampled_input_without_noise;
			else {
				printf("Adding %s%% noise to input.%n", add_noise_to_input);
				Noise noise = new Noise(noise_seed, noise_mean,
						noise_epsilon * Noise.root_mean_square(unsampled_input_without_noise.map(line -> line.outval)));
				this.unsampled_input = unsampled_input_without_noise.map(line -> line.addNoise(noise));

			}
		}

		this.invarSeq = seqTo(numInVars);

		Optional<String> varline = inlines.findFirst(str -> str.startsWith("**"));
		UList<String> allvarnms = varline.isPresent()
				? split1(varline.get().replace("**", "").trim().replaceAll("\\s+", " "), sep)
				: invarSeq.map(i -> "X" + i).add("Y");
		myassert(allvarnms.size() == numInVars + 1, allvarnms);
		this.invarnms = allvarnms.subList(0, numInVars);
		this.invars = invarSeq.map(i -> new InVar(invarnms.get(i), i)); //  input.map(vs -> vs.invals.get(i))));
		this.outvarnm = allvarnms.get(numInVars);

		printf("Operators: %s%n", operators);
		if (exp_inds.isNotEmpty())
			printf("Exponent inds: %s%n", exp_inds);
		//		if (exp_range_low < exp_range_high)
		//			printf("Exponent range: %s - %s%n", exp_range_low, exp_range_high);

		if (operators.isEmpty())
			die("You must specify a list of operators: in your yaml file.");

		this.mlprod = NormNode.getNormML();
		this.mlleaves = mkUList1(mlprod);
		this.twoprods = mkUList1(mlprod, mlprod);

		unsampled_input.forEachI((i, line) -> printf("%2s: %s -> %s%n", i, line.invals, line.outval));

		if (dimensionVars0.isNotEmpty())
			myassertSubset(invarnms, dimensionVars0.keySet());

		UMap<String, InVar> invarMap = invars.mkrevmap(iv -> iv.nm);
		this.dimensionVars = dimensionVars0.filterByKeys(nm -> invarnms.contains(nm)).mapKeys(nm -> invarMap.get(nm));
		this.dimensionVarsByDimUnit = dimensionVars.mapValues((vr, dims) -> super.dimensionUnits.zipMap(dims));

		if (dimensionVars0.isNotEmpty()) {
			myassert(dimensionVars0.containsKey(outvarnm), "The dimensions for this var are not given: %s", outvarnm);
			this.outvarDimUnits = super.dimensionUnits.zipMap(dimensionVars0.get(outvarnm));
		} else
			this.outvarDimUnits = UMap.empty();

		this.dimensionUnits = super.dimensionUnits
				.filter(dim -> dimensionVarsByDimUnit.values().some(dimmap -> dimmap.get(dim) != 0));
		this.ndimunits = dimensionUnits.size();

		//		printf("DIMUS %s %s%n", ndimunits, dim_mask_sz);
		//myassert(ndimunits * dim_mask_sz <= 8 * Integer.BYTES);
		myassert(ndimunits * dim_mask_sz <= 8 * Long.BYTES);
		this.invar_dim_masks = new long[numInVars];
		//		if (use_dimensional_analysis) {
		//			for (int vi = 0; vi < numInVars; vi++)
		//				invar_dim_masks[vi] = dim_mask(dimensionVars0.get(invarnms.get(vi)));
		//
		//			this.outvar_dims = dim_mask(dimensionVars0.get(outvarnm));
		//		} else {
		//			this.outvar_dims = 0;
		//		}
		this.outvar_dims = 0;

		if (reuse_baron_files)
			printf("Re-using outputdir %s%n", outputDir);
		else {
			if (VRAFileUtils.fileExists(outputDir)) {
				printf("Renaming and recreating outputDir %s%n", outputDir);
				for (int i = 0; i < 1000; ++i)
					if (new File(outputDir).renameTo(new File(outputDir + i)))
						break;
			}
			//			printf("output exists? %s%n", VRAFileUtils.fileExists(outputDir));
			if (!VRAFileUtils.fileExists(outputDir))
				new File(outputDir).mkdirs();
		}

		dbg = new PrintStream(outputDir + "/bout.dbg");
		dbg.printf("dimunit: %s%nvarsByDU: %s%n", dimensionUnits, dimensionVarsByDimUnit);

		if (use_dimensional_analysis && dimensionUnits.isEmpty())
			printf("Warning:  can't do use_dimensional_analysis, because the vars used are dimensionless.%n");
		//		printf("DIMU %s%n", dimensionVarsByDimUnit);
		//		printf("DIMU %s%n", dimensionUnits);
		//		kludges();

		if (relaxUSE_I)
			myassert(numInVars == 1, "Can only have one input var using relax_USE_I");

		printOpts();
	}

	private void kludges() {
		double diff = 0;
		for (DataLine line : unsampled_input) {
			//** m1 m2 r1 r2 G      U
			double m1 = line.getx(0);
			double m2 = line.getx(1);
			double r1 = line.getx(2);
			double r2 = line.getx(3);
			double G = line.getx(4);
			double U = line.outval;
			//# Feynman I.13.12   G*m1*m2*(1/r2-1/r1)
			double U2 = G * m1 * m2 * (1 / r2 - 1 / r1);
			diff += (U - U2) * (U - U2);
		}
		die("DIFF %s%n", diff);
	}

	private UList<String> genData() {
		Random rand = new Random(genDataSeed);

		// doesn't have to be exact
		int nPs = 0;
		{
			String expr = genDataFromExpr;
			while (true) {
				String s = expr.replaceFirst("P", "X");
				if (s.equals(expr))
					break;
				expr = s;
				nPs++;
			}
		}
		myassert(genDataConsts.size() == genDataExps.size());
		genDataExps.assertEach(exps -> exps.size() == genDataVars.size());

		myassert(min_const_val == -max_const_val); // added min later

		UList<ProdVal> pvals = null;
		die();
		//		UList<ProdVal> pvals = rangeClosedMap(0, nPs - 1,
		//				i -> !genDataConsts.isEmpty()
		//						? new ProdVal(-1, genDataConsts.get(i), genDataExps.get(i).map(x -> (double) x), genDataVars)
		//						: new ProdVal(-1, max_var_exp, max_const_val, rand, genDataVars));

		ScriptEngineManager mgr = new ScriptEngineManager();
		ScriptEngine jsEngine = mgr.getEngineByName("JavaScript");

		{
			String s = genDataFromExpr;
			for (ProdVal pval : pvals)
				s = s.replaceFirst("P", pval.toString());
			printf("GENERATED EXPR: %s (%s)%n", s, genDataFromExpr);
		}

		final double max_invar_val = 10.0;
		//		StringBuilder sb = new StringBuilder();
		return rangeClosedMap(0, genDataNumLines, i -> {
			if (i == 0) {
				UList<String> dummynms = rangeClosed(1, genDataNumDummyVars).map(j -> "dummy" + j);
				return "** " + genDataVars.concat(dummynms).join(" ") + " y";
			}
			//		jsEngine.put("namesListKey", namesList);
			try {
				Object result = null;
				String s = genDataFromExpr;
				UList<Double> invals = genDataVars.map(v -> rand.nextDouble() * max_invar_val);
				int ii = 0;
				for (ProdVal pval : pvals)
					s = s.replaceFirst("P", "" + pval.eval(new DataLine(ii++, invals, 0.0)));
				result = jsEngine.eval(s);
				UList<Double> dummyvals = rangeClosed(1, genDataNumDummyVars)
						.map(v -> rand.nextDouble() * max_invar_val);
				return invals.concat(dummyvals).join(" ") + " " + result;
			} catch (ScriptException ex) {
				ex.printStackTrace();
				die();
				return null;
			}
		});
	}

	public double sumSquaresExpr(final String expr) {
		ScriptEngineManager mgr = new ScriptEngineManager();
		ScriptEngine jsEngine = mgr.getEngineByName("JavaScript");

		UList<String> suborder = sort(invarnms, (x, y) -> -chainCompare(x.length(), y.length()));
		//		printf("SUBORDER %s%n", suborder);
		UMap<String, Integer> varpos = invarnms.mkmapI((i, nm) -> i);

		final String noword = "____";
		String expr2 = expr.replaceAll("Math.pow", noword); // expr.replaceAll("^", "**"); 
		return sumd(unsampled_input.map(line -> {
			try {
				Object result = null;
				String s = expr2;
				UList<Double> invals = line.invals;
				for (String nm : suborder)
					s = s.replaceAll(nm, "" + invals.get(varpos.get(nm)));
				s = line.outval + " - " + s;
				s = s.replaceAll(noword, "Math.pow");
				//				printf("EVALLING %s%n", s);
				result = jsEngine.eval(s);
				//				printf("RES %s%n", result);
				if (result instanceof Integer)
					return (double) ((int) result * (int) result);
				if (result instanceof Double)
					return ((double) result * (double) result);
				else {
					die("BAD TYPE: %s%n", result);
					return 0.0;
				}
			} catch (ScriptException ex) {
				ex.printStackTrace();
				die("ERROR EVALLING EXPR");
				return null;
			}
		}));
	}

}
