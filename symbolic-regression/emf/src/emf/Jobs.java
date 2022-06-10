// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package emf;

import static utils.UList.*;
import static utils.VRAUtils.*;

import java.io.IOException;
import java.util.Optional;

import utils.OList;
import utils.UList;
import utils.UMap;

public class Jobs {
	public final Input inpx;
	public final Input2 inpx2;
	private OList<BaronJob> jobsLeft;

	private UList<BaronJob> running_jobs = UList.empty();
	private UList<BaronJob> running_jobs_restart_list = UList.empty();
	private UList<BaronJob> failed_jobs = UList.empty();
	private UList<BaronJob> finished_jobs = UList.empty();
	private UList<BaronJob> unstarted_hopeless_jobs = UList.empty();
	private UList<RunResult> finished_results = UList.empty();

	public Jobs(Input2 inpx2, UList<BaronJob> jobs) {
		this.jobsLeft = sort(jobs);
		this.inpx2 = inpx2;
		this.inpx = inpx2.inpx;
	}

	public UList<BaronJob> failed_jobs() {
		return failed_jobs;
	}

	public UList<BaronJob> running_jobs() {
		return running_jobs;
	}

	public UList<BaronJob> jobsLeft() {
		return jobsLeft;
	}

	public UList<RunResult> finished_results() {
		return finished_results;
	}

	public boolean allDone() {
		return running_jobs.isEmpty() && jobsLeft.isEmpty();
	}

	public void addJobs(UList<BaronJob> newjobs) {
		//		OList<BaronJob> oldjobs = jobsLeft;
		this.jobsLeft = new OList<>(jobsLeft.concat(newjobs));
	}

	public void startMoreJobs(UList<RunResultVal> bestres) throws IOException {
		UList<RunResultVal> bestres0 = bestres.filter(rv -> rv.val <= inpx.hopelessJobThresh);
		// these jobs would just be immediately killed as hopeless if we started them.
		//		UList<BaronJob> new_hopeless = jobsLeft.groupingBy(j -> {
		//			Optional<RunResultVal> better = bestres0.findFirst(rv -> rv.baronJob.expr.complexity <= j.expr.complexity);
		//			if (better.isPresent())
		//				printf("(%s:%s is very good and less complex, so not running %s)%n", j.expr.nodeNum.get(), j.expr,
		//						better.get().baronJob.expr.nodeNum.get(), better.get().baronJob.expr);
		//			return better.isPresent();
		//		});

		UMap<Optional<RunResultVal>, UList<BaronJob>> new_hopeless = maybeList(inpx.stop_hopeless_jobs, jobsLeft)
				.groupingBy(j -> bestres0.findFirst(rv -> rv.baronJob.expr.complexity <= j.expr.complexity))
				.filterByKeys(x -> x.isPresent());

		if (new_hopeless.isNotEmpty())
			new_hopeless.forEachEntry(
					(better, jlist) -> printf("(%s:%s is very good and less complex, so not running %s)%n",
							better.get().baronJob.expr.nodeNum.get(), better.get().baronJob.expr, jlist));

		final OList<BaronJob> jobsLeft = this.jobsLeft.diffUL(concatULists(new_hopeless.values()));
		//		new_hopeless.forEach(j-> printf("(Not running %s:%s, since %s:%s is very good and less complex)%n", j.expr.nodeNum.get(), j.expr, ));

		UList<BaronJob> jobsToStart = inpx.multitask ? jobsLeft
				: jobsLeft.subList(0, Math.min(jobsLeft.size(), inpx.max_num_jobs - running_jobs.size()));
		//		printf("JOBS %s%n", jobsToStart);
		int i = 0;
		for (BaronJob j : jobsToStart) {
			// COULD START GIVING BEST RESULT SO FAR - drop for now
			//			RunResult res = best_results.maybeGet(j.complexity);
			//				printf("Starting %s with bnd %s%n", j.index, res == null ? inpx.infinity : res.val.get());
			j.start(inpx.infinity); // res == null ? inpx.infinity : res.val.get());
			if (inpx.multitask && i >= inpx.max_num_jobs)
				j.suspend();
			i++;
		}
		running_jobs = running_jobs.concat(jobsToStart);
		running_jobs_restart_list = running_jobs_restart_list.concat(jobsToStart);

		this.jobsLeft = jobsLeft.subListOL(jobsToStart.size());
	}

	private int multitask_secs;

	public UList<RunResult> checkDone() {
		if (inpx.reuse_baron_files)
			return running_jobs.map(j -> j.result());

		UList<BaronJob> done_jobs = running_jobs.filter(j -> j.isDone());
		running_jobs = running_jobs.diff(done_jobs);
		running_jobs_restart_list = running_jobs_restart_list.diff(done_jobs);
		if (inpx.multitask && running_jobs.size() >= inpx.max_num_jobs) {
			//			printf("CHECK %s%n", multitask_secs);
			if (multitask_secs++ >= inpx.multitask_secs) {
				multitask_secs = 0;
				int nToStop = Math.min(inpx.max_num_jobs, running_jobs_restart_list.size() - inpx.max_num_jobs);
				UList<BaronJob> toStop = running_jobs_restart_list.subList(0, nToStop);
				toStop.forEach(j -> j.suspend());
				running_jobs_restart_list = running_jobs_restart_list.subList(nToStop).concat(toStop);
				UList<BaronJob> toResume = running_jobs_restart_list.subList(0, inpx.max_num_jobs).diff(toStop);

				toResume.forEach(j -> j.resume());
				//				printf("(running: %s)%n", running_jobs_restart_list.subList(0, inpx.max_num_jobs));
			}
		}

		jobsLeft = jobsLeft.diffUL(done_jobs);
		UList<BaronJob> failed = done_jobs.filter(j -> !j.hopeless() && !j.result().isPresent());
		failed_jobs = failed_jobs.concat(failed);
		finished_jobs = finished_jobs.concat(done_jobs.diff(failed));
		if (failed.isNotEmpty()) {
			printf("THESE JOBS FAILED: %s%n", failed);
			UList<BaronJob> syntax = failed.filter(j -> j.syntax_error());
			if (syntax.isNotEmpty())
				printf("RERUNNING (except %s, which had syntax errors)%n.", syntax);
			else
				printf("RERUNNING%n.");
			jobsLeft = jobsLeft.concat(sort(failed.diff(syntax).map(x -> x.rerun())));
		}
		UList<RunResult> newres = done_jobs.filter(j -> j.result().isPresent()).map(j -> j.result());
		finished_results = finished_results.concat(newres);

		return newres;
	}

	//	public void kill_remaining_jobs() {
	//		running_jobs().forEach(x -> x.kill());
	// }

	public UList<BaronJob> relatedJobs(BaronJob pj) {
		return finished_jobs.filter(jb -> pj.expr == jb.expr).concat(running_jobs).filter(jb -> pj.expr == jb.expr)
				.concat(jobsLeft).filter(jb -> pj.expr == jb.expr);
	}

	public UList<BaronJob> relatedFinishedJobs(BaronJob pj) {
		return finished_jobs.filter(jb -> pj.expr == jb.expr);
	}

	public void stopJobsIfHopeless(UList<RunResultVal> bestres) {
		UMap<Optional<RunResultVal>, UList<BaronJob>> hopeless = running_jobs()
				.groupingBy(j -> j.stopJobIfHopeless(bestres)).filterByKeys(x -> x.isPresent());

		if (hopeless.isNotEmpty())
			hopeless.forEachEntry((bres, jlist) -> {
				if (bres.get().val <= inpx.hopelessJobThresh)
					printf("(%s is very good and less complex, so killing jobs %s)%n", bres.get().baronJob, jlist);
				else
					printf("(%s has result better than lbounds of these jobs being killed:  %s)%n", bres.get().baronJob,
							jlist);
				//expr.nodeNum.get(), expr, lbnd, bres.get().val, bres.get().baronJob.expr);
			});

	}
}
/*
		int doneInd = running_jobs.findFirstIndex(j -> j.isDone());
			if (doneInd >= 0) {
				BaronJob j = running_jobs.get(doneInd);
				running_jobs = running_jobs.remove(doneInd);
				//				printf("JOBx %s %s%n", j, j.result().get().val);
				njobs_finished++;
				if (!j.result().isPresent()) {
					printf("JOB %s FAILED%n", j.index);
					failed_jobs = failed_jobs.add(j);
				} else {
					RunResult res = j.result().get();
					results = results.addEntry(j.complexity, cons(res, results.getOrElse(j.complexity, UList.empty())));

					if (res.val.isPresent()) {
						if (!best_results.containsKey(j.complexity)
								|| res.val.get() < best_results.get(j.complexity).val.get()) {
							best_results = best_results.addEntry(j.complexity, res);
							printf("new best for complexity %s by %s: %s %s%n", j.complexity, j.index, res.val.get(),
									j.expr);
						}
					} else
						jobs_to_rerun = jobs_to_rerun.add(j);
				}
				noChange = false;
			}

			if (nextToStart == jobs_to_run.size()) {
				// finished all jobs at this complexity, start next level
				if (currentComplexity < maxComplexity) {
					currentComplexity++;
					while (!jobs_by_complexity.containsKey(currentComplexity))
						currentComplexity++;
					if (currentComplexity == maxComplexity) {
						jobs_to_run = UList.empty();
						if (running_jobs.isNotEmpty())
							printf("=== Started all jobs, waiting for %s to finish.%n", running_jobs.size());
						else
							printf("=== Processed all jobs%n");
					} else {
						jobs_to_rerun_by_complexity = jobs_to_rerun_by_complexity.addEntry(currentComplexity,
								jobs_to_rerun);
						printf("=== Processing complexity %s%n", currentComplexity);
						jobs_to_run = jobs_by_complexity.get(currentComplexity);
					}
					nextToStart = 0;
				} else if (running_jobs.isNotEmpty())
					printf("=== Waiting for remaining %s jobs to finish %s%n", running_jobs.size(),
							running_jobs.map(j -> j.index));
			}

			while (running_jobs.size() < inpx.max_num_jobs && nextToStart < jobs_to_run.size()) {
				BaronJob j = jobs_to_run.get(nextToStart++);
				RunResult res = best_results.maybeGet(j.complexity);
				//				printf("Starting %s with bnd %s%n", j.index, res == null ? inpx.infinity : res.val.get());
				j.start(res == null ? inpx.infinity : res.val.get());
				myassert(running_jobs.all(jb -> jb.index != j.index));
				running_jobs = running_jobs.add(j);
				noChange = false;
			}
*/
