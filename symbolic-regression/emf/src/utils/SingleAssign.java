// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package utils;

import static utils.VRAUtils.*;

public class SingleAssign<T> {
	private T x;

	public SingleAssign() {
	}

	public T get() {
		myassert(this.x != null);
		return x;
	}

	public void set(T x) {
		myassert(this.x == null);
		this.x = x;
	}

	public boolean isPresent() {
		return x != null;
	}

}
