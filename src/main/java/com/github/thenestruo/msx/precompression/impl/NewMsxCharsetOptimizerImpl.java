package com.github.thenestruo.msx.precompression.impl;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.tinylog.Logger;

import com.github.thenestruo.commons.math.Range;
import com.github.thenestruo.msx.precompression.MsxCharsetOptimizer;
import com.github.thenestruo.msx.precompression.MsxLineOptimizer;
import com.github.thenestruo.msx.precompression.OptimizationMerger;
import com.github.thenestruo.msx.precompression.model.MsxCharset;
import com.github.thenestruo.msx.precompression.model.MsxLine;

public class NewMsxCharsetOptimizerImpl implements MsxCharsetOptimizer {

	private Range<Integer> exclusion = null;

	@Override
	public MsxCharsetOptimizer setPatternOptimizer(MsxLineOptimizer patternOptimizer) {
		throw new UnsupportedOperationException();
	}

	@Override
	public MsxCharsetOptimizer setColorOptimizer(MsxLineOptimizer colorOptimizer) {
		throw new UnsupportedOperationException();
	}

	@Override
	public MsxCharsetOptimizer setMerger(OptimizationMerger merger) {
		throw new UnsupportedOperationException();
	}

	@Override
	public MsxCharsetOptimizer setExclusion(final int from, final int to) {
		return this.setExclusion(new Range<>(from, to));
	}

	@Override
	public MsxCharsetOptimizer setExclusion(final Range<Integer> exclusion) {
		this.exclusion = exclusion;
		return this;
	}

	@Override
	public MsxCharset optimize(final MsxCharset charset) {

		final byte mostCommonColor = this.findMostCommonColor(charset);

		// (creates a mutable instance)
		final MsxCharset optimizedCharset = new MsxCharset(charset);

		MsxLine previousValue = new MsxLine(MsxLine.PATTERN_BG, mostCommonColor, mostCommonColor);
		for (int i = 0, n = charset.size(); i < n; i++) {
			final MsxLine candidate = charset.get(i);
			final MsxLine optimized =
					(this.exclusion != null) && (this.exclusion.contains(i))
					? candidate
					: this.optimize(candidate, mostCommonColor, previousValue);

			optimizedCharset.set(i, optimized);

			Logger.debug("{} {} {} (#{})", candidate, candidate.equals(optimized) ? "==" : "->", optimized, i);

			previousValue = optimized;
		}

		return optimizedCharset;
	}

	private byte findMostCommonColor(final MsxCharset charset) {

		final int count[] = new int[16];

		for (int i = 0, n = charset.size(); i < n; i++) {
			final MsxLine line = charset.get(i);
			final byte pattern = line.pattern();
			final int fg = line.fg();
			final int bg = line.bg();
			for (int b = 0; b < 8; b++) {
				count[((pattern & (1 << b)) != 0) ? fg : bg]++;
			}
		}

		Logger.debug("Color distribution: {}",
				Arrays.stream(count).mapToObj(Integer::toString).collect(Collectors.joining(", ")));

		int mostCommonColor = 0;
		for (int i = 1, n = count.length; i < n; i++) {
			if (count[i] > count[mostCommonColor]) {
				mostCommonColor = i;
			}
		}

		Logger.debug("Most common color: {}", mostCommonColor);

		return (byte) mostCommonColor;
	}

	private MsxLine optimize(MsxLine candidate, byte mostCommonColor, MsxLine previousValue) {

		final Byte singleColor = candidate.singleColor();

		// Single color
		if (singleColor != null) {

			// Attempt to continue the sequence
			if (previousValue != null) {
				if (singleColor.byteValue() == previousValue.bg()) {
					return previousValue.withPattern(MsxLine.PATTERN_BG);
				}
				if (singleColor.byteValue() == previousValue.fg()) {
					return previousValue.withPattern(MsxLine.PATTERN_FG);
				}
			}

			// etc.
			if (singleColor.byteValue() == mostCommonColor) {
				return new MsxLine(MsxLine.PATTERN_BG, mostCommonColor, mostCommonColor);
			}
			return new MsxLine(MsxLine.PATTERN_FG, singleColor, mostCommonColor);
		}

		// Two colores, one is the most common color
		if (candidate.bg() == mostCommonColor) {
			return candidate;
		}
		if (candidate.fg() == mostCommonColor) {
			return candidate.inverted();
		}

		// Two colors, no match with the most common color
		if (candidate.fg() < candidate.bg()) {
			return candidate.inverted();
		}
		return candidate;
	}
}
