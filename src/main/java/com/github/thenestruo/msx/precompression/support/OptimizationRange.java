package com.github.thenestruo.msx.precompression.support;

import java.util.Comparator;
import java.util.function.BiFunction;

import org.tinylog.Logger;

public class OptimizationRange {

	public static final Comparator<OptimizationRange> COMPARATOR_BY_FROM =
			Comparator.<OptimizationRange> comparingInt(OptimizationRange::getFrom);

	public static enum Type {
		PATTERN(MsxLine::optimizedPattern),
		COLOR(MsxLine::optimizedColor);

		private final BiFunction<MsxLine, MsxLine, MsxLine> optimizer;

		private Type(final BiFunction<MsxLine, MsxLine, MsxLine> optimizer) {
			this.optimizer = optimizer;
		}

		public BiFunction<MsxLine, MsxLine, MsxLine> getOptimizer() {
			return this.optimizer;
		}
	};

	private final Type type;

	private final MsxLine sample;

	/** The first address this optimization range covers (included) */
	private final int from;

	/** The last address this optimization range covers (included) */
	private final int to;

	public OptimizationRange(final Type type, final MsxLine sample, final int from, final int to) {
		super();

		this.type = type;
		this.sample = sample;

		if (from < to) {
			this.from = from;
			this.to = to;
		} else {
			this.from = to;
			this.to = from;
		}
	}

	@Override
	public String toString() {
		return String.format("%dx{%s %s}@[%d..%d]", this.size(), this.type, this.sample, this.from, this.to);
	}

	/**
	 * @return the size of this optimization range
	 */
	public int size() {
		return this.to - this.from + 1;
	}

	/**
	 * @param that the other optimization range
	 * @return {@code true} if this optimization range ends before the other optimization range stars,
	 * {@code false} otherwise
	 */
	public boolean isStrictlyBefore(final OptimizationRange that) {
		return this.to < that.from;
	}

	/**
	 * @param that the other optimization range
	 * @return {@code true} if the other optimization range is completely contained within this optimization range,
	 * {@code false} otherwise
	 */
	public boolean contains(final OptimizationRange that) {
		return this.from <= that.from && this.to >= that.to;
	}

	/**
	 * @param that the other optimization range
	 * @return {@code true} if both optimization ranges are of the same type and have a compatible sample value,
	 * {@code false} otherwise
	 */
	public boolean isMergeableWith(final OptimizationRange that) {

		// Checks same type
		if (this.type != that.type) {
			return false;
		}

		// Checks compatible sample
		if (this.type == Type.PATTERN) {
			return this.sample.getPattern() == that.sample.getPattern()
					|| this.sample.inverted().getPattern() == that.sample.getPattern();
		}
		if (this.type == Type.COLOR) {
			return this.sample.getColor() == that.sample.getColor()
					|| this.sample.inverted().getColor() == that.sample.getColor();
		}

		// Uncompatible samples
		return false;
	}

	/**
	 * @param that the other optimization range
	 * @return an optimization range that matches this optimization range,
	 * but extended to contain the other optimization range
	 */
	public OptimizationRange mergeWith(final OptimizationRange that) {

		// (should never happen)
		if (this.type != that.type) {
			throw new UnsupportedOperationException();
		}

		return new OptimizationRange(this.type, this.sample,
				Math.min(this.from, that.from),
				Math.max(this.to, that.to));
	}

	/**
	 * Applies the optimization of this optimization range
	 * to the values of the charset within the range
	 * @param charset the MSX charset
	 */
	public void applyTo(final MsxCharset charset) {

		for (int i = this.from; i <= this.to; i++) {
			final MsxLine candidate = charset.get(i);
			final MsxLine optimized = this.type.optimizer.apply(candidate, this.sample);

			if (optimized == null) {
				Logger.warn("Expected {} optimization, but got null for {}", candidate, this);
				continue;
			}

			charset.set(i, optimized);
		}
	}

	public Type getType() {
		return this.type;
	}

	public MsxLine getSample() {
		return this.sample;
	}

	public int getFrom() {
		return this.from;
	}

	public int getTo() {
		return this.to;
	}
}
