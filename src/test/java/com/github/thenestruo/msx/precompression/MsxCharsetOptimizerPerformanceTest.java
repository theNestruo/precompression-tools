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
import java.util.Map.Entry;
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

import com.github.thenestruo.commons.ByteArrays;
import com.github.thenestruo.commons.io.ClassPathResource;
import com.github.thenestruo.commons.maps.Pair;
import com.github.thenestruo.commons.msx.MsxCharset;
import com.github.thenestruo.commons.msx.MsxColor;
import com.github.thenestruo.commons.msx.MsxPalettes;

public class MsxCharsetOptimizerPerformanceTest {

	private static final List<String> FILENAMES = Collections.unmodifiableList(Arrays.asList(
			"ninjasenki.png",
			"pyramidwarpex.png",
			"stevedore.png",
			"youkaiyashiki.png",
			"trucho-river.png",
			"trucho-reservoir.png",
			"trucho-pier.png",
			"trucho-beach.png",
			"roadster-forest.png",
			"roadster-night.png",
			"roadster-desert.png"));

	private static final Map<String, Integer> referenceUncompressedTotalSizes = new LinkedHashMap<>();
	private static final Map<String, Integer> referenceZx0TotalSizes = new LinkedHashMap<>();
	private static final Map<String, Integer> zx0TotalSizes = new LinkedHashMap<>();

	@BeforeAll
	static void beforeAll() {
		referenceUncompressedTotalSizes.clear();
		referenceZx0TotalSizes.clear();
		zx0TotalSizes.clear();
	}

	@ParameterizedTest
	@MethodSource("performanceTestArguments")
	void performanceTest(final String filename, final boolean invert, final List<MsxColor> palette, final ToDoubleFunction<MsxColor> function,
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

		final MsxCharset referenceCharset = MsxCharset.of(chrBytes, clrBytes);

		// When

		final MsxCharset optimizedCharset = new MsxCharsetOptimizer()
				.setColorOrder(palette, function)
				.optimize(referenceCharset);

		// Then

		for (int i = 0; i < referenceUncompressedChrSize; i++) {
			Assertions.assertTrue(
					referenceCharset.get(i).isEquivalentTo(optimizedCharset.get(i)),
					"Expected " + referenceCharset.get(i) + ", but found: " + optimizedCharset.get(i));
		}

		// Performance traces

		final double referenceChrEntropyRatio = 100.0d * ByteArrays.entropyRatio(chrBytes);
		final double referenceClrEntropyRatio = 100.0d * ByteArrays.entropyRatio(clrBytes);
		final double referenceEntropyRatio = (referenceChrEntropyRatio + referenceClrEntropyRatio) / 2;

		final int referenceZx0ChrSize = zx0(chrBytes).length;
		final int referenceZx0ClrSize = zx0(clrBytes).length;
		final int referenceZx0TotalSize = referenceZx0ChrSize + referenceZx0ClrSize;
		final double referenceZx0Ratio = (100.0d * referenceZx0TotalSize) / referenceUncompressedTotalSize;

		final double chrEntropyRatio = 100.0d * ByteArrays.entropyRatio(optimizedCharset.chrtbl());
		final double clrEntropyRatio = 100.0d * ByteArrays.entropyRatio(optimizedCharset.clrtbl());
		final double entropyRatio = (chrEntropyRatio + clrEntropyRatio) / 2;

		final int zx0ChrSize = zx0(optimizedCharset.chrtbl()).length;
		final int zx0ClrSize = zx0(optimizedCharset.clrtbl()).length;
		final int zx0TotalSize = zx0ChrSize + zx0ClrSize;
		final double zx0Ratio = (100.0d * zx0TotalSize) / referenceUncompressedTotalSize;

		final int zx0ChrDelta = zx0ChrSize - referenceZx0ChrSize;
		final int zx0ClrDelta = zx0ClrSize - referenceZx0ClrSize;
		final int zx0Delta = zx0TotalSize - referenceZx0TotalSize;
		final double zx0RatioDelta = zx0Ratio - referenceZx0Ratio;
		final double entropyRatioDelta = entropyRatio - referenceEntropyRatio;

		Logger.info(String.format("""
				%s :: B:%4d \
				-> B:%4d (%4d+%4d)  E:%2d%% (%2d%%,%2d%%)  CR:%2.2f%% \
				-> B:%4d (%4d+%4d) [%+5d (%+5d%+5d)]  E:%2d%% (%2d%%,%2d%%) [%+3d%%]  CR:%2.2f%% [%+3.2f%%] \
				:: %s""",
				filename,
				//
				referenceUncompressedTotalSize,
				referenceZx0TotalSize, referenceZx0ChrSize, referenceZx0ClrSize,
				(int) referenceEntropyRatio, (int) referenceChrEntropyRatio, (int) referenceClrEntropyRatio,
				referenceZx0Ratio,
				//
				zx0TotalSize, zx0ChrSize, zx0ClrSize, zx0Delta, zx0ChrDelta, zx0ClrDelta,
				(int) entropyRatio, (int) chrEntropyRatio, (int) clrEntropyRatio, (int) entropyRatioDelta,
				zx0Ratio, zx0RatioDelta,
				//
				label));

		referenceUncompressedTotalSizes.put(label,
				referenceUncompressedTotalSizes.getOrDefault(label, 0) + referenceUncompressedTotalSize);
		referenceZx0TotalSizes.put(label, referenceZx0TotalSizes.getOrDefault(label, 0) + referenceZx0TotalSize);
		zx0TotalSizes.put(label, zx0TotalSizes.getOrDefault(label, 0) + zx0TotalSize);
	}

	private static Stream<Arguments> performanceTestArguments() {

		final List<Arguments> list = new ArrayList<>();

		for (final String filename : FILENAMES) {

			for (final Pair<String, List<MsxColor>> palette : Arrays.asList(
					Pair.of("none___", (List<MsxColor>) null),
					Pair.of("tms9918", MsxPalettes.TMS9918_PALETTE),
					Pair.of("tms9219", MsxPalettes.TMS9219_PALETTE),
					Pair.of("yazioh_", MsxPalettes.YAZIOH_PALETTE),
					Pair.of("toshiba", MsxPalettes.TOSHIBA_PALETTE),
					Pair.of("v9938__", MsxPalettes.V9938_PALETTE))) {

				for (final Pair<String, ToDoubleFunction<MsxColor>> function : Arrays.asList(
						Pair.of("brighne", (ToDoubleFunction<MsxColor>) MsxColor::brightness),
						Pair.of("percBri", (ToDoubleFunction<MsxColor>) MsxColor::perceivedBrightness),
						Pair.of("relLumi", (ToDoubleFunction<MsxColor>) MsxColor::relativeLuminance))) {

					final String label = String.format("%s-%s", function.getKey(), palette.getKey());
					list.add(Arguments.of(filename, false, palette.getValue(), function.getValue(), label));
					list.add(Arguments.of(filename, true, palette.getValue(), function.getValue(), label + "-i"));
				}
			}
		}

		return list.stream();
	}

	@AfterAll
	static void afterAll() {

		final List<Map.Entry<String, Integer>> list = new ArrayList<>(zx0TotalSizes.entrySet());
		Collections.sort(list, Comparator.<Map.Entry<String, Integer>>comparingInt(Entry::getValue));

		for (final Map.Entry<String, Integer> entry : list) {
			final String label = entry.getKey();

			final int referenceUncompressedTotalSize = referenceUncompressedTotalSizes.get(label);

			final int referenceZx0TotalSize = referenceZx0TotalSizes.get(label);
			final double referenceZx0Ratio = (100.0d * referenceZx0TotalSize) / referenceUncompressedTotalSize;

			final int zx0TotalSize = entry.getValue();
			final int zx0Delta = zx0TotalSize - referenceZx0TotalSize;
			final double zx0Ratio = (100.0d * zx0TotalSize) / referenceUncompressedTotalSize;
			final double zx0RatioDelta = zx0Ratio - referenceZx0Ratio;

			Logger.info(String.format("""
					Total :: B:%4d \
					-> B:%4d  CR:%2.2f%% \
					-> B:%4d [%+5d]  CR:%2.2f%% [%+3.2f%%] \
					:: %s""",
					referenceUncompressedTotalSize,
					referenceZx0TotalSize, referenceZx0Ratio,
					zx0TotalSize, zx0Delta, zx0Ratio, zx0RatioDelta,
					label));
		}
	}

	private static byte[] zx0(final byte[] input) {

		final int skip = 0;
		final boolean backwardsMode = false;
		final boolean classicMode = false;
		final boolean quickMode = false;
		final int threads = 1;
		final boolean verbose = false;
		final int[] delta = { 0 };

		return new zx0.Compressor().compress(
				new zx0.Optimizer().optimize(
						input, skip, quickMode ? zx0.Main.MAX_OFFSET_ZX7 : zx0.Main.MAX_OFFSET_ZX0, threads, verbose),
				input, skip, backwardsMode, !classicMode && !backwardsMode, delta);
	}
}
