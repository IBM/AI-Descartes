// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package utils;

import static java.util.Collections.unmodifiableMap;
import static utils.OptError.malformedInputError;
import static utils.VRAUtils.addMapOnce;
import static utils.VRAUtils.die;
import static utils.VRAUtils.newArrayList;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;



public class ObjUniv<T extends ObjId> implements Iterable<T>, Collection<T>  {
	private LinkedHashMap<String, T> tm = new LinkedHashMap<String, T>(); // for all types
	final private Set<T> allvals = new LinkedHashSet<T>(); // for all types
	final private Set<T> allvs = Collections.unmodifiableSet(allvals);
	
	public final Map<String,T> lookup = unmodifiableMap(tm);
	public Set<T> allvals() {
		return allvs;
	}
	final String prefix;
	
	public ObjUniv(String prefix, Collection<T> xs) {
		this.prefix=prefix;
		for (T x : xs) {
			addMapOnce(tm, x.id(), x);
			allvals.add(x);
		}
	}
	
	public T get(String id) {
		return tm.get(id);
	}

	public List<T> getListOrError(Collection<String> ids, String rectag) { 
		List<T> xs = newArrayList();
		for (String id : ids)
			xs.add(getOrError(id, rectag));
		return xs;
	}
	
	public T getOrError(String id, String rectag) { 
		T x = get(id);
			if (x==null) {
				// the univ prefix will probably not be helpful, but is better than nothing.
				String objtype = (tm.values().iterator().hasNext() ? 
								tm.values().iterator().next().getClass().getName() :  this.prefix);
				malformedInputError(MalformedInputErrorCode.REFERENTIAL_INTEGRITY_FAILURE, "ERROR: no %s (of type %s) found for %s", id, objtype, rectag);
			}
			return x;
		}
	 
	public T getOrNullOrError(String id, String rectag) { 
		if (id==null || id.equals(""))
			return null;
		return getOrError(id,rectag);
	}
	
	///// ITERABLE METHODS
	@Override	
	public int size() { return allvals().size(); }
	
	@Override
	public Iterator<T> iterator() {
		return allvals().iterator();
	}
		

	@Override
	public boolean add(T e) {
		die("DON'T DO this");
		return false;
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		die("DON'T DO this");
		return false;
	}

	@Override
	public void clear() {
		die("DON'T DO this");		
	}

	@Override
	public boolean contains(Object o) {
		return allvals().contains(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return allvals().containsAll(c); 
	}

	@Override
	public boolean isEmpty() {
		return allvals().isEmpty();
	}

	@Override
	public boolean remove(Object o) {
		die("DON'T DO this");
		return false;				
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		die("DON'T DO this");		
		return false;		
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		die("DON'T DO this");		
		return false;
	}

	@Override
	public Object[] toArray() {
		return allvals().toArray();
	}

	@Override
	public <T1> T1[] toArray(T1[] a) {
		return allvals().toArray(a);
	}
	

}
