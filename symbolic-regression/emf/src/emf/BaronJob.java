// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package emf;

import static emf.Operator.*;
import static utils.UList.*;
import static utils.VRAFileUtils.*;
import static utils.VRAUtils.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Optional;

import utils.SingleAssignUList;
import utils.UList;
import utils.VRAFileUtils;
import utils.VRAUtils;
import utils.VRAUtils.NullOutputStream;

public class BaronJob implements Comparable<BaronJob> {
	public final Input inpx;
	public final Input2 inpx2;

	public final int index;
	public final UList<Split> splits;
	public final NormNode expr;
	public final INode nodeInstance;
	public final int complexity;

	public final BaronFiles bfiles;
	public final String baronInputName;
	public final String outputName;
	public final String errorName;
	public final String resoutName;
	public final String timoutName;

	public final int redo;

	public static boolean showOutputOnScreen = false;

	//	public final PrintProb printprob;

	public String toString() {
		//		return String.format("JOB(%s%s,%s)", index, splits.isEmpty() ? "" : "," + splits, expr);
		return expr.nodeNum.get() + ":" + expr;
	}

	private RunResult result;

	public RunResult result() {
		return result;
	}

	public boolean resultIsPresent() {
		return result != null && result.isPresent();
	}

	public PrintStream dbgout;

	public BaronJob rerun() {
		return new BaronJob(inpx2, expr, expr.mkinstance(inpx2.inpx), complexity, splits, redo + 1);
	}

	public void obj_variations() {
		for (int i = 1; i < inpx.obj_variations; ++i)
			new BaronJob(inpx2, expr, expr.mkinstance(inpx2.inpx), complexity, splits, i).printBaron(0.0);
	}

	public BaronJob(Input2 inpx2, NormNode expr, int complexity) {
		this(inpx2, expr, complexity, UList.empty());
	}

	public BaronJob(Input2 inpx2, NormNode expr, int complexity, UList<Split> splits) {
		this(inpx2, expr, expr.mkinstance(inpx2.inpx), complexity, splits, 0);
	}

	public BaronJob(Input2 inpx2, NormNode expr, INode nodeInstance, int complexity, UList<Split> splits, int redo) {
		super();
		this.inpx2 = inpx2;
		this.inpx = inpx2.inpx;
		this.index = expr.nodeNum.get();
		this.splits = splits;
		this.expr = expr;
		this.nodeInstance = nodeInstance; //expr.mkinstance(inpx);
		this.complexity = complexity;
		this.redo = redo;

		String bind0 = inpx.outputDir + "/" + (redo > 0 ? "redo" + redo + "/" : "");
		String bind = bind0 + "b" + index + "_";
		if (redo == 0 && !inpx.reuse_baron_files && splits.isEmpty()
				&& allFiles(bind0, "b" + index + "_*").isNotEmpty())
			printf("FILE NAME CONFLICT!  %s%n", inpx.outputDir + "/" + "b" + index + "_");
		String dir = bind + expr.toString().replaceAll("/", "div") + "/" + splits.join("");

		this.bfiles = new BaronFiles(dir + "/b" + index, inpx.extension);
		this.baronInputName = bfiles.baronInputName;
		this.outputName = bfiles.outputName;
		this.resoutName = bfiles.resoutName;
		this.errorName = bfiles.errorName;
		this.timoutName = bfiles.timoutName;

		if (!inpx.reuse_baron_files)
			if (!fileExists(bfiles.dir))
				myassert(new File(bfiles.dir).mkdirs());

		if (inpx.reuse_baron_files) {
			printedBaron = true;
			this.dbgout = devnull;

			printBaronAux(devnull, 0.0); // nuts

			printf("parsing %s%n", baronInputName);
			parseOutput();
			printf("done%n");
		}
		//String dbgnm = baronInputName + ".dbg";

		//		this.dbgout = dbgout;
	}

	@Override
	public int compareTo(BaronJob that) {
		//		return java.util.Comparator.comparing(BaronJob::index);

		// nuts
		if (chainCompare(this.splits.isNotEmpty(), that.splits.isNotEmpty()) == 0)
			return chainCompare(this.index, that.index, this.splits, that.splits);

		return chainCompare(this.splits.isNotEmpty(), that.splits.isNotEmpty());
	}

	private UList<BaronJob> subjobs;

	public UList<BaronJob> subjobs() {
		return subjobs == null ? UList.empty() : subjobs;
	}

	private BaronJob split(Split newsplit) {
		if (newsplit instanceof DropConstSplit)
			return new BaronJob(inpx2, expr, nodeInstance.dropConsts(mkUList1(newsplit.mlsplit)), complexity,
					splits.add(newsplit), redo);

		die(); // careful about nodeInstance
		return new BaronJob(inpx2, expr, nodeInstance, complexity, splits.add(newsplit), redo);
	}

	private <T extends Split> BaronJob split(UList<T> newsplits) {
		die();
		return new BaronJob(inpx2, expr, complexity, splits.concat(newsplits.map(x -> x)));
	}

	//	public UList<BaronJob> split_on_max_consts(int max_num_consts) {
	//		UList<MLInstance> cand_mlis = nodeInstance.dropConstsCands();
	//		if (cand_mlis.size() < max_num_consts)
	//			return mkUList1(this);
	//		if (splits.filterSubType(DropConstSplit.class).isNotEmpty()) {
	//			die("CAN'T DROPCONST SPLIT!  Already did it%n");
	//			//			return UList.empty();
	//		}
	//		//		printf("SPLITTING %s %s %s %s%n", this, nodeInstance.mlInstances(), existing_splits, mlinsts);
	//		myassert(subjobs == null);
	//		subjobs = cand_mlis.allSubsets(max_num_consts).map(x -> split(new DropConstSplit(x)));
	//		return subjobs;
	//	}

	//	public UList<BaronJob> split_on_drop_const(int cnst_thresh) {
	//		UList<MLInstance> mlinsts = nodeInstance.mlInstances();
	//		if (mlinsts.size() < cnst_thresh)
	//			return mkUList1(this);
	//		if (splits.filterSubType(DropConstSplit.class).isNotEmpty()) {
	//			die("CAN'T DROPCONST SPLIT!  Already did it%n");
	//			//			return UList.empty();
	//		}
	//		//		printf("SPLITTING %s %s %s %s%n", this, nodeInstance.mlInstances(), existing_splits, mlinsts);
	//		myassert(subjobs == null);
	//		subjobs = mlinsts.map(x -> split(new DropConstSplit(x)));
	//		return subjobs;
	//	}

	public UList<BaronJob> split_on_consts() {
		UList<MLInstance> existing_splits = splits.filterSubType(ConstSplit.class).map(sp -> sp.mlsplit);
		UList<MLInstance> mlinsts = nodeInstance.mlInstances().diff(existing_splits);
		if (mlinsts.isEmpty()) {
			printf("CAN'T SPLIT ON %s!  no more consts (%s)%n", this, this.baronInputName);
			return UList.empty();
		}
		printf("SPLITTING %s %s %s %s%n", this, nodeInstance.mlInstances(), existing_splits, mlinsts);
		myassert(subjobs == null);
		subjobs = trueFalse.map(bool -> split(new ConstSplit(mlinsts.first(), bool)));
		return subjobs;
	}

	public UList<BaronJob> split_on_all_consts() {
		myassert(splits.isEmpty());
		//		UList<MLInstance> existing_splits = splits.filterSubType(ConstSplit.class).map(sp -> sp.mlsplit);
		//UList<MLInstance> mlinsts = nodeInstance.mlInstances().diff(existing_splits);
		UList<MLInstance> mlinsts = nodeInstance.mlInstances();
		printf("SPLITTING %s %s %s%n", this, nodeInstance.mlInstances(), mlinsts);
		myassert(subjobs == null);

		UList<UList<Split>> newsplits = mlinsts.map(sp -> trueFalse.map(bl -> new ConstSplit(sp, bl)));

		subjobs = UList.allCombs(newsplits).map(sps -> split(sps));
		return subjobs;
	}

	public UList<BaronJob> split_on_all_exps() {
		myassert(splits.isEmpty());
		//		UList<MLInstance> existing_splits = splits.filterSubType(ConstSplit.class).map(sp -> sp.mlsplit);
		//UList<MLInstance> mlinsts = nodeInstance.mlInstances().diff(existing_splits);
		UList<MLInstance> mlinsts = inpx.split_on_denom_exp && expr.op == DIV ? nodeInstance.righti().mlInstances()
				: nodeInstance.mlInstances();
		printf("SPLITTING ALL EXPS %s %s %s%n", this, nodeInstance.mlInstances(), mlinsts);
		myassert(subjobs == null);
		myassert(!inpx.using_expn_op);
		UList<UList<Split>> newsplits = null;

		die();
		//				mlinsts.flatMap(				sp -> sp.varexps.map(var -> rangeClosed(0, inpx.max_var_exp).map(i -> new IntExpSplit(var, i))));

		subjobs = UList.allCombs(newsplits).map(sps -> split(sps));
		return subjobs;
	}

	public UList<BaronJob> split_on_exp0() {
		myassert(splits.isEmpty());
		//		UList<MLInstance> existing_splits = splits.filterSubType(ConstSplit.class).map(sp -> sp.mlsplit);
		//UList<MLInstance> mlinsts = nodeInstance.mlInstances().diff(existing_splits);
		UList<MLInstance> mlinsts = //inpx.split_on_denom_exp && expr.op == DIV ? nodeInstance.righti().mlInstances() :
				nodeInstance.mlInstances().subList(0, 1);
		printf("SPLITTING EXP0 %s %s %s%n", this, nodeInstance.mlInstances(), mlinsts);
		myassert(subjobs == null);
		myassert(!inpx.using_expn_op);
		UList<UList<Split>> newsplits = null;

		die();
		//		mlinsts				.map(sp -> rangeClosed(0, inpx.max_var_exp).map(i -> new IntExpSplit(sp.varexps.get(0), i)));

		subjobs = UList.allCombs(newsplits).map(sps -> split(sps));
		return subjobs;
	}

	public UList<BaronJob> split_on_small() {
		myassert(splits.isEmpty());
		if (nodeInstance.op != DIV)
			return mkUList1(this);

		UList<MLInstance> mlinstsl = nodeInstance.lefti().mlInstances();
		UList<MLInstance> mlinstsr = nodeInstance.righti().mlInstances();
		UList<MLInstance> mlinsts = mlinstsl.size() < mlinstsr.size() ? mlinstsl : mlinstsr;

		//		printf("SPLITTING SMALL %s %s %s%n", this, nodeInstance.mlInstances(), mlinsts);
		myassert(subjobs == null);
		myassert(!inpx.using_expn_op);
		UList<UList<Split>> newsplits = null;

		die();
		//		mlinsts.flatMap(sp -> sp.varexps)		.map(vexp -> rangeClosed(vexp.lbound, vexp.ubound).map(i -> new IntExpSplit(vexp, i)));

		subjobs = UList.allCombs(newsplits).map(sps -> split(sps));
		return subjobs;
	}

	private boolean printedBaron;

	public void printBaron(double ubound_squared) {
		if (printedBaron)
			return;

		if (inpx.trace_baron || inpx.trace_baron_tree == index)
			printBaron(System.out, ubound_squared);

		if (inpx.reuse_baron_files && fileExists(baronInputName)) {
			UList<String> blines = readFile(baronInputName);
			if (blines.last().startsWith("OBJ:"))
				return;
		}

		if (inpx.print_baron || inpx.trace_baron_tree == index) {
			PrintStream out = VRAFileUtils.newPrintStream(baronInputName);
			printBaron(out, ubound_squared);
			out.close();
		}
		printedBaron = true;
	}

	public final SingleAssignUList<MLInstance> multiLeavesNotFixedWithConst = new SingleAssignUList<>();

	// nuts
	public PrintProb printBaronAux(PrintStream out, double ubound_squared) {
		PrintProb pp = new PrintProb(inpx2, expr, nodeInstance, splits, redo, out);
		multiLeavesNotFixedWithConst.init(pp.multiLeavesNotFixedWithConst);
		return pp;
	}

	private void printBaron(PrintStream out, double ubound_squared) {
		PrintProb pp = printBaronAux(out, ubound_squared);

		pp.print(resoutName, timoutName, ubound_squared);
	}

	//https://wiki.sei.cmu.edu/confluence/display/java/FIO07-J.+Do+not+let+external+processes+block+on+IO+buffers
	// how silly
	static class StreamGobbler implements Runnable {
		private final InputStream is;
		private final PrintStream os;

		private StringBuilder output = new StringBuilder();

		public UList<String> lines() {
			return splitOnNewlines(output.toString());
		}

		StreamGobbler(InputStream is, PrintStream os) {
			this.is = is;
			this.os = os;
		}

		public void run() {
			try {
				int c;
				while ((c = is.read()) != -1) {
					os.print((char) c);
					output.append((char) c);
				}
			} catch (IOException x) {
				// Handle error
			}
		}
	}

	private Process proc;
	private long pid;
	private BufferedReader reader;
	private Thread errorGobbler, outputGobbler;
	private StreamGobbler outputGobbler1;
	private long startTime;
	private long last_updateTime;
	private long doneTime;
	private long runTime;
	private boolean suspended;
	private boolean killed = false;
	private boolean hopeless = false;
	private long hopeless_killtime;
	private int num_times_killed = 0;

	public long startTime() {
		return startTime;
	}

	public long doneTime() {
		return doneTime;
	}

	private void update_runTime() {
		if (!suspended && !killed) {
			long tm = System.currentTimeMillis();
			runTime += tm - last_updateTime;
			last_updateTime = tm;
		}
	}

	public long runTime() {
		update_runTime();
		return runTime;
	}

	public boolean started() {
		return startTime > 0;
	}

	public void start(double ubound_squared) throws IOException {
		if (inpx.reuse_baron_files) {
			parseOutput();
			return;
		}

		if (startTime == 0) {
			startTime = System.currentTimeMillis();
			last_updateTime = startTime;
			myassert(startTime > 0);

			if (inpx.reuse_baron_output && parseOutput())
				return;

			String dbgnm = bfiles.baseName + ".dbg";
			try {
				this.dbgout = new PrintStream(dbgnm);
			} catch (FileNotFoundException e) {
				printf("ERROR CREATING DBG STREAM %s - just ignoring.%n", dbgnm);
				this.dbgout = new PrintStream(new NullOutputStream());
			}

			printBaron(ubound_squared);
			if (showOutputOnScreen)
				readFile(baronInputName).forEach(s -> println(s));

			//			printf("STARTING %s%n", baronInputName);
			//			if (splits.isNotEmpty()) {
			//				printf("SP %s%n", splits);
			//				die();
			//			}

			ProcessBuilder pb = new ProcessBuilder(inpx.baron_exec, baronInputName);
			//			pb.inheritIO();
			//			pb.redirectError(new File(errorName));
			//			pb.redirectOutput(new File(outputName));
			this.proc = pb.start();
			this.pid = getPid(proc);

			// Any error message?
			this.errorGobbler = new Thread(new StreamGobbler(proc.getErrorStream(),
					showOutputOnScreen ? System_out : new PrintStream(errorName)));

			// Any output?
			this.outputGobbler1 = new StreamGobbler(proc.getInputStream(),
					showOutputOnScreen ? System_out : new PrintStream(outputName));

			this.outputGobbler = new Thread(outputGobbler1);

			errorGobbler.start();
			outputGobbler.start();
		}
	}

	public void suspend() {
		if (!suspended) {
			//			printf("susp %s%n", index);
			suspend1("STOP");
			update_runTime();
			suspended = true;
			last_updateTime = 1000000000;
		}
		//		printf("suspended %s%n", pid);
	}

	public void resume() {
		if (suspended) {
			//			printf("resume %s%n", index);
			suspend1("CONT");
			last_updateTime = System.currentTimeMillis();
			suspended = false;
			//		printf("resumed %s%n", pid);
		}
	}

	private void suspend1(String cmdarg) {
		if (inpx.reuse_baron_files)
			return;

		myassert(pid >= 0);
		//		System.system("kill -STOP " + pid);
		int retval = -1;
		try {
			String[] args = { "/bin/kill", "-" + cmdarg, "" + pid }; // "-C", dirname(path), "-czf", path + ".tgz", basename(path) };
			Process p = Runtime.getRuntime().exec(args);
			retval = p.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// don't know what the exit status is, man page doesn't say.
	}

	//	boolean isRunning() {
	//		return proc != null && !isDone();
	//	}

	boolean failed() {
		return !hopeless && startTime > 0 && proc == null && !result.isPresent();
	}

	public boolean hopeless() {
		return hopeless;
	}

	private int lastNsols = 0;
	private int killTimeMult = 1;

	public Optional<RunResultVal> stopJobIfHopeless(UList<RunResultVal> bestres) {
		if (true)
			// BUG - see below
			return Optional.empty();

		if (inpx.reuse_baron_files)
			return Optional.empty();

		update_runTime();

		if (killed)
			return Optional.empty();

		//		 16559          1731            16.21     4.54986          20.0681   
		UList<String> lines = outputGobbler1.lines();
		if (lines.isEmpty())
			return Optional.empty();
		UList<String> cols = VRAUtils.split1(lines.last().replaceAll("^\\*", "").trim(), " +"); // .map(s->s.tr);
		//		if (expr.nodeNum.get() == 6)
		//			printf("STOP? %s %s%n", cols.size(), cols);

		if (cols.size() != 5)
			return Optional.empty();

		if (!cols.all(s -> isDouble(s)))
			return Optional.empty();

		double lbnd = toDouble(cols.get(3));
		Optional<RunResultVal> bres = bestres.findFirst(res -> res.baronJob.expr.complexity <= expr.complexity
				&& (res.val <= lbnd || res.val <= inpx.hopelessJobThresh));
		//		if (expr.nodeNum.get() == 6)
		//			bestres.forEach(res -> printf("STOP? %s %s %s %s%n", res.baronJob.expr.complexity, expr.complexity,
		//					res.val.get(), lbnd));
		if (bres.isPresent()) {
			/*
			 * WRONG - lbnd for one kind of problem is killing others that aren't subject to the lbound.
			 * 
			Printing all jobs upfront - ubound is 0.0
			   1| OPT objval by        0:P:     11.904 P (1.3250911739318374*omega_0)
			   2| OPT objval by    1:(P+P):      1.063 (P+P) ((4.399370943483093*c^-2*v^2*omega_0)+(omega_0))
			   2| OPT objval by 3:pow((P+P)):      1.539 pow((P+P)) (((7.945870673977302*c^-1*v*omega_0^2)))^0.5
			   REDUNDANT pow((P)) (((7.945870673977302*c^-1*v*omega_0^2)))^0.5
			   3| OPT objval by  2:(P+P+P):      0.201 (P+P+P) ((1.8234903683798065*c^-2*v^2*omega_0)+(omega_0)+(c^-1*v*omega_0))
			   3| OPT objval by 6:pow((P+P+P)):      1.539 pow((P+P+P)) (((7.94578057037207*c^-1*v*omega_0^2)))^0.5
			   REDUNDANT pow((P)) (((7.94578057037207*c^-1*v*omega_0^2)))^0.5
			(2:(P+P+P) is very good and less complex, so killing jobs [4:(P/(P+P)), 7:(P+pow((P+P)))])
			(2:(P+P+P) is very good and less complex, so not running [10:(P/(P+P+P)), 11:(P/pow((P+P))), 12:(P+P+P+P+P), 13:pow((P/(P+P))), 14:pow((P+P+P+P)), 15:((P+P)/(P+P)), 16:(P+(P/(P+P))), 17:(P+P+pow((P+P))), 18:(pow((P+P))*(P+P)), 19:(P/(P+P+P+P)), 20:(P+P+P+P+P+P), 21:((P+P+P)/(P+P)), 22:(pow((P+P))/(P+P)), 23:((P+P)/(P+P+P)), 24:((P+P)/pow((P+P))), 25:(P+P+(P/(P+P))), 26:(P+P+P+pow((P+P))), 27:pow(((P+P)/(P+P))), 28:(P+((P+P)/(P+P))), 29:(pow((P+P))+pow((P+P))), 30:(pow((P+P))*(P+P+P)), 31:(P+P+P+P+P+P+P), 32:((P+P+P)/(P+P+P)), 33:((P+P+P)/pow((P+P))), 34:((P+P+P+P)/(P+P)), 35:(pow((P+P))/(P+P+P)), 36:((P+P)/(P+P+P+P)), 37:(P+P+P+(P/(P+P))), 38:(P+P+P+P+pow((P+P))), 39:(P+P+((P+P)/(P+P))), 40:(pow((P+P))+(P/(P+P))), 41:((P*pow((P+P)))/(P+P)), 42:(pow((P+P))*(P+P+P+P)), 43:(P/(pow((P+P))*(P+P))), 44:((pow((P+P))*(P+P))/P), 45:(P+P+P+P+P+P+P+P), 46:((P+P+P)/(P+P+P+P)), 47:((P+P+P+P)/(P+P+P)), 48:((P+P+P+P)/pow((P+P))), 49:(pow((P+P))/(P+P+P+P)), 50:(P+P+P+P+(P/(P+P))), 51:(P+P+P+((P+P)/(P+P))), 52:((P/(P+P))+(P/(P+P))), 53:(pow((P+P))+((P+P)/(P+P))), 54:((pow((P+P))*(P+P))/(P+P)), 55:((P+P)/(pow((P+P))*(P+P))), 56:((P+P+P+P)/(P+P+P+P)), 57:(P+P+P+P+((P+P)/(P+P))), 58:((P/(P+P))+((P+P)/(P+P))), 59:(((P+P)/(P+P))+((P+P)/(P+P)))])
			(2:(P+P+P) is very good and less complex, so killing jobs [8:(P*pow((P+P))), 9:(pow((P+P))/P)])
			(2:(P+P+P) is very good and less complex, so killing jobs [5:(P+P+P+P)])
			still running: []
			All jobs finished.
			0 jobs failed.
			
			------
			OPT CAND   2:     0.2008                            (P+P+P) S ((1.8234903683798065*c^-2*v^2*omega_0)+(omega_0)+(c^-1*v*omega_0))
			OPT CAND   1:     1.0634                              (P+P) S ((4.399370943483093*c^-2*v^2*omega_0)+(omega_0))
			OPT CAND   3: REDUNDANT (    1.54) pow(P) ((7.945870673977302*c^-1*v*omega_0^2))^0.5
			OPT CAND   6: REDUNDANT (    1.54) pow(P) ((7.94578057037207*c^-1*v*omega_0^2))^0.5
			OPT CAND   0:    11.9041                                  P S (1.3250911739318374*omega_0)
			*/
			kill();
			return bres;
		}
		return Optional.empty();
	}

	public void kill() {
		proc.destroy();
		killed = true;
		num_times_killed++;
		hopeless = true;
		update_runTime();
		hopeless_killtime = System.currentTimeMillis();
		this.doneTime = System.currentTimeMillis();
		result = new RunResultError(this);
	}

	boolean isDone() {
		if (inpx.reuse_baron_files)
			return true;

		update_runTime();

		if (result != null)
			return true;

		if (suspended)
			return false;

		if (proc.isAlive()) {
			//			printf("alive %s%n", index);
			double currTime = System.currentTimeMillis();

			// sometimes killing once doesn't seem to work
			boolean rekill = (hopeless && hopeless_killtime + 1 * 1000 <= currTime);
			if (!killed || rekill) {
				if (rekill) {
					if (num_times_killed > 2) {
						printf("CAN'T KILL JOB %s!  just giving up.%n", index);
						killed = true;
						proc = null;
						result = new RunResultError(this);
						return true;
					}

					printf("(killing %s a second time)%n", index);
					hopeless_killtime = System.currentTimeMillis();
					num_times_killed++;
				}

				if (inpx.killTime > 0 && currTime - startTime >= 1000 * (inpx.killTime * killTimeMult + 1)) {
					UList<String> lines = readFile(resoutName);
					int nsols = lines.filter(s -> s.startsWith("*")).size();
					if (nsols > lastNsols) {
						// hacky
						printf("NOT killing baron process %s, even though taking a long time, because it is making progress.%n",
								index);
						lastNsols = nsols;
						killTimeMult++;
					}

					// Sometimes Baron seems to ignore the time limit
					printf("KILLING RUNAWAY BARON PROCESS %s (last line: %s)%n", index, lines.last());
					proc.destroy();
					update_runTime();
					killed = true;
				}
			}

			return false;
		}

		try {
			errorGobbler.join();
			// Handle condition where the
			outputGobbler.join(); // process ends before the threads finish
		} catch (InterruptedException e) {
			die();
		}

		this.doneTime = System.currentTimeMillis();
		// proc.exitValue() probably not helpful
		//		int exitCode = proc.waitFor(100);
		if (killed)
			result = new RunResultError(this);
		else if (result == null)
			parseOutput();
		proc = null;
		return true;
	}

	private boolean syntax_error = false;

	public boolean syntax_error() {
		return syntax_error;
	}

	private boolean parseOutput() {

		dbgout.printf("Parsing %s %s%n", index, expr);

		//		kill $(ps x|grep baron|sed 's/pts.*//')
		{
			UList<String> output = readFile(errorName);
			if (output.isNotEmpty()) {
				if (output.first().startsWith("warning:")) {
					System_err.flush();
					System_out.flush();
					printf(">>>> BARRON WARNING in %s (%s):%n", index, baronInputName);
					output.forEach(line -> printf("%s%n", line));
					printf(">>>> Hopefully, these are just warnings and not errors - we will ignore them, but they may cause the program to fail.%n");
				} else {
					System_err.flush();
					System_out.flush();
					if (output
							.some(x -> x.contains("*** A potentially catastrophic access violation just took place"))) {
						printf("(baron process %s died, restarting)%n", index);
					} else {
						printf("BARRON ERRORS in %s (%s):%n", index, baronInputName);
						output.forEach(line -> printf("%s%n", line));
					}
					result = new RunResultError(this);
					syntax_error = output.some(x -> x.contains("Syntax"));
					return true;
				}
			}
		}

		myassert(this.result == null);
		this.result = bfiles.parseOutput(this);

		if (!inpx.reuse_baron_files)
			dbgout.close(); // whatever this is, caused problems

		return true;
	}

}
