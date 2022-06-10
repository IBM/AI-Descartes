// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package emf;

import static utils.VRAUtils.*;

import java.io.IOException;

import utils.GrowList;
import utils.UList;

public class JobPool {
	public final UList<BaronJob> jobs_by_id;
	public final Input inpx;
	public final Input2 inpx2;

	private final long starttm = System.currentTimeMillis();

	public JobPool(Input2 inpx2, UList<NormNode> ns) {
		this(inpx2, ns.map(n -> new BaronJob(inpx2, n //.norm.arithNorm()//.maybeDropConsts(inpx.max_num_consts)
				, n.depth)), false);
	}

	private JobPool(Input2 inpx2, UList<BaronJob> jobs_by_id, boolean ignore) {
		this.inpx2 = inpx2;
		this.inpx = inpx2.inpx;

		this.jobs_by_id = jobs_by_id;

		//		jobs_by_id.map(n -> n.expr.nodeNum.get()).checkDistinct();

		if (inpx.trace_baron_tree >= 0) {
			jobs_by_id.get(inpx.trace_baron_tree).printBaron(inpx.infinity);
			die();
		}
		//		else if (!inpx.reuse_baron_files)
		//			jobs.forEach(j -> j.printBaron());

	}

	//	public JobPool splitOnConsts() {
	//		UList<BaronJob> jobs_left = jobs_by_id.filter(j -> !j.resultIsPresent() || !j.result().get().optimal);
	//		return new JobPool(inpx2, jobs_left.flatMap(job -> job.split_on_consts()), false);
	//	}
	//
	//	public JobPool splitOnDropConst(int cnst_thresh) {
	//		UList<BaronJob> jobs_left = jobs_by_id.filter(j -> !j.resultIsPresent() || !j.result().get().optimal);
	//		return new JobPool(inpx2, jobs_left.flatMap(job -> job.split_on_drop_const(cnst_thresh)), false);
	//	}

	private Jobs jobs; // for shutdown hood

	String timeString() {
		final long tm = System.currentTimeMillis();
		long secs = (tm - starttm) / 1000;
		if (secs <= 60)
			return "" + secs;
		long mins = secs / 60;
		secs -= mins * 60;
		return mins + ":" + secs;

	}

	public UList<RunResult> run(boolean splitOnSmall) throws InterruptedException, IOException {
		if (jobs_by_id.isEmpty()) {
			printf("(no jobs to run)%n");
			return UList.empty();
		}

		myassert(!splitOnSmall);
		jobs = new Jobs(inpx2, // inpx.max_num_consts >= 0 ? jobs_by_id.flatMap(j -> j.split_on_consts()):
				!splitOnSmall ? jobs_by_id : jobs_by_id.flatMap(j -> j.split_on_small()));

		//		UMap<Integer, RunResult> best_results = UMap.empty();
		long lastReportTime = System.currentTimeMillis();
		long startTime = lastReportTime;

		UList<String> last_summary = UList.empty();
		long sleepTime = 1;
		int njobs_finished = 0;
		int foox = 0;
		long lastjob_finish_time = 0;
		UList<RunResultVal> bestres = UList.empty();
		while (!jobs.allDone()) {
			//			dbgout.printf("Running: %s%n", running_jobs.map(j -> j.index));
			boolean noChange = true;

			final long tm = System.currentTimeMillis();
			if (tm - startTime > inpx.total_maxsecs * 1000) {
				printf("STOPPING, since exceeded total_maxsecs (%s).%n", inpx.total_maxsecs);
				//								printf("%s %s%n", (tm - startTime) * 1000, inpx.total_maxsecs);
				jobs.running_jobs().forEach(x -> x.kill());
				return jobs.finished_results();
			}

			//			printf("LAST %s %s %s%n", tm, lastReportTime, inpx.reportIntervalSecs * 1000);
			if (tm < lastReportTime + inpx.reportIntervalSecs * 1000) {
				long secs = (tm - lastReportTime) / 1000;
				lastReportTime = tm;
				//				final UMap<Integer, Integer> compLeft = jobs_to_rerun_by_complexity.mapValues(lst -> lst.size());
				// THIS DIDN'T WORK FOR SANJEEB - figure out later
				//				final UList<String> summary = best_results.sortByKey()
				//				final UList<String> summary = best_results.sortByKeyField(x -> x)
				//						.bimap((comp, res) -> String.format("%2s: %4s %10.4f %3s %5s %20s %20s", comp,
				//								res.baronJob.index, res.val.get(), res.optimal ? "OPT" : "",
				//								compLeft.getOrElse(comp, 0), res.resform, res.baronJob.expr));
				//				if (!summary.equals(last_summary)) {
				//					printf("njobs_finished: %3s   secs since last summary: %s   secs since start: %s%n", njobs_finished,
				//							secs, (tm - startTime) / 1000);
				//					summary.forEach(s -> println(s));
				//					println();
				//					last_summary = summary;
				//					njobs_finished = 0;
				//				}
			}

			jobs.startMoreJobs(bestres);
			UList<RunResult> newres0 = jobs.checkDone();
			UList<RunResultVal> newres = newres0.filterSubType(RunResultVal.class);
			UList<RunResultLBound> newreslb = newres0.filterSubType(RunResultLBound.class);
			njobs_finished += newres0.size();
			GrowList<NormNode> redundantForms = new GrowList<>();
			//			if (false)

			newres.forEach(res -> {
				printf("%4s| %s objval by %10s: %10.5f%s %s %s%n", timeString(), res.optimal ? "OPT" : "new",
						res.baronJob, res.val,
						res.baronJob.inpx2.input == res.baronJob.inpx.unsampled_input ? ""
								: String.format(" (%10.2f)", res.evalObjective(res.baronJob.inpx.unsampled_input)),
						res.baronJob.expr, res.resform);
				ResForm simpform = res.resform;
				RunResultVal r = res;
				if (simpform == null || simpform.form() != r.raw_resform.form()) {
					printf("   %s %s %s%n", r.optimal ? "REDUNDANT" : "redundant",
							simpform == null ? "?" : simpform.form(), simpform);
					if (simpform != null)
						redundantForms.add(simpform.form());
				}
			});
			newreslb.forEach(res -> {
				if (res.lbound > 1e-2)
					printf("new lbound by %10s: %10.2f %s%n", res.baronJob, res.lbound, res.baronJob.expr);
			});
			bestres = updateBestRes(bestres, newres);
			//			newres.forEach(res -> {
			//				BaronJob job = res.baronJob;
			//				UList<BaronJob> allrelated = jobs.relatedJobs(job);
			//				UList<BaronJob> related = jobs.relatedFinishedJobs(job).filter(jb -> jb.subjobs().isEmpty());
			//				UList<BaronJob> haveres = related.filter(j -> j.resultIsPresent());
			//				UList<BaronJob> haveopt = related.filter(j -> j.resultIsPresent() && j.result().get().val.isPresent());
			//				UList<BaronJob> havelb = related.filter(j -> j.resultIsPresent() && j.result().get().lbound.isPresent())
			//						.diff(haveopt);
			//				//				printf("JSTAT %s: %s %s %s %s%n", res.baronJob.index, res.baronJob.expr,
			//				//						allrelated.size() - related.size(), haveres.size(), havelb.size());
			//				//				(allrelated.diff(related))
			//				if (false)
			//					related.forEach(j -> printf("rel %s %s %s %s %s %s%n", j.index, j.expr, j.splits, // j.resultIsPresent(),
			//							!j.resultIsPresent() ? null : j.result().get().optimal,
			//							!j.resultIsPresent() ? null : j.result().get().val,
			//							!j.resultIsPresent() ? null : j.result().get().lbound));
			//
			//				if (related.all(j -> j.resultIsPresent() && j.result().get().optimal)) {
			//					UList<BaronJob> havevals = related.filter(j -> j.result().get().val.isPresent());
			//					UList<BaronJob> havelbs = related.filter(j -> j.result().get().lbound.isPresent());
			//					//					printf("OPT FOR %s: %s %s (%s)%n", job.expr,
			//					//							mindOrElse(havevals.map(j -> j.result().get().val.get()), -100),
			//					//							maxdOrElse(havelbs.map(j -> j.result().get().val.get()), -100), related.diff(havevals));
			//				}
			//			});

			if (false)
				jobs.addJobs(newres.filter(res -> !res.optimal //&& !res.lbound.isPresent()
				).flatMap(res -> res.baronJob.split_on_consts()));
			else if (inpx.split_on_all_consts)
				jobs.addJobs(newres.filter(res -> !res.optimal).map(r -> r.baronJob).filter(j -> j.splits.isEmpty())
						.flatMap(j -> j.split_on_all_consts()));
			else if (inpx.split_on_all_exps || inpx.split_on_denom_exp)
				jobs.addJobs(newres.filter(res -> !res.optimal).map(r -> r.baronJob).filter(j -> j.splits.isEmpty())
						.flatMap(j -> j.split_on_all_exps()));
			else if (inpx.split_on_exp0)
				jobs.addJobs(newres.filter(res -> !res.optimal).map(r -> r.baronJob).filter(j -> j.splits.isEmpty())
						.flatMap(j -> j.split_on_exp0()));

			for (NormNode rnode : redundantForms.elems())
				for (BaronJob j : jobs.running_jobs())
					if (rnode == j.expr) {
						printf("(killing job %s since it is redundant)%n", j);
						j.kill();
					}

			if (false)
				if (foox + 20 < njobs_finished) {
					foox = njobs_finished;
					jobs.running_jobs().forEach(j -> printf("x %s %s %s%n", j.index, j.expr, j.splits));
					jobs.jobsLeft().forEach(j -> printf("y %s %s %s%n", j.index, j.expr, j.splits));
				}
			if (inpx.stop_hopeless_jobs)
				jobs.stopJobsIfHopeless(bestres);

			if (newres.isNotEmpty())
				lastjob_finish_time = tm;

			if (inpx.stop_on_good_fit && newres.some(x -> x.val <= inpx.stop_on_good_fit_thresh)) {
				printf("STOPPING, since found a good solution.%n");
				jobs.running_jobs().forEach(x -> x.kill());
				return jobs.finished_results();
			}

			if (tm - lastjob_finish_time >= 10 * 1000) {
				lastjob_finish_time = tm; // fudge
				printf("still running: %s%n", jobs.running_jobs().map(x -> x.expr.nodeNum.get()));
			}
			//	
			//
			//			if (noChange)
			//				sleepTime *= 2;
			//			else
			//				sleepTime = 1;
			Thread.sleep(sleepTime * 1000);
		}

		printf("All jobs finished.%n");
		// failed jobs are rerun
		printf("%s jobs failed.%n", jobs.failed_jobs().size());

		myassert(jobs.running_jobs().isEmpty());

		return jobs.finished_results();
		//		printf("%s jobs to rerun. %s %s%n", sum(jobs_to_rerun_by_complexity.values().map(l -> l.size())),
		//				jobs_to_rerun_by_complexity.values().map(l -> l.size()), jobs_to_rerun_by_complexity);

		//		for (int comp = 1; comp < maxComplexity; comp++)
		//			if (best_results.containsKey(comp)) {
		//				RunResult res = best_results.get(comp);
		//				printf("best for complexity %s by %s: %s %s%n", comp, res.baronJob.index, res.val.get(),
		//						res.baronJob.expr);
		//			}
	}

	public UList<RunResult> reuse_baron_files() {
		UList<RunResult> res = jobs_by_id.map(j -> j.result());
		printf("REUSING: %s %s%n", res.count(r -> r.isPresent()), res.count(r -> r instanceof RunResultVal));
		return res;
	}

	private static UList<RunResultVal> updateBestRes(UList<RunResultVal> bestres, UList<RunResultVal> newres) {
		UList<Integer> comp = newres.map(x -> x.baronJob.expr.complexity);
		return bestres.filter(x -> !comp.contains(x.baronJob.expr.complexity)).concat(newres)
				.sort((x, y) -> chainCompare(x.baronJob.expr.complexity, y.baronJob.expr.complexity));
	}

	public void print_timing_info() {
		printf("%4s %2s  %6s %6s  %s%n", "", "", "Start", "Duration", "Complexity");
		//		for (NormNode n : numberedNodes) {
		//			Optional<RunResult> vl = res.findFirst(x -> x.baronJob.expr == n);
		//			if (vl.isPresent()) {
		//				BaronJob j = vl.get().baronJob;
		//				//				printf("TIME %2s: %6.1f %6.1f    %s%n", j.expr.nodeNum.get(), (j.startTime() - starttm) / 1000,
		//				//						(j.doneTime() - j.startTime()) / 1000, j.expr.complexity);
		//			}
		//		}
		//		for (BaronJob j : jobs_by_id) {
		//			if (j.resultIsPresent()) {
		//				//				printf("TIME %2s: %6.1f %6.1f    %s%n", j.expr.nodeNum.get(), (j.startTime() - starttm) / 1000,
		//				//						(j.doneTime() - j.startTime()) / 1000, j.expr.complexity);
		//			}
		//		}
		jobs_by_id.forEach(j -> {
			printf("TIME %2s: %6.1f %6.1f    %s%n", j.expr.nodeNum.get(), (j.startTime() - starttm) / 1000,
					(j.doneTime() - j.startTime()) / 1000, j.expr.complexity);
		});
	}

	public void addShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				if (jobs != null) {
					printf("SHUTTING DOWN!  KILLING ALL JOBS!%n");
					jobs.running_jobs().forEach(j -> j.kill());
				}
			}
		});
	}
}
