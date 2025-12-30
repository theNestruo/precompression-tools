package com.github.thenestruo.msx.precompression.impl;

import com.github.thenestruo.msx.precompression.MsxLineOptimizer;
import com.github.thenestruo.msx.precompression.model.MsxLine;

public enum NullMsxLineOptimizer implements MsxLineOptimizer {

	INSTANCE;

	@Override
	public boolean isPattern() {
		return false;
	}

	@Override
	public boolean isColor() {
		return false;
	}

	@Override
	public MsxLine optimize(final MsxLine candidate, final MsxLine reference) {
		return null;
	}
}
