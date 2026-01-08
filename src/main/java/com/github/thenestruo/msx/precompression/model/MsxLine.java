package com.github.thenestruo.msx.precompression.model;

import java.util.Objects;

/**
 * A line of 8 pixels
 */
public class MsxLine {

	public static final byte PATTERN_FG = (byte) 0xff;

	public static final byte PATTERN_BG = (byte) 0x00;

	/** The line pattern */
	private final byte pattern;

	/** The line color */
	private final byte color;

	public MsxLine(final byte pattern, final byte color) {
		this.pattern = pattern;
		this.color = color;
	}

	@Override
	public String toString() {
		return String.format("%02X %01X %01X", this.pattern, this.fg(), this.bg());
	}

	/**
	 * @param that the other MsxLine to compare
	 * @return {@code true} if both MsxLines are equivalent and will look the same, {@code false} otherwise
	 */
	public boolean isEquivalentTo(final MsxLine that) {

		if ((this.pattern == that.pattern)
				&& (this.color == that.color)) {
			return true;
		}
		if ((this.invertedPattern() == that.pattern)
				&& (this.invertedColor() == that.color)) {
			return true;
		}
		if ((this.singleColor() != null)
				&& Objects.equals(this.singleColor(), that.singleColor())) {
			return true;
		}
		return false;
	}

	//

	public byte pattern() {
		return this.pattern;
	}

	/**
	 * @return the pattern for the line inverted from this line
	 */
	public byte invertedPattern() {
		return (byte) (this.pattern ^ 0xff);
	}

	//

	public byte color() {
		return this.color;
	}

	/**
	 * @return the foreground color
	 */
	public byte fg() {
		return (byte) ((this.color >> 4) & 0x0f);
	}

	/**
	 * @return the background color
	 */
	public byte bg() {
		return (byte) (this.color & 0x0f);
	}

	/**
	 * @return if the line has a single color, the single color.
	 *         {@code null} otherwise
	 */
	public Byte singleColor() {
		if (this.pattern == PATTERN_FG) {
			return this.fg();
		}
		if (this.pattern == PATTERN_BG) {
			return this.bg();
		}
		if (this.fg() == this.bg()) {
			return this.fg();
		}
		return null;
	}

	/**
	 * @return the color for the line inverted from this line
	 */
	public byte invertedColor() {
		return (byte) ((this.bg() << 4) | (this.fg() & 0x0f));
	}

	//

	/**
	 * @return a line, inverted from this line
	 */
	public MsxLine inverted() {
		return new MsxLine(this.invertedPattern(), this.invertedColor());
	}

	/**
	 * @param reference the MsxLine used to set the pattern
	 * @return a line with the reference pattern and this line color
	 */
	public MsxLine withPatternOf(final MsxLine reference) {
		return new MsxLine(reference.pattern, this.color);
	}

	/**
	 * @param reference the MsxLine used to set the color
	 * @return a line with the reference color and this line pattern
	 */
	public MsxLine withColorOf(final MsxLine reference) {
		return new MsxLine(this.pattern, reference.color);
	}

	/**
	 * @param singleColor the single color
	 * @return a line with a single color and this line pattern (unused)
	 */
	public MsxLine withSingleColor(final byte singleColor) {
		return this.withColor((byte) ((singleColor << 4) | (singleColor & 0x0f)));
	}

	/**
	 * @param color the color byte
	 * @return a line with the specified color and this line pattern
	 */
	public MsxLine withColor(final byte color) {
		return new MsxLine(this.pattern, color);
	}

}
