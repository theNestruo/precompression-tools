package com.github.thenestruo.msx.precompression.impl;

import com.github.thenestruo.msx.precompression.MsxLineOptimizer;
import com.github.thenestruo.msx.precompression.model.MsxLine;

public enum PatternAndColorMsxLineOptimizer implements MsxLineOptimizer {

	INSTANCE;

	@Override
	public String toString() {
		return "PatternAndColor";
	}

	@Override
	public boolean isPattern() {
		return true;
	}

	@Override
	public boolean isColor() {
		return false;
	}

	@Override
	public MsxLine optimize(final MsxLine candidate, final MsxLine reference) {

		// (sanity check)
		if ((candidate == null) || (reference == null)) {
			return null;
		}

		// Already optimized
		if (candidate.pattern() == reference.pattern()) {
			return candidate;
		}

		// Optimize if invert matches reference pattern
		if (candidate.invertedPattern() == reference.pattern()) {
			return candidate.inverted();
		}

		// Optimize "unused" pattern
		final Byte singleColor = candidate.singleColor();
		if (singleColor != null) {
			return candidate.withPatternOf(reference).withSingleColor(singleColor);
		}

		// Cannot optimize
		return null;
	}

}
