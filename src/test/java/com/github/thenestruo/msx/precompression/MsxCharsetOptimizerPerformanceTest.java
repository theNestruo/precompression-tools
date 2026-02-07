package com.github.thenestruo.msx.precompression;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.tinylog.Logger;

import com.github.thenestruo.commons.io.ClassPathResource;
import com.github.thenestruo.commons.maps.Pair;
import com.github.thenestruo.commons.math.IntStats;
import com.github.thenestruo.commons.msx.MsxCharset;
import com.github.thenestruo.commons.msx.MsxColor;
import com.github.thenestruo.commons.msx.MsxPalettes;

public class MsxCharsetOptimizerPerformanceTest {

	private static final List<String> FILENAMES = Collections.unmodifiableList(Arrays.asList(
			"ninjasenki.png",
			"pyramidwarpex.png",
			"stevedore.png",
			"youkaiyashiki.png"
		));

	private static final Map<String, IntStats> referenceUncompressedTotalSizes = new LinkedHashMap<>();
	private static final Map<String, IntStats> referenceZx0TotalSizes = new LinkedHashMap<>();
	private static final Map<String, IntStats> zx0TotalSizes = new LinkedHashMap<>();

	@BeforeAll
	static void beforeAll() {
		referenceUncompressedTotalSizes.clear();
		referenceZx0TotalSizes.clear();
		zx0TotalSizes.clear();
	}

	@ParameterizedTest
	@MethodSource("performanceTestArguments")
	void performanceTest(final String filename,
			final List<MsxColor> colorOrderPalette,
			final ToDoubleFunction<MsxColor> colorOrderFunction,
			final Boolean forceStrippedImage,
			final String label) throws IOException {

		// Given

		final byte[] chrBytes;
		final byte[] clrBytes;
		try (
				final InputStream chrInputStream = new ClassPathResource(filename + ".chr").getInputStream();
				final InputStream clrInputStream = new ClassPathResource(filename + ".clr").getInputStream()) {
			chrBytes = chrInputStream.readAllBytes();
			clrBytes = clrInputStream.readAllBytes();
		}
		Assumptions.assumeTrue(chrBytes.length == clrBytes.length);
		final int referenceUncompressedChrSize = chrBytes.length;
		final int referenceUncompressedClrSize = clrBytes.length;
		final int referenceUncompressedTotalSize = referenceUncompressedChrSize + referenceUncompressedClrSize;
		referenceUncompressedTotalSizes
				.computeIfAbsent(label, x -> new IntStats())
				.add(referenceUncompressedTotalSize);

		final MsxCharset referenceCharset = MsxCharset.of(chrBytes, clrBytes);

		// When

		final MsxCharset optimizedCharset = new MsxCharsetOptimizer()
				.setColorOrder(colorOrderPalette, colorOrderFunction)
				.setForceStrippedImage(forceStrippedImage)
				.optimize(referenceCharset);

		// Then

		for (int i = 0; i < referenceUncompressedChrSize; i++) {
			Assertions.assertTrue(
					referenceCharset.get(i).isEquivalentTo(optimizedCharset.get(i)),
					"Expected " + referenceCharset.get(i) + ", but found: " + optimizedCharset.get(i));
		}

		// Performance traces

		final int referenceZx0ChrSize = zx0(chrBytes).length;
		final int referenceZx0ClrSize = zx0(clrBytes).length;
		final int referenceZx0TotalSize = referenceZx0ChrSize + referenceZx0ClrSize;
		referenceZx0TotalSizes
				.computeIfAbsent(label, x -> new IntStats())
				.add(referenceZx0TotalSize);

		final double referenceZx0Ratio = (100.0d * referenceZx0TotalSize) / referenceUncompressedTotalSize;

		final int zx0ChrSize = zx0(optimizedCharset.chrtbl()).length;
		final int zx0ClrSize = zx0(optimizedCharset.clrtbl()).length;
		final int zx0TotalSize = zx0ChrSize + zx0ClrSize;
		zx0TotalSizes
				.computeIfAbsent(label, x -> new IntStats())
				.add(zx0TotalSize);

		final double zx0Ratio = (100.0d * zx0TotalSize) / referenceUncompressedTotalSize;

		final int zx0ChrDelta = zx0ChrSize - referenceZx0ChrSize;
		final int zx0ClrDelta = zx0ClrSize - referenceZx0ClrSize;
		final int zx0Delta = zx0TotalSize - referenceZx0TotalSize;
		final double zx0RatioDelta = zx0Ratio - referenceZx0Ratio;

		Logger.info(String.format("""
				B:%4d (%4d+%4d) [%+5d (%+5d%+5d)]  CR:%2.2f%% [%+3.2f%%] \
				<- B:%4d (%4d+%4d)  CR:%2.2f%% \
				<- B:%4d \
				:: %s :: %s""",
				//
				zx0TotalSize, zx0ChrSize, zx0ClrSize, zx0Delta, zx0ChrDelta, zx0ClrDelta,
				zx0Ratio, zx0RatioDelta,
				//
				referenceZx0TotalSize, referenceZx0ChrSize, referenceZx0ClrSize,
				referenceZx0Ratio,
				//
				referenceUncompressedTotalSize,
				//
				filename, label));
	}

	private static Stream<Arguments> performanceTestArguments() {

		final List<Arguments> list = new ArrayList<>();

		for (final Boolean forceStrippedImage : new Boolean[]{ null, Boolean.TRUE, Boolean.FALSE }) {

			for (final String filename : FILENAMES) {

				for (final Pair<String, List<MsxColor>> palette : Arrays.asList(
						Pair.of("(unset)", (List<MsxColor>) null),
						Pair.of("TMS9918", MsxPalettes.TMS9918_PALETTE),
						Pair.of("TMS9219", MsxPalettes.TMS9219_PALETTE),
						Pair.of("YAZIOH ", MsxPalettes.YAZIOH_PALETTE),
						Pair.of("TOSHIBA", MsxPalettes.TOSHIBA_PALETTE),
						Pair.of("V9938  ", MsxPalettes.V9938_PALETTE)
					)) {

					for (final Pair<String, ToDoubleFunction<MsxColor>> function : Arrays.asList(
							Pair.of("(unset)            ", (ToDoubleFunction<MsxColor>) null),
							Pair.of("relativeLuminance  ", (ToDoubleFunction<MsxColor>) MsxColor::relativeLuminance),
							Pair.of("perceivedBrightness", (ToDoubleFunction<MsxColor>) MsxColor::perceivedBrightness),
							Pair.of("brightness         ", (ToDoubleFunction<MsxColor>) MsxColor::brightness)
						)) {

						final String paletteLabel = palette.getKey();
						final String functionLabel = function.getKey();
						final String strippedLabel =
								  forceStrippedImage == null ? "auto"
								: forceStrippedImage.booleanValue() ? "stripped" : "non-stripped";
						final String label = String.format("%s-%s-%s", paletteLabel, functionLabel, strippedLabel);
						list.add(Arguments.of(filename, palette.getValue(), function.getValue(), forceStrippedImage, label));
					}
				}
			}
		}

		return list.stream();
	}

	@AfterAll
	static void afterAll() {

		final List<Map.Entry<String, IntStats>> list = new ArrayList<>(zx0TotalSizes.entrySet());
		Collections.sort(list, Comparator.<Map.Entry<String, IntStats>>comparingInt(e -> e.getValue().sum()));

		for (final Map.Entry<String, IntStats> entry : list) {
			final String label = entry.getKey();

			final int referenceUncompressedTotalSize = referenceUncompressedTotalSizes.get(label).sum();

			final int referenceZx0TotalSize = referenceZx0TotalSizes.get(label).sum();
			final double referenceZx0Ratio = (100.0d * referenceZx0TotalSize) / referenceUncompressedTotalSize;

			final int zx0TotalSize = entry.getValue().sum();
			final int zx0Delta = zx0TotalSize - referenceZx0TotalSize;
			final double zx0Ratio = (100.0d * zx0TotalSize) / referenceUncompressedTotalSize;
			final double zx0RatioDelta = zx0Ratio - referenceZx0Ratio;

			final IntStats dStats = IntStats.ofDifferences(entry.getValue(), referenceZx0TotalSizes.get(label));
			// final FloatStats rStats = FloatStats.ofRatios(entry.getValue(), referenceZx0TotalSizes.get(label));

			Logger.info(String.format("""
					Total :: B:%4d \
					-> B:%4d  CR:%2.2f%% \
					-> B:%4d [%+5d]  CR:%2.2f%% [%+3.2f%%] \
					:: STATS[%+d..%+d, avg:%+5.2f, median:%+d] \
					:: %s""",
					referenceUncompressedTotalSize,
					referenceZx0TotalSize, referenceZx0Ratio,
					zx0TotalSize, zx0Delta, zx0Ratio, zx0RatioDelta,
					dStats.min(), dStats.max(), dStats.average(), dStats.median(),
					label));
		}
	}

	private static byte[] zx0(final byte[] input) {

		final int skip = 0;
		final boolean backwardsMode = false;
		final boolean classicMode = false;
		final boolean quickMode = true; // false;
		final int threads = 1;
		final boolean verbose = false;
		final int[] delta = { 0 };

		return new zx0.Compressor().compress(
				new zx0.Optimizer().optimize(
						input, skip, quickMode ? zx0.Main.MAX_OFFSET_ZX7 : zx0.Main.MAX_OFFSET_ZX0, threads, verbose),
				input, skip, backwardsMode, !classicMode && !backwardsMode, delta);
	}
}
