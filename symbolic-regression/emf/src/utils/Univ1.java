// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package utils;

import java.util.LinkedHashMap;
import java.util.function.Function;

public class Univ1<K, T> {
	private LinkedHashMap<K, T> univ = new LinkedHashMap<>();

	private final Function<K, T> constructor;

	public Univ1(Function<K, T> constructor) {
		this.constructor = constructor;
	}

	public T get(K k) {
		return univ.computeIfAbsent(k, k1 -> constructor.apply(k1));
	}
}
