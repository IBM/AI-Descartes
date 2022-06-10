// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package utils;

public interface UListInt<T, TL extends UList<T>> {
	//	List<T> vals();

	//	default boolean isEmpty() {
	//		return vals().isEmpty();
	//	}

	public TL construct(UList<T> x);

	public UList<T> getThis();

	default TL diff(TL x) {
		UList<T> that = getThis();
		return construct(that.diff(x));
	}

	default TL concat(TL x) {
		UList<T> that = getThis();
		return construct(that.concat(x));
	}

	//	default UListInt<T> concat(UListInt<T> x) {
	//		if (vals().isEmpty())
	//			return x;
	//		if (x.isEmpty())
	//			return this;
	//		ArrayList<T> union = new ArrayList<T>(vals());
	//		union.addAll(x.vals());
	//		return new UList<>(union);
	//	}
}
