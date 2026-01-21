package com.github.thenestruo.msx.precompression.impl;

import com.github.thenestruo.msx.precompression.MsxLineOptimizer;
import com.github.thenestruo.msx.precompression.model.MsxLine;

public enum PatternOnlyMsxLineOptimizer implements MsxLineOptimizer {

	INSTANCE;

	@Override
	public String toString() {
		return "PatternOnly";
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

		// Optimize "unused" pattern
		if (candidate.fg() == candidate.bg()) {
			return candidate.withPatternOf(reference);
		}

		// Cannot optimize
		return null;
	}

}
