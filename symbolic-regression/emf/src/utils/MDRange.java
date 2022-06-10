// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package utils;

import static utils.UList.*;

public class MDRange {
	public final boolean nullset;

	public final OList<DRange> rngs;

	public static MDRange newMDRange(UList<DRange> rngs0) {
		return new MDRange(rngs0);
	}

	public String toString() {
		return "MR[" + rngs.map(x -> x.lbound + "," + x.ubound).join("][") + "]";
	}

	private MDRange(UList<DRange> rngs0) {
		super();
		if (rngs0.isEmpty()) {
			this.nullset = true;
			this.rngs = OList.emptyOUL();
		} else {
			OList<DRange> rngs1 = sort(rngs0.filter(x -> !x.nullset).distinct()
					.filter(x -> !rngs0.some(y -> x.subrange(y) && !x.equals(y))));
			int i;
			for (i = 0; i < rngs1.size() - 1; ++i) {
				if (rngs1.get(i).intersects(rngs1.get(i + 1)))
					break;
			}
			if (i == rngs1.size() - 1) {
				this.nullset = false;
				this.rngs = rngs1;
			} else {
				GrowList<DRange> rngs2 = new GrowList<>(rngs1.subList(0, i));
				DRange r = rngs1.get(i).intersect(rngs1.get(i + 1));
				for (i += 2; i < rngs1.size() - 1; ++i) {
					if (r.intersects(rngs1.get(i)))
						r = r.intersect(rngs1.get(i));
					else {
						rngs2.add(r);
						r = rngs1.get(i);
					}
				}
				rngs2.add(r);
				this.nullset = false;
				this.rngs = sort(rngs1);
			}
		}
	}

	//private static OList<DRange> fixranges(UList<>)

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = nullset ? 1 : 2;
		result = prime * result + rngs.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		@SuppressWarnings("unchecked")
		MDRange that = (MDRange) obj;
		if (nullset || that.nullset)
			return nullset && that.nullset;
		return this.rngs.equals(that.rngs);
	}

}
