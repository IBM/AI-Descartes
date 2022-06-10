// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package emf;

import static utils.UList.*;
import static utils.VRAFileUtils.*;
import static utils.VRAUtils.*;

import java.util.Arrays;

import utils.UList;

public class BaronFiles {
	public final String baronInputName;
	public final String outputName;
	public final String errorName;
	public final String resoutName;
	public final String timoutName;
	public final String baseName;

	public final String dir;

	public BaronFiles(String basename, String extension) {
		this.baronInputName = basename + extension;
		this.outputName = basename + ".out";
		this.resoutName = basename + ".res";
		this.errorName = basename + ".err";
		this.timoutName = basename + ".tim";
		this.dir = basename.replaceAll("/[^/]*\\z", "");
		this.baseName = basename;
		//		printf("DIR %s%n", dir);
	}

	public RunResult parseOutput(BaronJob job) {
		UList<String> output = readFile(outputName);
		UList<String> resout = readFile(resoutName);

		RunResult resx = RunResult.resultInfeasible(output) ? new RunResultInfeasible(job)
				: RunResult.someSolutionFound(resout) >= 0 ? new RunResultVal(job, output, resout, readFile(timoutName))
						: RunResult.heuristicCompletion(output) ? new RunResultLBound(job, output, readFile(timoutName))
								: new RunResultError(job, output);
		//		printf("DID %s%n", resx);
		return resx;
	}

	public static void main(String[] argsIn) throws Exception {
		String fname = argsIn[argsIn.length - 1];
		String ext = YamlInput.baron_extension;
		String basename = fname.endsWith(ext) ? fname.substring(0, fname.length() - ext.length()) : fname;

		String estr = basename.replaceAll(".*/b\\d*_", "").replaceAll("/.*", "").replaceAll("div", "/");

		String[] args = Arrays.copyOfRange(argsIn, 0, argsIn.length - 1);
		printf("ARGS: %s%n", mkUList(args));

		Main main = new Main(args);
		UList<NormNode> numberedNodes = main.genNodes();
		//Input inpx = new Input(args, true);
		Input inpx = main.inpx;
		Input2 inpx2 = new Input2(inpx, inpx.unsampled_input);

		myassert(inpx.inputSampleSize == 0);
		printf("Looking for expr %s%n", estr);
		NormNode node = numberedNodes.findFirst(x -> x.toString().equals(estr)).get();
		BaronJob job = new BaronJob(inpx2, node, 0);
		job.printBaronAux(System_out, 0); // nuts
		RunResult res0 = new BaronFiles(basename, ext).parseOutput(job);
		if (res0 instanceof RunResultVal) {
			RunResultVal res = (RunResultVal) res0;
			printf("%s objval by %10s: %10.2f%s %s %s%n", res.optimal ? "OPT" : "new", res.baronJob, res.val,
					res.baronJob.inpx2.input == res.baronJob.inpx.unsampled_input ? ""
							: String.format(" (%10.2f)", res.evalObjective(res.baronJob.inpx.unsampled_input)),
					res.baronJob.expr, res.resform);

		} else
			printf("Result did not produce a solution!%");
	}
}
