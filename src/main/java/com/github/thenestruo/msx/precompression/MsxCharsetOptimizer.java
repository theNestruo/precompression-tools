package com.github.thenestruo.msx.precompression;

import org.apache.commons.lang3.NumberRange;

import com.github.thenestruo.msx.precompression.model.MsxCharset;

public interface MsxCharsetOptimizer {

	MsxCharsetOptimizer setPatternOptimizer(MsxLineOptimizer patternOptimizer);

	MsxCharsetOptimizer setColorOptimizer(MsxLineOptimizer colorOptimizer);

	MsxCharsetOptimizer setMerger(OptimizationMerger merger);

	MsxCharsetOptimizer setExclusion(int from, int to);

	MsxCharsetOptimizer setExclusion(NumberRange<Integer> exclusion);

	MsxCharset optimize(MsxCharset charset);
}
