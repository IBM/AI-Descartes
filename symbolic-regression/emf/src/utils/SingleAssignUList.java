// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package utils;

public class SingleAssignUList<T> extends UList<T> {
	//	private Supplier<UList<T>> constructor;
	//
	//	public LazyUList(Supplier<UList<T>> constructor) {
	//		super("howdy");
	//
	//		myassert(constructor != null);
	//		this.constructor = constructor;
	//	}
	//	
	//	private void construct
	//
	//	public T get(int ind) {
	//		if (constructor != null) {
	//			myassert(vals.isEmpty());
	//			vals.addAll(constructor.get().vals);
	//			constructor = null;
	//		}
	//
	//		return vals.get(ind);
	//	}

	public SingleAssignUList() {
		super("howdy");
	}

	public void init(UList<T> vals) {
		super.init(vals);
	}
}
