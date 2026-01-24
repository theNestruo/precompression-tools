package com.github.thenestruo.msx.precompression;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

import org.tinylog.Logger;

import com.github.thenestruo.commons.IntArrays;
import com.github.thenestruo.commons.math.Range;
import com.github.thenestruo.commons.msx.MsxCharset;
import com.github.thenestruo.commons.msx.MsxColor;
import com.github.thenestruo.commons.msx.MsxLine;

public class MsxCharsetOptimizer {

	private Range<Integer> exclusion = null;

	public MsxCharsetOptimizer setExclusion(final int from, final int to) {
		return this.setExclusion(new Range<>(from, to));
	}

	public MsxCharsetOptimizer setExclusion(final Range<Integer> exclusion) {
		this.exclusion = exclusion;
		return this;
	}

	//

	/**
	 * The default {@link #colorOrder} matches the order of
	 * the {@link com.github.thenestruo.commons.msx.MsxPalettes#YAZIOH_PALETTE}
	 * sorted by {@link com.github.thenestruo.commons.color.Color#relativeLuminance()}
	 * because empirical test have shown a slightly better compression ratio.
	 */
	private static final List<Byte> DEFAULT_COLOR_ORDER = Collections.unmodifiableList(Arrays.asList(
			(byte) 0x0, (byte) 0x1, (byte) 0x4, (byte) 0x6, (byte) 0xD, (byte) 0x5, (byte) 0x8, (byte) 0xC,
			(byte) 0x2, (byte) 0x9, (byte) 0x3, (byte) 0xA, (byte) 0x7, (byte) 0xE, (byte) 0xB, (byte) 0xF));

	/**
	 * The palette indexes, sorted by brigthness/luminance
	 *
	 * @see #optimizeNotPreferredBackground(MsxLine, byte)
	 */
	private List<Byte> colorOrder = DEFAULT_COLOR_ORDER;

	public MsxCharsetOptimizer setColorOrder(final List<MsxColor> palette, final ToDoubleFunction<MsxColor> function) {

		if ((palette == null) || (function == null)) {
			return this.setColorOrder(null);
		}

		final ArrayList<MsxColor> modifiableList = new ArrayList<>(Objects.requireNonNull(palette));
		modifiableList.sort(
				Comparator.comparingDouble(Objects.requireNonNull(function))
						.thenComparing(Comparator.comparingInt(MsxColor::getIndex)));
		return this.setColorOrder(modifiableList.stream().map(MsxColor::getIndex).toList());
	}

	public MsxCharsetOptimizer setColorOrder(final List<Byte> colorOrder) {

		if ((colorOrder == null) || (colorOrder.size() != DEFAULT_COLOR_ORDER.size())) {
			this.colorOrder = DEFAULT_COLOR_ORDER;
			return this;
		}

		this.colorOrder = Collections.unmodifiableList(new ArrayList<>(colorOrder));
		return this;
	}

	//

	public MsxCharset optimize(final MsxCharset charset) {

		// Locates the most common color (to be used as background where possible)
		final int[] colorDistribution = this.computeColorDistribution(charset);
		final byte preferredBackground = (byte) IntArrays.indexOfMax(colorDistribution);

		Logger.debug("Preferred background: {} (most common color). Luminance order: {}. Color distribution: {}.",
				String.format("%01X", preferredBackground),
				this.colorOrder.stream().map(i -> String.format("%01X", i)).collect(Collectors.joining()),
				Arrays.toString(colorDistribution));

		// (creates a mutable instance)
		final MsxCharset optimizedCharset = MsxCharset.copyOf(charset);

		MsxLine previousValue = MsxLine.backgroundOfColor(preferredBackground);
		for (int i = 0, n = charset.size(); i < n; i++) {
			final MsxLine candidate = charset.get(i);
			final MsxLine optimized = (this.exclusion == null) || (!this.exclusion.contains(i))
					? this.optimize(candidate, preferredBackground, previousValue)
					: candidate;
			optimizedCharset.set(i, optimized);
			previousValue = optimized;
		}

		return optimizedCharset;
	}

	/**
	 * @param charset the MSX charset data
	 * @return the number of pixels of each color
	 */
	private int[] computeColorDistribution(final MsxCharset charset) {

		final int colorDistribution[] = new int[16];

		for (int i = 0, n = charset.size(); i < n; i++) {
			final MsxLine line = charset.get(i);
			final byte pattern = line.chrtblByte();
			final int fg = line.fg();
			final int bg = line.bg();
			for (int bit = 0; bit < 8; bit++) {
				final int colorIndex = ((pattern & (1 << bit)) != 0) ? fg : bg;
				colorDistribution[colorIndex]++;
			}
		}
		return colorDistribution;
	}

	/**
	 * @param candidate           the MSX line to be optimized
	 * @param preferredBackground the most common color (to be used as background where possible)
	 * @param previousValue       the previous MSX line (to reuse the CLRTBL byte where possible)
	 * @return the optimized MSX line
	 */
	private MsxLine optimize(final MsxLine candidate, final byte preferredBackground, final MsxLine previousValue) {

		return candidate.isSingleColor()
				? this.optimizeSingleColor(candidate, preferredBackground, previousValue)
				: this.optimizeTwoColors(candidate, preferredBackground, previousValue);
	}

	private MsxLine optimizeSingleColor(
			final MsxLine candidate, final byte preferredBackground, final MsxLine previousValue) {

		// Single color
		final byte singleColor = candidate.singleColor();

		// Attempts to reuse the previous CLRTBL value...
		if (previousValue != null) {

			// ...as single color background/foreground
			if (singleColor == previousValue.bg()) {
				return MsxLine.backgroundUsing(previousValue);
			}
			if (singleColor == previousValue.fg()) {
				return MsxLine.foregroundUsing(previousValue);
			}
		}

		// Attempts to use preferred background
		if (singleColor == preferredBackground) {
			return MsxLine.backgroundOfColor(preferredBackground);
		}

		// Uses the single color as foreground over the preferred background
		return MsxLine.foregroundOf(singleColor, preferredBackground);
	}

	private MsxLine optimizeTwoColors(
			final MsxLine candidate, final byte preferredBackground, final MsxLine previousValue) {

		// Two colors

		// Attempts to use the preferred background
		if (candidate.bg() == preferredBackground) {
			return candidate;
		}
		if (candidate.fg() == preferredBackground) {
			return candidate.inverted();
		}

		// Attempts to reuse the previous CLRTBL value
		if (candidate.clrtblByte() == previousValue.clrtblByte()) {
			return candidate;
		}
		if (candidate.invertedClrtblByte() == previousValue.clrtblByte()) {
			return candidate.inverted();
		}

		// Most common color not present
		return this.optimizeNotPreferredBackground(candidate, preferredBackground);
	}

	private MsxLine optimizeNotPreferredBackground(final MsxLine candidate, final byte preferredBackground) {

		// Two colors, most common color not present

		final int referenceIndex = this.colorOrder.indexOf(preferredBackground);
		final int bgIndex = this.colorOrder.indexOf(candidate.bg());
		final int fgIndex = this.colorOrder.indexOf(candidate.fg());

		// Background should be darker when the preferred background is dark,
		// and brighter when the preferred background is bright
		final boolean isInverted = referenceIndex < 8
				? bgIndex > fgIndex
				: bgIndex < fgIndex;

		return isInverted ? candidate.inverted() : candidate;
	}
}
