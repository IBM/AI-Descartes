// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package utils;

import static utils.OptError.malformedInputError;

import java.util.Comparator;

public interface ObjId {
	String id();
	
	//void dump(PrintStream out, String fmt, Object... s);
	
	final public static Comparator<ObjId> compareIds = new Comparator<ObjId>() {
		@Override
		public int compare(ObjId x, ObjId y) {
			return x.id().compareTo(y.id());
		}
	};
	
	final public static Comparator<ObjId> idComparator = compareIds; 

	public static class ObjIdSuperClass implements ObjId {
		public final String id;
		public String id() { return id; }
		
		public ObjIdSuperClass(String id) {
			this.id = id;
			if (id.equals(""))
				malformedInputError(MalformedInputErrorCode.EMPTY_STRING_AS_ID, "empty string");
		}
	}
}
