package com.github.thenestruo.msx.precompression.impl;

import com.github.thenestruo.msx.precompression.MsxLineOptimizer;
import com.github.thenestruo.msx.precompression.model.MsxLine;

public enum ColorAndPatternMsxLineOptimizer implements MsxLineOptimizer {

	INSTANCE;

	@Override
	public String toString() {
		return "ColorAndPattern";
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
		if (candidate.pattern() == MsxLine.PATTERN_FG) {
			return	  (candidate.fg() == reference.fg()) ? candidate.withColorOf(reference)
					: (candidate.fg() == reference.bg()) ? candidate.inverted().withColorOf(reference)
					: null;
		}

		// All BG: optimize if BG color matches any reference color
		if (candidate.pattern() == MsxLine.PATTERN_BG) {
			return	  (candidate.bg() == reference.fg()) ? candidate.inverted().withColorOf(reference)
					: (candidate.bg() == reference.bg()) ? candidate.withColorOf(reference)
					: null;
		}

		// Single color: optimize if single color matches any reference color
		if (candidate.fg() == candidate.bg()) {
			return	  (candidate.fg() == reference.fg()) ? new MsxLine(MsxLine.PATTERN_FG, reference.color())
					: (candidate.fg() == reference.bg()) ? new MsxLine(MsxLine.PATTERN_BG, reference.color())
					: null;
		}

		// Two colors: optimize if invert matches reference colors
		if (candidate.invertedColor() == reference.color()) {
			return candidate.inverted();
		}

		// Cannot optimize
		return null;
	}

}
