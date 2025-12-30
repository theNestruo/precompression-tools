package com.github.thenestruo.msx.precompression.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

import com.github.thenestruo.msx.precompression.OptimizationMerger;
import com.github.thenestruo.msx.precompression.model.Optimization;

public enum PrioritizeColorOptimizationMerger implements OptimizationMerger {

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

		// Ensures proper order
		Collections.sort(mergedList, Optimization.MINIMUM_COMPARATOR);
		return mergedList;
	}

}
