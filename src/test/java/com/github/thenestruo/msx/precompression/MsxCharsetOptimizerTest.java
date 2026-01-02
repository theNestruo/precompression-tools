package com.github.thenestruo.msx.precompression;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.lang3.Strings;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.tinylog.Logger;

import com.github.thenestruo.msx.precompression.impl.ColorAndPatternMsxLineOptimizer;
import com.github.thenestruo.msx.precompression.impl.ColorOnlyMsxLineOptimizer;
import com.github.thenestruo.msx.precompression.impl.DefaultOptimizationMerger;
import com.github.thenestruo.msx.precompression.impl.MsxCharsetOptimizerImpl;
import com.github.thenestruo.msx.precompression.impl.NullMsxLineOptimizer;
import com.github.thenestruo.msx.precompression.impl.PatternAndColorMsxLineOptimizer;
import com.github.thenestruo.msx.precompression.impl.PatternOnlyMsxLineOptimizer;
import com.github.thenestruo.msx.precompression.impl.PrioritizeColorOptimizationMerger;
import com.github.thenestruo.msx.precompression.impl.PrioritizePatternOptimizationMerger;
import com.github.thenestruo.msx.precompression.model.MsxCharset;
import com.github.thenestruo.util.ByteArrayUtils;
import com.github.thenestruo.util.ClassPathResource;

public class MsxCharsetOptimizerTest {

	private static final String[] FILENAMES = new String[] {
			"ninjasenki.png",
			"pyramidwarpex.png",
			"stevedore.png",
			"youkaiyashiki.png",
			// "trucho-river.png",
			// "trucho-reservoir.png",
			// "trucho-pier.png",
			// "trucho-beach.png"
			// "roadster-forest.png",
			// "roadster-night.png",
			// "roadster-desert.png",
		};

	private static final MsxLineOptimizer[] PATTERN_OPTIMIZERS = new MsxLineOptimizer[] {
			NullMsxLineOptimizer.INSTANCE,
			PatternOnlyMsxLineOptimizer.INSTANCE,
			PatternAndColorMsxLineOptimizer.INSTANCE };

	private static final MsxLineOptimizer[] COLOR_OPTIMIZERS = new MsxLineOptimizer[] {
			NullMsxLineOptimizer.INSTANCE,
			ColorOnlyMsxLineOptimizer.INSTANCE,
			ColorAndPatternMsxLineOptimizer.INSTANCE };

	private static final OptimizationMerger[] OPTIMIZATION_MERGERS = new OptimizationMerger[] {
			DefaultOptimizationMerger.INSTANCE,
			PrioritizePatternOptimizationMerger.INSTANCE,
			PrioritizeColorOptimizationMerger.INSTANCE };

	//

	private static final Map<String, Integer> referenceUncompressedTotalSizes = new LinkedHashMap<>();
	private static final Map<String, Integer> referenceOptimizedZx0TotalSizes = new LinkedHashMap<>();
	private static final Map<String, Integer> referenceZx0TotalSizes = new LinkedHashMap<>();
	private static final Map<String, Integer> zx0TotalSizes = new LinkedHashMap<>();

	@BeforeAll
	static void beforeAll() {
		referenceUncompressedTotalSizes.clear();
		referenceOptimizedZx0TotalSizes.clear();
		referenceZx0TotalSizes.clear();
		zx0TotalSizes.clear();
	}

	@ParameterizedTest
	@MethodSource("optimizationTestArguments")
	void optimizationTest(final String filename, final String label,
			final MsxLineOptimizer patternOptimizer, final MsxLineOptimizer colorOptimizer, final OptimizationMerger merger)
			throws IOException {

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

		final MsxCharset referenceCharset = new MsxCharset(chrBytes, clrBytes);

		final byte[] oChrBytes;
		final byte[] oClrBytes;
		try (
				final InputStream chrInputStream = new ClassPathResource(filename + ".chr-o").getInputStream();
				final InputStream clrInputStream = new ClassPathResource(filename + ".clr-o").getInputStream()) {
			oChrBytes = chrInputStream.readAllBytes();
			oClrBytes = clrInputStream.readAllBytes();
		}
		Assumptions.assumeTrue(oChrBytes.length == oClrBytes.length);
		final int referenceOptimizedZx0ChrSize = zx0(oChrBytes).length;
		final int referenceOptimizedZx0ClrSize = zx0(oClrBytes).length;
		final int referenceOptimizedZx0TotalSize = referenceOptimizedZx0ChrSize + referenceOptimizedZx0ClrSize;

		// When

		final MsxCharset optimizedCharset = new MsxCharsetOptimizerImpl()
				.setPatternOptimizer(patternOptimizer)
				.setColorOptimizer(colorOptimizer)
				.setMerger(merger)
				.optimize(referenceCharset);

		// Then

		for (int i = 0; i < referenceUncompressedChrSize; i++) {
			Assertions.assertTrue(
					referenceCharset.get(i).isEquivalentTo(optimizedCharset.get(i)),
					"Expected " + referenceCharset.get(i) + ", but found: " + optimizedCharset.get(i));
		}

		// Performance traces

		final double referenceChrEntropyRatio = 100.0d * ByteArrayUtils.entropyRatio(chrBytes);
		final double referenceClrEntropyRatio = 100.0d * ByteArrayUtils.entropyRatio(clrBytes);
		final double referenceEntropyRatio = (referenceChrEntropyRatio + referenceClrEntropyRatio) / 2;

		final int referenceZx0ChrSize = zx0(chrBytes).length;
		final int referenceZx0ClrSize = zx0(clrBytes).length;
		final int referenceZx0TotalSize = referenceZx0ChrSize + referenceZx0ClrSize;
		final double referenceZx0Ratio = (100.0d * referenceZx0TotalSize) / referenceUncompressedTotalSize;

		final double chrEntropyRatio = 100.0d * ByteArrayUtils.entropyRatio(optimizedCharset.chrtbl());
		final double clrEntropyRatio = 100.0d * ByteArrayUtils.entropyRatio(optimizedCharset.clrtbl());
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

		Logger.info(String.format("%s :: B:%4d "
				+ "-> B:%4d (%4d+%4d)  E:%2d%% (%2d%%,%2d%%)  CR:%2.2f%% "
				+ "(-> B:%4d (%4d+%4d)) "
				+ "-> B:%4d (%4d+%4d) [%+5d (%+5d%+5d)]  E:%2d%% (%2d%%,%2d%%) [%+3d%%]  CR:%2.2f%% [%+3.2f%%] "
				+ ":: %s",
				filename,
				//
				referenceUncompressedTotalSize,
				referenceZx0TotalSize, referenceZx0ChrSize, referenceZx0ClrSize,
				(int) referenceEntropyRatio, (int) referenceChrEntropyRatio, (int) referenceClrEntropyRatio,
				referenceZx0Ratio,
				//
				referenceOptimizedZx0TotalSize, referenceOptimizedZx0ChrSize, referenceOptimizedZx0ClrSize,
				//
				zx0TotalSize, zx0ChrSize, zx0ClrSize, zx0Delta, zx0ChrDelta, zx0ClrDelta,
				(int) entropyRatio, (int) chrEntropyRatio, (int) clrEntropyRatio, (int) entropyRatioDelta,
				zx0Ratio, zx0RatioDelta,
				//
				label));

		referenceUncompressedTotalSizes.put(label, referenceUncompressedTotalSizes.getOrDefault(label, 0) + referenceUncompressedTotalSize);
		referenceOptimizedZx0TotalSizes.put(label, referenceOptimizedZx0TotalSizes.getOrDefault(label, 0) + referenceOptimizedZx0TotalSize);
		referenceZx0TotalSizes.put(label, referenceZx0TotalSizes.getOrDefault(label, 0) + referenceZx0TotalSize);
		zx0TotalSizes.put(label, zx0TotalSizes.getOrDefault(label, 0) + zx0TotalSize);
	}

	@AfterAll
	static void afterAll() {

		final List<Map.Entry<String, Integer>> list = new ArrayList<>(zx0TotalSizes.entrySet());
		Collections.sort(list, Comparator.<Map.Entry<String, Integer>> comparingInt(e -> e.getValue()));

		for(final Map.Entry<String, Integer> entry : list) {
			final String label = entry.getKey();

			final int referenceUncompressedTotalSize = referenceUncompressedTotalSizes.get(label);

			final int referenceZx0TotalSize = referenceZx0TotalSizes.get(label);
			final double referenceZx0Ratio = (100.0d * referenceZx0TotalSize) / referenceUncompressedTotalSize;

			final int referenceOptimizedZx0TotalSize = referenceOptimizedZx0TotalSizes.get(label);
			final double referenceOptimizedZx0Ratio = (100.0d * referenceOptimizedZx0TotalSize) / referenceUncompressedTotalSize;

			final int zx0TotalSize = entry.getValue();
			final int zx0Delta = zx0TotalSize - referenceZx0TotalSize;
			final double zx0Ratio = (100.0d * zx0TotalSize) / referenceUncompressedTotalSize;
			final double zx0RatioDelta = zx0Ratio - referenceZx0Ratio;

			Logger.info(String.format("Total :: B:%4d "
				+ "-> B:%4d  CR:%2.2f%% "
				+ "-> B-o:%4d [%+5d]  CR-o:%2.2f%% [%+3.2f%%] "
				+ "-> B:%4d [%+5d]  CR:%2.2f%% [%+3.2f%%] "
				+ ":: %s",
				referenceUncompressedTotalSize,
				referenceZx0TotalSize, referenceZx0Ratio,
				referenceOptimizedZx0TotalSize, referenceOptimizedZx0TotalSize - referenceZx0TotalSize,
				referenceOptimizedZx0Ratio, referenceOptimizedZx0Ratio - referenceZx0Ratio,
				zx0TotalSize, zx0Delta, zx0Ratio, zx0RatioDelta,
				label));
		}
	}

	private static Stream<Arguments> optimizationTestArguments() {

		final List<Arguments> list = new ArrayList<>();

		for (final String filename : FILENAMES) {
			for (final MsxLineOptimizer patternOptimizer : PATTERN_OPTIMIZERS) {
				for (final MsxLineOptimizer colorOptimizer : COLOR_OPTIMIZERS) {
					for (final OptimizationMerger merger : OPTIMIZATION_MERGERS) {

						final String label = String.format(
								"%s + %s (%s)",
								Strings.CS.removeEnd(patternOptimizer.getClass().getSimpleName(), "MsxLineOptimizer"),
								Strings.CS.removeEnd(colorOptimizer.getClass().getSimpleName(), "MsxLineOptimizer"),
								Strings.CS.removeEnd(merger.getClass().getSimpleName(), "OptimizationMerger"));

						list.add(Arguments.of(filename, label, patternOptimizer, colorOptimizer, merger));
					}
				}
			}
		}

		return list.stream();
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
