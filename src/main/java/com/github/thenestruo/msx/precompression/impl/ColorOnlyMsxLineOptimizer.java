package com.github.thenestruo.msx.precompression.impl;

import com.github.thenestruo.msx.precompression.MsxLineOptimizer;
import com.github.thenestruo.msx.precompression.model.MsxLine;

public enum ColorOnlyMsxLineOptimizer implements MsxLineOptimizer {

	INSTANCE;

	@Override
	public String toString() {
		return "ColorOnly";
	}

	@Override
	public boolean isPattern() {
		return false;
	}

	@Override
	public boolean isColor() {
		return true;
	}

	@Override
	public MsxLine optimize(final MsxLine candidate, final MsxLine reference) {

		// (sanity check)
		if ((candidate == null) || (reference == null)) {
			return null;
		}

		// Already optimized
		if (candidate.color() == reference.color()) {
			return candidate;
		}

		// All FG: optimize if FG color matches any reference color
		if ((candidate.pattern() == MsxLine.PATTERN_FG) && (candidate.fg() == reference.fg())) {
			return candidate.withColorOf(reference);
		}

		// All BG: optimize if BG color matches any reference color
		if ((candidate.pattern() == MsxLine.PATTERN_BG) && (candidate.bg() == reference.bg())) {
			return candidate.withColorOf(reference);
		}

		// Cannot optimize
		return null;
	}

}
