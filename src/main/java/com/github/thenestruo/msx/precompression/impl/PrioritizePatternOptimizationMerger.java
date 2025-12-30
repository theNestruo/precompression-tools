package com.github.thenestruo.msx.precompression.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

import com.github.thenestruo.msx.precompression.OptimizationMerger;
import com.github.thenestruo.msx.precompression.model.Optimization;

public enum PrioritizePatternOptimizationMerger implements OptimizationMerger {

	INSTANCE;

	@Override
	public List<Optimization> merge(
			final List<Optimization> patternList, final List<Optimization> colorList) {

		// (sanity checks)
		if (CollectionUtils.isEmpty(patternList)) {
			return CollectionUtils.isEmpty(colorList)
					? Collections.emptyList()
					: colorList;
		}
		if (CollectionUtils.isEmpty(colorList)) {
			return patternList;
		}

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

		// Ensures proper order
		Collections.sort(mergedList, Optimization.MINIMUM_COMPARATOR);
		return mergedList;
	}

}
