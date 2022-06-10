// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package utils;

/** These differ from InputErrorCode, in that these are really never ever supposed to happen. They are RuntimeErrors, and so are not annotated. */
public enum MalformedInputErrorCode {
	/** used any time "" is used an id for an entity (Node, Seg, etc) */
	EMPTY_STRING_AS_ID,
	
	REFERENTIAL_INTEGRITY_FAILURE,
	
	INVALID_COMMODITY_TYPE,

	NEGATIVE_VALUE,
	
	/** used for any input that is supposed to represent a 1-1 mapping */
	BAD_MAP,
	
	/** catch-all for any value that isn't in an accepted set */
	INVALID_VALUE,
}
