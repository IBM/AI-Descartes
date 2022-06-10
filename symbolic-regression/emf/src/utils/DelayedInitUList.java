// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package utils;

import static utils.VRAUtils.*;

import java.util.function.Supplier;

import utils.UList;

public class DelayedInitUList<T> extends UList<T> {
	private final Supplier<UList<T>> supplier;
	private final String nm;

	public DelayedInitUList(Supplier<UList<T>> supplier) {
		this(supplier, "NONAME");
	}

	public DelayedInitUList(Supplier<UList<T>> supplier, String nm) {
		super(nm);
		this.supplier = supplier;
		this.nm = nm;
	}

	public DelayedInitUList() {
		super("howdy");
		this.supplier = null;
		this.nm = "NONAME";
	}

	public void init() {
		printf("INITING ULIST %s%n", nm);
		super.init(supplier.get());
	}

	public void init(UList<T> initval) {
		super.init(initval);
	}

	public static <TT> DelayedInitUList<TT> empty() {
		return new DelayedInitUList<TT>(() -> UList.empty());
	}
}
