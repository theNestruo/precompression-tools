package com.github.thenestruo.msx.precompression.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.github.thenestruo.msx.precompression.model.Optimization;

public class PrioritizeColorOptimizationMerger extends AbstractOptimizationMerger {

	public static final PrioritizeColorOptimizationMerger INSTANCE = new PrioritizeColorOptimizationMerger();

	private PrioritizeColorOptimizationMerger() {
		super();
	}

	@Override
	public List<Optimization> doMerge(
			final List<Optimization> patternList, final List<Optimization> colorList) {

		// Pattern optimizations not overlapped by any color optimization
		final List<Optimization> mergedList = new ArrayList<>(patternList);
		for (final Iterator<Optimization> it = mergedList.iterator(); it.hasNext(); ) {
			final Optimization pattern = it.next();
			for (final Optimization color : colorList) {
				if (pattern.isOverlappedBy(color)) {
					it.remove();
					break;
				}
			}
		}

		// All color optimizations
		mergedList.addAll(colorList);

		return mergedList;
	}

}
