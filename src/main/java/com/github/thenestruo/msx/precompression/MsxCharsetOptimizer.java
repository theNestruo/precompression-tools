package com.github.thenestruo.msx.precompression;

import com.github.thenestruo.commons.math.Range;
import com.github.thenestruo.msx.precompression.model.MsxCharset;

public interface MsxCharsetOptimizer {

	MsxCharsetOptimizer setExclusion(int from, int to);

	MsxCharsetOptimizer setExclusion(Range<Integer> exclusion);

	MsxCharset optimize(MsxCharset charset);
}
