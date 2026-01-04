package com.github.thenestruo.msx.precompression.impl;

import java.util.Collections;
import java.util.List;

import com.github.thenestruo.msx.precompression.OptimizationMerger;
import com.github.thenestruo.msx.precompression.model.Optimization;

public abstract class AbstractOptimizationMerger implements OptimizationMerger {

	@Override
	public final List<Optimization> merge(List<Optimization> a, List<Optimization> b) {

		// (sanity checks)
		if (a == null || a.isEmpty()) {
			return b == null || b.isEmpty()
					? Collections.emptyList()
					: b;
		}
		if (b == null || b.isEmpty()) {
			return a;
		}

		final List<Optimization> merged = this.doMerge(a, b);
		if (merged == null || merged.isEmpty()) {
			return Collections.emptyList();
		}

		// Ensures proper order
		Collections.sort(merged, Optimization.MINIMUM_COMPARATOR);

		return merged;
	}

	protected abstract List<Optimization> doMerge(List<Optimization> a, List<Optimization> b);
}
