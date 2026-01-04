package com.github.thenestruo.msx.precompression.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.github.thenestruo.msx.precompression.model.Optimization;

public class PrioritizePatternOptimizationMerger extends AbstractOptimizationMerger {

	public static final PrioritizePatternOptimizationMerger INSTANCE = new PrioritizePatternOptimizationMerger();

	private PrioritizePatternOptimizationMerger() {
		super();
	}

	@Override
	public List<Optimization> doMerge(
			final List<Optimization> patternList, final List<Optimization> colorList) {

		// Color optimizations not overlapped by any pattern optimization
		final List<Optimization> mergedList = new ArrayList<>(colorList);
		for (final Iterator<Optimization> it = mergedList.iterator(); it.hasNext(); ) {
			final Optimization color = it.next();
			for (final Optimization pattern : patternList) {
				if (color.isOverlappedBy(pattern)) {
					it.remove();
					break;
				}
			}
		}

		// All color optimizations
		mergedList.addAll(patternList);

		return mergedList;
	}

}
