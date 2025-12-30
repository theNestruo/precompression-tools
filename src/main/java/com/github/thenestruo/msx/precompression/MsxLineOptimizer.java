package com.github.thenestruo.msx.precompression;

import com.github.thenestruo.msx.precompression.model.MsxLine;

public interface MsxLineOptimizer {

	boolean isPattern();

	boolean isColor();

	/**
	 * @param candidate the MsxLine candidate for optimization
	 * @param reference the MsxLine used to set the optimized values
	 * @return optimized MsxLine if already optimized or can be optimized,
	 * {@code null} otherwise
	 */
	MsxLine optimize(MsxLine candidate, MsxLine reference);
}
