package org.eng.util;

import org.eng.cache.IMultiKeyCache;
import org.eng.cache.MemoryCache;

/**
 * Provides caching of the items contain in an arbitrary IShuffleIterable.
 * This may be useful when re-iterating through iterables that may require non-trivial effort to {@link #dereference(String)}.
 * For examples, reading from files or modifying iterable items somehow.
 * @param <ITEM>
 */
public class CachingShuffleIterable<ITEM> extends AbstractReferenceShuffleIterable<ITEM, CachingShuffleIterable<ITEM>> implements IShuffleIterable<ITEM> {

	/** Holds our cached items */ 
	private final IMultiKeyCache<String, ITEM> cache; 

	/** Iterable over which we are providing caching */
	private final IShuffleIterable<ITEM> iterable;
	
	private static <ITEM> IMultiKeyCache<String,ITEM> tryToShareCache(IShuffleIterable<ITEM> iterable) {
		// TODO: probably not safe to share caches if two iterables use the same references to refer to different items.
//		if (iterable instanceof CachingShuffleIterable<?>)
//			return ((CachingShuffleIterable)iterable).cache;
//		else
			return new MemoryCache<String,ITEM>();
	}

	public CachingShuffleIterable(IShuffleIterable<ITEM> iterable) {
		this(iterable.getReferences(), iterable, tryToShareCache(iterable));
	}

	private CachingShuffleIterable(Iterable<String> references, IShuffleIterable<ITEM> iterable, IMultiKeyCache<String, ITEM> cache) {
		super(references);
		this.iterable = iterable;
		this.cache = cache;
	}

	@Override
	public ITEM dereference(String reference) {
		ITEM item = cache.get(reference);
		if (item == null) {
//			AISPLogger.logger.info("Cache miss on reference : " + reference);
			item = iterable.dereference(reference);
			cache.put(item, reference);
//		} else {
//			AISPLogger.logger.info("Cache hit on reference : " + reference);
		}
		return item;
	}

	@Override
	public CachingShuffleIterable<ITEM> newIterable(Iterable<String> newReferences) {
		return new CachingShuffleIterable<ITEM>(newReferences, this.iterable, this.cache);	// Share out cache with the new instance since we will be using the same references.
	}

}
