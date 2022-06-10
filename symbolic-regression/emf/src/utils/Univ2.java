// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package utils;

import java.util.function.BiFunction;

public class Univ2<K1, K2, T> {
	private final Univ1<Pair<K1, K2>, T> univ;

	public Univ2(BiFunction<K1, K2, T> constructor) {
		this.univ = new Univ1<>(k1k2 -> constructor.apply(k1k2.left, k1k2.right));
	}

	public T get(K1 k1, K2 k2) {
		return univ.get(new Pair<>(k1, k2));
	}

	//	private LinkedHashMap<K1, LinkedHashMap<K2, T>> univ = new LinkedHashMap<>();
	//
	//	private final BiFunction<K1, K2, T> constructor;
	//
	//	public Univ2(BiFunction<K1, K2, T> constructor) {
	//		this.constructor = constructor;
	//	}
	//
	//	public T get(K1 k1, K2 k2) {
	//		LinkedHashMap<K2, T> u = univ.computeIfAbsent(k1, k-> new LinkedHashMap<>());
	//		
	//				(Function<Operator, NormNode>) op2 -> new NormNode(op2, inst));
	//	}
}
