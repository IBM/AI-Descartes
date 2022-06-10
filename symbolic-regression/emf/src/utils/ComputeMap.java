// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package utils;

import utils.UMap;

/** this is not actually immutable... but only increases. */
public class ComputeMap<K, V> extends UMap<K, V> {
	//	public Function<K, V> constructor;
	//
	//	private boolean done = false;
	//
	//	public void done() {
	//		done = true;
	//	}
	//
	//	public ComputeMap(Function<K, V> constructor) {
	//		super("", new LinkedHashMap<>());
	//		this.constructor = constructor;
	//	}
	//
	//	public V getOrCompute(K x) {
	//		myassert(x != null);
	//		if (done)
	//			return get(x);
	//		return map.computeIfAbsent(x, constructor);
	//	}

	public ComputeMap() {
		super("howdy");
	}

	public void init(UMap<K, V> map) {
		super.init(map);
	}
}
