// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package utils;

import java.util.function.Supplier;

import utils.UMap;

public class DelayedInitUMap<K, V> extends UMap<K, V> {
	private final Supplier<UMap<K, V>> supplier;

	public DelayedInitUMap(Supplier<UMap<K, V>> supplier) {
		super("howdy");
		this.supplier = supplier;
	}

	public void init() {
		super.init(supplier.get());
	}

	//	public static <TT> DelayedInitUList<TT> empty() {
	//		return new DelayedInitUList<TT>(() -> UList.empty());
	//	}

}
