package com.github.thenestruo.msx.precompression;

import java.util.List;

import com.github.thenestruo.msx.precompression.model.Optimization;

public interface OptimizationMerger {

	List<Optimization> merge(final List<Optimization> a, final List<Optimization> b);
}
