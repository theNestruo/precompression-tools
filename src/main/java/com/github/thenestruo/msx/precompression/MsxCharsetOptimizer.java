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

	/**
	 * The default {@link #colorOrder} matches the order of
	 * the {@link com.github.thenestruo.commons.msx.MsxPalettes#YAZIOH_PALETTE}
	 * sorted by {@link com.github.thenestruo.commons.color.Color#relativeLuminance()}
	 * because empirical test have shown a slightly better compression ratio:
	 * {@code 0146C285D937ABEF}
	 */
	private static final List<Byte> DEFAULT_COLOR_ORDER = Collections.unmodifiableList(Arrays.asList(
			// MsxPalettes#YAZIOH_PALETTE, Color#brightness:
			// (byte) 0x0, (byte) 0x1, (byte) 0x4, (byte) 0x6, (byte) 0xC, (byte) 0x2, (byte) 0x8, (byte) 0x5,
			// (byte) 0xD, (byte) 0x9, (byte) 0x3, (byte) 0x7, (byte) 0xA, (byte) 0xB, (byte) 0xE, (byte) 0xF

			// MsxPalettes#YAZIOH_PALETTE, Color#relativeLuminance:
			(byte) 0x0, (byte) 0x1, (byte) 0x4, (byte) 0x6, (byte) 0xD, (byte) 0x5, (byte) 0x8, (byte) 0xC,
			(byte) 0x2, (byte) 0x9, (byte) 0x3, (byte) 0xA, (byte) 0x7, (byte) 0xE, (byte) 0xB, (byte) 0xF
		));

	/**
	 * The palette indexes, sorted by brigthness/luminance.
	 * A typical charset may have some tiles with similar graphics with different
	 * colors;
	 * choosing the foreground/background colors based on luminance
	 * cause those graphics to have similar byte sequences in the CHRTBL data.
	 * Therefore, it prevents worse CHRTBL compression ratios,
	 * and has no effect on CLRTBL compression ratios.
	 */
	private List<Byte> colorOrder = DEFAULT_COLOR_ORDER;

	public MsxCharsetOptimizer setColorOrder(final List<MsxColor> palette, final ToDoubleFunction<MsxColor> function) {

		if ((palette == null) || (function == null)) {
			return this.setColorOrder(null);
		}

		final ArrayList<MsxColor> modifiableList = new ArrayList<>(Objects.requireNonNull(palette));
		modifiableList.sort(Comparator
				.comparingDouble(Objects.requireNonNull(function))
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

	private Boolean forceStrippedImage = null;

	public MsxCharsetOptimizer setForceStrippedImage(final Boolean forceStrippedImage) {
		this.forceStrippedImage = forceStrippedImage;
		return this;
	}

	private Range<Integer> exclusion = null;

	public MsxCharsetOptimizer setExclusion(final int from, final int to) {
		return this.setExclusion(new Range<>(from, to));
	}

	public MsxCharsetOptimizer setExclusion(final Range<Integer> exclusion) {
		this.exclusion = exclusion;
		return this;
	}

	//

	public MsxCharset optimize(final MsxCharset charset) {

		boolean strippedImage = this.detectStrippedImage(charset);
		return new Process(charset, this.colorOrder, strippedImage, this.exclusion)
				.optimize();
	}

	private boolean detectStrippedImage(final MsxCharset charset) {

		// Forced to yes/no?
		if (this.forceStrippedImage != null) {
			return this.forceStrippedImage.booleanValue();
		}

		final int[] colorCount = charset.colorCount(MsxLine::colorCountByPixel);
		final int[] evenLinesColorCount = charset.evenLinesColorCount(MsxLine::colorCountByPixel);
		final int[] oddLinesColorCount = charset.oddLinesColorCount(MsxLine::colorCountByPixel);

		final byte mostFrequentColor = (byte) IntArrays.indexOfMax(colorCount);
		final byte mostFrequentEvenColor = (byte) IntArrays.indexOfMax(evenLinesColorCount);
		final byte mostFrequentOddColor = (byte) IntArrays.indexOfMax(oddLinesColorCount);

		// Even/odd background is reference background and the other one is not?
		if ((mostFrequentColor == mostFrequentEvenColor) == (mostFrequentColor == mostFrequentOddColor)) {
			return false;
		}

		final int maxColorCount = colorCount[mostFrequentColor];
		final int maxEvenColorCount = evenLinesColorCount[mostFrequentEvenColor];
		final int maxOddColorCount = oddLinesColorCount[mostFrequentOddColor];

		// Enough excess of reference background representation in even/odd background (>= 5%)?
		final int excess = (mostFrequentColor == mostFrequentEvenColor)
				? Math.abs(maxEvenColorCount * 2 - maxColorCount) / 2
				: Math.abs(maxOddColorCount  * 2 - maxColorCount) / 2;
		final int threshold = 5 * charset.size() / 100;
		return excess >= threshold;
	}

	//

	private static class Process {

		private final MsxCharset charset;

		private final List<Byte> colorOrder;

		private final boolean strippedImage;

		private final Range<Integer> exclusion;

		private final byte preferredBackground;

		//

		private Process(final MsxCharset charset, final List<Byte> colorOrder, final boolean strippedImage,
				final Range<Integer> exclusion) {
			this.charset = charset;
			this.colorOrder = colorOrder;
			this.strippedImage = strippedImage;
			this.exclusion = exclusion;

			// Locates the most common color (to be used as background where possible)
			final int[] colorCountByPixel = charset.colorCount(MsxLine::colorCountByPixel);
			this.preferredBackground = (byte) IntArrays.indexOfMax(colorCountByPixel);

			Logger.debug("Colors: count={}, preferred bg={}, dark-to-light color order={}",
					Arrays.toString(colorCountByPixel),
					String.format("%01X", this.preferredBackground),
					this.colorOrder.stream().map(i -> String.format("%01X", i)).collect(Collectors.joining()));
		}

		private transient MsxLine previousValue;

		public MsxCharset optimize() {

			// (creates a mutable instance)
			final MsxCharset optimizedCharset = MsxCharset.copyOf(this.charset);

			this.previousValue = MsxLine.backgroundOfColor(this.preferredBackground);
			for (int i = 0, n = this.charset.size(); i < n; i++) {
				final MsxLine candidate = this.charset.get(i);

				final boolean isExcluded = (this.exclusion != null) && this.exclusion.contains(i);
				final MsxLine optimized = isExcluded ? candidate : this.optimize(candidate);
				optimizedCharset.set(i, optimized);

				this.previousValue = optimized;
			}

			return optimizedCharset;
		}

		/**
		 * @param candidate the MSX line to be optimized
		 * @return the optimized MSX line
		 */
		private MsxLine optimize(final MsxLine candidate) {

			if (candidate.isEquivalentTo(this.previousValue)) {
				// Best case scenario: continue using both CHRTBL and CLRTBL values
				this.debug(candidate, this.previousValue, "Same as previous line");
				return this.previousValue;
			}

			return candidate.isSingleColor()
					? this.optimizeSingleColor(candidate)
					: this.optimizeTwoColors(candidate);
		}

		private MsxLine optimizeSingleColor(final MsxLine candidate) {

			if (this.strippedImage) {
				// This seems to yield better compression ratios than more complex algorithms
				// for stripped images that have rapidly changing either CHRLTBL or CLRTBL bytes
				final MsxLine optimized = MsxLine.foregroundOf(candidate.singleColor(), this.preferredBackground);
				this.debug(candidate, optimized, "Full foreground");
				return optimized;
			}

			// Single color
			final byte singleColor = candidate.singleColor();

			// Attempts to reuse the previous CLRTBL value
			if (singleColor == this.previousValue.bg()) {
				final MsxLine optimized = MsxLine.backgroundUsing(this.previousValue);
				this.debug(candidate, optimized, "Full background (reuses previous line colors)");
				return optimized;
			}
			if (singleColor == this.previousValue.fg()) {
				final MsxLine optimized = MsxLine.foregroundUsing(this.previousValue);
				this.debug(candidate, optimized, "Full foreground (reuses previous line colors)");
				return optimized;
			}

			final int referenceIndex = this.colorOrder.indexOf(this.preferredBackground);
			final int singleColorIndex = this.colorOrder.indexOf(singleColor);
			final boolean isBackground = (referenceIndex < 8) == (singleColorIndex < 8);

			if (isBackground) {
				final MsxLine optimized = MsxLine.backgroundOf((byte) 0x00, singleColor);
				this.debug(candidate, optimized, "Full background");
				return optimized;
			}

			final MsxLine optimized = MsxLine.foregroundOf(singleColor, this.preferredBackground);
			this.debug(candidate, optimized, "Full foreground (over preferred background)");
			return optimized;
		}

		private MsxLine optimizeTwoColors(final MsxLine candidate) {

			// Attempts to continue using the previous CLRTBL value
			if (candidate.clrtblByte() == this.previousValue.clrtblByte()) {
				final MsxLine optimized = candidate;
				this.debug(candidate, optimized, "Reuses previous line colors");
				return optimized;
			}
			if (candidate.invertedClrtblByte() == this.previousValue.clrtblByte()) {
				final MsxLine optimized = candidate.inverted();
				this.debug(candidate, optimized, "Reuses previous line colors (inverted line)");
				return optimized;
			}

			// Attempts to use the preferred background
			if (candidate.bg() == this.preferredBackground) {
				final MsxLine optimized = candidate;
				this.debug(candidate, optimized, "Preferred background");
				return optimized;
			}
			if (candidate.fg() == this.preferredBackground) {
				final MsxLine optimized = candidate.inverted();
				this.debug(candidate, optimized, "Preferred background (inverted line)");
				return optimized;
			}

			// Two colors, preferred background not present

			final int referenceIndex = this.colorOrder.indexOf(this.preferredBackground);
			final int bgIndex = this.colorOrder.indexOf(candidate.bg());
			final int fgIndex = this.colorOrder.indexOf(candidate.fg());

			// Background should be darker when the preferred background is dark,
			// and brighter when the preferred background is bright
			final boolean isInverted = referenceIndex < 8
					? bgIndex > fgIndex
					: bgIndex < fgIndex;

			if (isInverted) {
				final MsxLine optimized = candidate.inverted();
				this.debug(candidate, optimized, "Two colors (inverted line)");
				return optimized;
			}

			final MsxLine optimized = candidate;
			this.debug(candidate, optimized, "Two colors");
			return optimized;
		}

		private void debug(final MsxLine from, final MsxLine to, final String message) {

			if (!Logger.isDebugEnabled()) {
				return;
			}

			Logger.debug("[{}] {} [{}] (pref.bg={}, previous=[{}]) {}",
					from, from.isSameAs(to) ? "==" : "XX", to,
					this.preferredBackground, this.previousValue, message);
		}
	}
}
