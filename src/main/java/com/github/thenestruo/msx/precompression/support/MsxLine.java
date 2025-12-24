package com.github.thenestruo.msx.precompression.support;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * A line of 8 pixels
 */
public class MsxLine {

	/**
	 * @param candidate the optimization candidate MsxLine
	 * @param reference the MsxLine used to set the optimized values
	 * @return optimized MsxLine if already optimized or can be optimized,
	 * {@code null} otherwise
	 */
	public static MsxLine optimizedPattern(final MsxLine candidate, final MsxLine reference) {

		return (candidate == null || reference == null)
				? null
				: candidate.optimizedPatternUsing(reference);
	}

	/**
	 * @param candidate the optimization candidate MsxLine
	 * @param reference the MsxLine used to set the optimized values
	 * @return optimized MsxLine if already optimized or can be optimized,
	 * {@code null} otherwise
	 */
	public static MsxLine optimizedColor(final MsxLine candidate, final MsxLine reference) {

		return (candidate == null || reference == null)
				? null
				: candidate.optimizedColorUsing(reference);
	}

	private static final byte PATTERN_FG = (byte) 0xff;

	private static final byte PATTERN_BG = (byte) 0x00;

	/** The line pattern */
	private final byte pattern;

	/** The line color */
	private final byte color;

	public MsxLine(final byte pattern, final byte color) {
		this.pattern = pattern;
		this.color = color;
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder()
				.append(this.pattern)
				.append(this.color)
				.toHashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof MsxLine)) {
			return false;
		}
		final MsxLine that = (MsxLine) obj;
		return new EqualsBuilder()
				.append(this.pattern, that.pattern)
				.append(this.color, that.color)
				.isEquals();
	}

	@Override
	public String toString() {
		return String.format("%02X %01X %01X", this.pattern, this.fg(), this.bg());
	}

	/**
	 * @return a line, inverted from this line
	 */
	public MsxLine inverted() {
		return new MsxLine(
			(byte) (this.pattern ^ 0xff),
			(byte) ((this.bg() << 4) | (this.fg() & 0x0f)));
	}

	/**
	 * @param reference the MsxLine used to set the optimized values
	 * @return optimized MsxLine if already optimized or can be optimized,
	 * {@code null} otherwise
	 */
	public MsxLine optimizedPatternUsing(final MsxLine reference) {

		// (sanity check)
		if (reference == null) {
			return null;
		}

		// Already optimized
		if (this.pattern == reference.pattern) {
			return this;
		}

		// Optimize if invert matches reference pattern
		if (this.inverted().pattern == reference.pattern) {
			return this.inverted();
		}

		// Optimize "unused" pattern
		final Byte singleColor = this.singleColor();
		if (singleColor != null) {
			return this.withPattern(reference).withSingleColor(singleColor);
		}

		// Cannot optimize
		return null;
	}

	/**
	 * @param reference the MsxLine used to set the optimized values
	 * @return optimized MsxLine if already optimized or can be optimized,
	 * {@code null} otherwise
	 */
	public MsxLine optimizedColorUsing(final MsxLine reference) {

		// (sanity check)
		if (reference == null) {
			return null;
		}

		// Already optimized
		if (this.color == reference.color) {
			return this;
		}

		// All FG: optimize if FG color matches any reference color
		if (this.pattern == PATTERN_FG) {
			return	  (this.fg() == reference.fg()) ? this.withColor(reference)
					: (this.fg() == reference.bg()) ? this.inverted().withColor(reference)
					: null;
		}

		// All BG: optimize if BG color matches any reference color
		if (this.pattern == PATTERN_BG) {
			return	  (this.bg() == reference.fg()) ? this.inverted().withColor(reference)
					: (this.bg() == reference.bg()) ? this.withColor(reference)
					: null;
		}

		// Single color: optimize if single color matches any reference color
		if (this.fg() == this.bg()) {
			return	  (this.fg() == reference.fg()) ? new MsxLine(PATTERN_FG, reference.color)
					: (this.fg() == reference.bg()) ? new MsxLine(PATTERN_BG, reference.color)
					: null;
		}

		// Two colors: optimize if invert matches reference colors
		if (this.inverted().color == reference.color) {
			return this.inverted();
		}

		// Cannot optimize
		return null;
	}

	//

	/**
	 * @return the foreground color
	 */
	private byte fg() {
		return (byte) ((this.color >> 4) & 0x0f);
	}

	/**
	 * @return the background color
	 */
	private byte bg() {
		return (byte) (this.color & 0x0f);
	}

	/**
	 * @return if the line has a single color, the single color.
	 * {@code null} otherwise
	 */
	private Byte singleColor() {
		return	  this.pattern == PATTERN_FG ? Byte.valueOf(this.fg())
				: this.pattern == PATTERN_BG ? Byte.valueOf(this.bg())
				: this.fg() == this.bg() ? Byte.valueOf(this.fg())
				: null;
	}

	/**
	 * @param reference the MsxLine used to set the pattern
	 * @return a line with the reference pattern and this line color
	 */
	private MsxLine withPattern(final MsxLine reference) {
		return new MsxLine(reference.pattern, this.color);
	}

	/**
	 * @param reference the MsxLine used to set the color
	 * @return a line with the reference color and this line pattern
	 */
	private MsxLine withColor(final MsxLine reference) {
		return new MsxLine(this.pattern, reference.color);
	}

	/**
	 * @param singleColor the single color
	 * @return a line with a single color and this line pattern (unused)
	 */
	private MsxLine withSingleColor(final byte singleColor) {
		return this.withColor((byte) ((singleColor << 4) | (singleColor & 0x0f)));
	}

	/**
	 * @param color the color byte
	 * @return a line with the specified color and this line pattern
	 */
	private MsxLine withColor(final byte color) {
		return new MsxLine(this.pattern, color);
	}

	public byte getPattern() {
		return this.pattern;
	}

	public byte getColor() {
		return this.color;
	}
}
