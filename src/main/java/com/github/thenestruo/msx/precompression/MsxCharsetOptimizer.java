package com.github.thenestruo.msx.precompression;

import com.github.thenestruo.msx.precompression.model.MsxCharset;

public interface MsxCharsetOptimizer {

	MsxCharsetOptimizer withPatternOptimizer(MsxLineOptimizer patternOptimizer);

	MsxCharsetOptimizer withColorOptimizer(MsxLineOptimizer colorOptimizer);

	MsxCharsetOptimizer withMerger(OptimizationMerger merger);

	MsxCharsetOptimizer withExclusion(int from, int to);

	MsxCharset optimize(MsxCharset charset);
}
