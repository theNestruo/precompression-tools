package com.github.thenestruo.msx.precompression.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.NumberRange;
import org.tinylog.Logger;

import com.github.thenestruo.msx.precompression.MsxLineOptimizer;
import com.github.thenestruo.util.ByteArrayUtils;

public class Optimization extends NumberRange<Integer> {

	private static final long serialVersionUID = 3964436030844404674L;

	public static final Comparator<NumberRange<Integer>> MINIMUM_COMPARATOR =
			Comparator.<NumberRange<Integer>> comparingInt(NumberRange<Integer>::getMinimum);

	/** The MSX charset (for {@link #entropy()}) */
	private final MsxCharset charset;

	/** The Optimizer that generated this optimization */
	private final MsxLineOptimizer optimizer;

	/** The reference value used to generate this optimization */
	private final MsxLine sample;

	public Optimization(final MsxCharset charset,
			final MsxLineOptimizer optimizer, final MsxLine sample, final int from, final int to) {
		super(from, to, null);

		this.charset = charset;
		this.optimizer = optimizer;
		this.sample = sample;
	}

	@Override
	public String toString() {
		return String.format("%dx{%s %s}@%s", this.size(), this.optimizer, this.sample, super.toString());
	}

	/**
	 * @param exclusion the exclusion to remove from this optimization
	 * @return the optimization result of removing the exclusion from this optimization:
	 * either this optimization, this optimization but shortened,
	 * or two optimizations (if the exclusion is contained within this optimization)
	 */
	public Collection<? extends Optimization> minus(final NumberRange<Integer> exclusion) {

		if (!this.isOverlappedBy(exclusion)) {
			return Collections.singletonList(this);
		}

		final List<Optimization> list = new ArrayList<>();
		if (exclusion.getMinimum() > this.getMinimum()) {
			list.add(new Optimization(this.charset, this.optimizer, this.sample, this.getMinimum(), exclusion.getMinimum() - 1));
		}
		if (exclusion.getMaximum() < this.getMaximum()) {
			list.add(new Optimization(this.charset, this.optimizer, this.sample, exclusion.getMaximum() + 1, this.getMaximum()));
		}
		return list;
	}

	/**
	 * @return the size of this optimization
	 */
	public int size() {
		return (this.getMaximum() - this.getMinimum()) + 1;
	}

	/**
	 * @return the entropy of this optimization
	 */
	public int entropy() {

		final byte[] array = this.optimizer.isPattern()
				? this.charset.chrtbl()
				: this.charset.clrtbl();

		return ByteArrayUtils.entropy(
				Arrays.copyOfRange(array, this.getMinimum(), this.getMaximum() + 1));
	}

	/**
	 * @param that the other optimization
	 * @return {@code true} if both optimizations are of the same type and have a compatible sample value,
	 * {@code false} otherwise
	 */
	public boolean isMergeableWith(final Optimization that) {

		// Checks same type
		if (this.optimizer.getClass() != that.optimizer.getClass()) {
			return false;
		}

		// Checks compatible sample
		if (this.optimizer.isPattern()) {
			return (this.sample.pattern() == that.sample.pattern())
					|| (this.sample.invertedPattern() == that.sample.pattern());
		}
		if (this.optimizer.isColor()) {
			return (this.sample.color() == that.sample.color())
					|| (this.sample.invertedColor() == that.sample.color());
		}

		// Uncompatible samples
		return false;
	}

	/**
	 * @param that the other optimization
	 * @return an optimization that matches this optimization,
	 * but extended to contain the other optimization
	 */
	public Optimization mergeWith(final Optimization that) {

		// (should never happen)
		if (this.optimizer.getClass() != that.optimizer.getClass()) {
			throw new UnsupportedOperationException();
		}

		return new Optimization(this.charset, this.optimizer, this.sample,
				Math.min(this.getMinimum(), that.getMinimum()),
				Math.max(this.getMaximum(), that.getMaximum()));
	}

	/**
	 * Applies the optimization of this optimization
	 * to the values of the charset within the optimization
	 * @param charset the MSX charset
	 */
	public void applyTo(final MsxCharset charset) {

		for (int i = this.getMinimum(); i <= this.getMaximum(); i++) {
			final MsxLine candidate = charset.get(i);
			final MsxLine optimized = this.optimizer.optimize(candidate, this.sample);

			if (optimized == null) {
				Logger.debug("Expected {} optimization, but got null for {}", candidate, this);
				continue;
			}

			charset.set(i, optimized);
		}
	}

	public MsxLineOptimizer optimizer() {
		return this.optimizer;
	}
}
