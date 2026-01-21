package com.github.thenestruo.msx.precompression.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.tinylog.Logger;

import com.github.thenestruo.commons.ByteArrays;
import com.github.thenestruo.commons.math.Range;
import com.github.thenestruo.msx.precompression.MsxCharsetOptimizer;
import com.github.thenestruo.msx.precompression.MsxLineOptimizer;
import com.github.thenestruo.msx.precompression.OptimizationMerger;
import com.github.thenestruo.msx.precompression.model.MsxCharset;
import com.github.thenestruo.msx.precompression.model.MsxLine;
import com.github.thenestruo.msx.precompression.model.Optimization;

public class MsxCharsetOptimizerImpl implements MsxCharsetOptimizer {

	/** The sensible default for pattern optimizer: disabled */
	private static final MsxLineOptimizer DEFAULT_PATTERN_OPTIMIZER = NullMsxLineOptimizer.INSTANCE;

	/** The sensible default for color optimizer: color and pattern optimizer */
	private static final MsxLineOptimizer DEFAULT_COLOR_OPTIMIZER = ColorAndPatternMsxLineOptimizer.INSTANCE;

	/** The optimization merger for same-type optimizations */
	private static final OptimizationMerger SAME_TYPE_MERGER = DefaultOptimizationMerger.INSTANCE;

	/** The sensible default for optimization merger: prioritize color optimizations */
	private static final OptimizationMerger DEFAULT_MERGER = DefaultOptimizationMerger.INSTANCE;

	//

	/** The pattern optimizer */
	private MsxLineOptimizer patternOptimizer = DEFAULT_PATTERN_OPTIMIZER;

	/** The color optimizer */
	private MsxLineOptimizer colorOptimizer = DEFAULT_COLOR_OPTIMIZER;

	/** The optimization merger */
	private OptimizationMerger merger = DEFAULT_MERGER;

	private Range<Integer> exclusion = null;

	//

	@Override
	public MsxCharsetOptimizer setPatternOptimizer(final MsxLineOptimizer patternOptimizer) {
		this.patternOptimizer = Objects.requireNonNullElse(patternOptimizer, DEFAULT_PATTERN_OPTIMIZER);
		return this;
	}

	@Override
	public MsxCharsetOptimizer setColorOptimizer(final MsxLineOptimizer colorOptimizer) {
		this.colorOptimizer = Objects.requireNonNullElse(colorOptimizer, DEFAULT_COLOR_OPTIMIZER);
		return this;
	}

	@Override
	public MsxCharsetOptimizer setMerger(final OptimizationMerger merger) {
		this.merger = Objects.requireNonNullElse(merger, DEFAULT_MERGER);
		return this;
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

		final int referenceChrtblEntropy = ByteArrays.entropy(charset.chrtbl());
		final int referenceClrtblEntropy = ByteArrays.entropy(charset.clrtbl());
		Logger.debug("Source entropy: CHR = {}, CLR = {}", referenceChrtblEntropy, referenceClrtblEntropy);

		final List<Optimization> optimizationRanges = this.computeOptimizationRanges(charset);
		Logger.debug("Applying {} optimizations...", optimizationRanges.size());

		// (creates a mutable instance)
		final MsxCharset optimizedCharset = new MsxCharset(charset);
		for (final Optimization optimizationRange : optimizationRanges) {
			optimizationRange.applyTo(optimizedCharset);
		}

		final int chrtblEntropy = ByteArrays.entropy(optimizedCharset.chrtbl());
		final int clrtblEntropy = ByteArrays.entropy(optimizedCharset.clrtbl());
		Logger.debug("Optimized entropy: CHR = {} -> {} ({}), CLR = {} -> {} ({})",
				referenceChrtblEntropy, chrtblEntropy, chrtblEntropy - referenceChrtblEntropy,
				referenceClrtblEntropy, clrtblEntropy, clrtblEntropy - referenceClrtblEntropy);

		return optimizedCharset;
	}

	//

	private List<Optimization> computeOptimizationRanges(final MsxCharset charset) {

		final List<Optimization> forwardPatternOptimizations = this.applyExclusion(
				this.findForwardOptimizationRanges(charset, this.patternOptimizer));
		final List<Optimization> backwardsPatternOptimizations = this.applyExclusion(
				this.findBackwardsOptimizationRanges(charset, this.patternOptimizer));

		final List<Optimization> patternOptimizations = SAME_TYPE_MERGER.merge(forwardPatternOptimizations,
				backwardsPatternOptimizations);

		this.debug("patternOptimizations", patternOptimizations, charset);

		//

		final List<Optimization> forwardColorOptimizations = this.applyExclusion(
				this.findForwardOptimizationRanges(charset, this.colorOptimizer));
		final List<Optimization> backwardsColorOptimizations = this.applyExclusion(
				this.findBackwardsOptimizationRanges(charset, this.colorOptimizer));

		final List<Optimization> colorOptimizations = SAME_TYPE_MERGER.merge(forwardColorOptimizations,
				backwardsColorOptimizations);

		this.debug("colorOptimizations", colorOptimizations, charset);

		//

		final List<Optimization> optimizations = this.merger.merge(patternOptimizations, colorOptimizations);

		this.debug("optimizations", optimizations, charset);

		return optimizations;
	}

	private List<Optimization> findForwardOptimizationRanges(final MsxCharset charset,
			final MsxLineOptimizer optimizer) {

		final List<Optimization> ranges = new ArrayList<>();

		final int n = charset.size();
		int i = 0;
		MsxLine reference = charset.get(i++);
		Integer from = null;
		for (; i < n; i++) {
			final MsxLine candidate = charset.get(i);
			final MsxLine optimized = optimizer.optimize(candidate, reference);

			if (optimized != null) {
				reference = optimized;
				if (from == null) {
					// The new range starts in the previous index (i-1)
					from = i - 1;
				}

			} else {
				if (from != null) {
					// The range ends in the previous index (i+1)
					ranges.add(new Optimization(charset, optimizer, reference, from, i - 1));
				}
				reference = candidate;
				from = null;
			}
		}

		if (from != null) {
			// The range ends in the previous index (i+1)
			ranges.add(new Optimization(charset, optimizer, reference, from, i - 1));
		}

		Collections.sort(ranges, Optimization.MINIMUM_COMPARATOR);
		return ranges;
	}

	private List<Optimization> findBackwardsOptimizationRanges(final MsxCharset charset,
			final MsxLineOptimizer optimizer) {

		final List<Optimization> ranges = new ArrayList<>();

		final int n = charset.size();
		int i = n - 1;
		MsxLine reference = charset.get(i--);
		Integer from = null;
		for (; i >= 0; i--) {
			final MsxLine candidate = charset.get(i);
			final MsxLine optimized = optimizer.optimize(candidate, reference);

			if (optimized != null) {
				reference = optimized;
				if (from == null) {
					// The new range starts in the previous index (i+1)
					from = i + 1;
				}

			} else {
				if (from != null) {
					// The range ends in the previous index (i+1)
					ranges.add(new Optimization(charset, optimizer, reference, from, i + 1));
				}
				reference = candidate;
				from = null;
			}
		}

		if (from != null) {
			// The range ends in the previous index (i+1)
			ranges.add(new Optimization(charset, optimizer, reference, from, i + 1));
		}

		Collections.sort(ranges, Optimization.MINIMUM_COMPARATOR);
		return ranges;
	}

	private List<Optimization> applyExclusion(final List<Optimization> pList) {

		// (sanity check)
		if (this.exclusion == null) {
			return pList;
		}

		final List<Optimization> list = new ArrayList<>();
		for (final Optimization range : pList) {
			list.addAll(range.minus(this.exclusion));
		}
		return list;
	}

	private void debug(final String key, final List<Optimization> list, final MsxCharset charset) {

		if (!Logger.isDebugEnabled()) {
			return;
		}

		final char[] array = new char[charset.size()];
		Arrays.fill(array, '_');
		for (final Optimization range : list) {
			final char c = range.optimizer() instanceof PatternAndColorMsxLineOptimizer ? 'P'
					: range.optimizer() instanceof PatternOnlyMsxLineOptimizer ? 'p'
					: range.optimizer() instanceof ColorAndPatternMsxLineOptimizer ? 'C'
					: range.optimizer() instanceof ColorOnlyMsxLineOptimizer ? 'c'
					: '?';
			for (int i = range.getMinimum(), n = range.getMaximum(); (i < n) && (i < array.length); i++) {
				array[i] = c;
			}
		}

		final StringBuilder sb = new StringBuilder();
		for (int i = 0, n = array.length; i < n; i += 8) {
			sb.append(new String(array, i, 8)).append(" ");
		}
		final String s = sb.toString();

		Logger.debug("{} =\n{}", key, s);
	}
}
