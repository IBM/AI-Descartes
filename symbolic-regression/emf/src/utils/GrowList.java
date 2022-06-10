// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package utils;

import java.util.ArrayList;

public class GrowList<T> {
	//	private boolean done = false;
	//
	//	public GrowList() {
	//		super();
	//	}
	//
	//	// has to override UList.add()
	//	public UList<T> add(T x) {
	//		VRAUtils.myassert(!done);
	//		//return vals.add(x);
	//		vals.add(x);
	//		return this;
	//	}
	//
	//	public boolean push(T x) {
	//		VRAUtils.myassert(!done);
	//		return vals.add(x);
	//	}
	//
	//	public void done() {
	//		done = true;
	//	}
	//
	//	public boolean isDone() {
	//		return done;
	//	}

	private final ArrayList<T> vals = new ArrayList<>();
	private boolean done = false;
	private boolean doneWhenFetched;

	public GrowList() {
		this(false);
	}

	public GrowList(boolean doneWhenFetched) {
		this.doneWhenFetched = doneWhenFetched;
	}

	public GrowList(UList<T> init) {
		vals.addAll(init.asCollection());
	}

	public void add(T x) {
		VRAUtils.myassert(!done);
		vals.add(x);
	}

	public boolean checkAdd(T x) {
		VRAUtils.myassert(!done);
		boolean rv = vals.contains(x);
		vals.add(x);
		return rv;
	}

	public boolean push(T x) {
		VRAUtils.myassert(!done);
		return vals.add(x);
	}

	public void done() {
		done = true;
	}

	public UList<T> elems() {
		if (doneWhenFetched)
			done();
		return UList.mkUList(vals);
	}

	public int currentSize() {
		if (doneWhenFetched)
			done();
		return vals.size();
	}

	public boolean isDone() {
		return done;
	}
}
