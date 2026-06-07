package com.github.thenestruo.msx.precompression;

import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.tinylog.Logger;

import com.github.thenestruo.commons.io.ClassPathResource;
import com.github.thenestruo.commons.msx.MsxCharset;
import com.github.thenestruo.commons.msx.MsxLine;

public class ScratchpadTest {

	private static int referenceChrZx0Size;
	private static int referenceClrZx0Size;
	private static int referenceZx0Size;

	private static MsxCharset referenceCharset;

	@BeforeAll
	static void beforeAll() throws IOException {
		// Configuration.set("writer.level", "trace");
	}

	private static Stream<Arguments> testArguments() {

		return Stream.of(
			Arguments.of("ninjasenki.png"),						// delta:  +0
			Arguments.of("pyramidwarpex.png"),					// delta:  +0
			Arguments.of("stevedore.png"),						// delta:  +0
			Arguments.of("youkaiyashiki.png") 					// delta:  +0
		);
	}

	@ParameterizedTest
	@MethodSource("testArguments")
	void test10(final String filename) throws IOException {

		testPerformanceOf(new MsxCharsetOptimizer(), filename, "default");
	}

	@ParameterizedTest
	@MethodSource("testArguments")
	void test20(final String filename) throws IOException {

		testPerformanceOf(new MsxCharsetOptimizer() {
			@Override
			protected MsxLine singleColorOfNotStrippedImage(final MsxLine candidate) {

				// Single color
				final byte singleColor = candidate.singleColor();

				return this.isSingleColorBackground(singleColor)
						? this.singleColorBackground(candidate, singleColor)
						: this.singleColorForeground(candidate, singleColor);
			}
		}, filename, "single color ignore previous");
	}

	@ParameterizedTest
	@MethodSource("testArguments")
	void test30(final String filename) throws IOException {

		testPerformanceOf(new MsxCharsetOptimizer() {

			@Override
			protected boolean isSingleColorBackground(byte singleColor) {
				return false;
			}
		}, filename, "single color always foreground");
	}

	@ParameterizedTest
	@MethodSource("testArguments")
	void test35(final String filename) throws IOException {

		testPerformanceOf(new MsxCharsetOptimizer() {

			@Override
			protected boolean isSingleColorBackground(byte singleColor) {
				return false;
			}

			@Override
			protected MsxLine singleColorForeground(final MsxLine candidate, final byte singleColor) {

				final MsxLine optimized = MsxLine.foregroundOf(singleColor, singleColor);
				this.debug(candidate, optimized, "Full foreground (over preferred background)");
				return optimized;
			}
		}, filename, "single color always foreground same nibble");
	}

	@ParameterizedTest
	@MethodSource("testArguments")
	void test37(final String filename) throws IOException {

		testPerformanceOf(new MsxCharsetOptimizer() {

			@Override
			protected boolean isSingleColorBackground(byte singleColor) {
				return false;
			}

			@Override
			protected MsxLine singleColorForeground(final MsxLine candidate, final byte singleColor) {

				final MsxLine optimized = MsxLine.foregroundOf(singleColor, (byte) 0x00);
				this.debug(candidate, optimized, "Full foreground (over preferred background)");
				return optimized;
			}
		}, filename, "single color always foreground bg-00");
	}

	@ParameterizedTest
	@MethodSource("testArguments")
	void test40(final String filename) throws IOException {

		testPerformanceOf(new MsxCharsetOptimizer() {

			@Override
			protected boolean isSingleColorBackground(byte singleColor) {
				return true;
			}
		}, filename, "single color always background");
	}

	@ParameterizedTest
	@MethodSource("testArguments")
	void test45(final String filename) throws IOException {

		testPerformanceOf(new MsxCharsetOptimizer() {

			@Override
			protected boolean isSingleColorBackground(byte singleColor) {
				return true;
			}

			@Override
			protected MsxLine singleColorBackground(final MsxLine candidate, final byte singleColor) {

				final MsxLine optimized = MsxLine.backgroundOf(singleColor, singleColor);
				this.debug(candidate, optimized, "Full background");
				return optimized;
			}

		}, filename, "single color always background same nibble");
	}

	@ParameterizedTest
	@MethodSource("testArguments")
	void test50(final String filename) throws IOException {

		testPerformanceOf(new MsxCharsetOptimizer() {

			@Override
			protected MsxLine singleColorBackground(final MsxLine candidate, final byte singleColor) {

				final MsxLine optimized = MsxLine.of(this.previousValue.chrtblByte(), singleColor, singleColor);
				this.debug(candidate, optimized, "Full background");
				return optimized;
			}

			@Override
			protected MsxLine singleColorForeground(final MsxLine candidate, final byte singleColor) {

				final MsxLine optimized = MsxLine.of(this.previousValue.chrtblByte(), singleColor, singleColor);
				this.debug(candidate, optimized, "Full foreground (over preferred background)");
				return optimized;
			}
		}, filename, "single color reuse CHRTBL same nibble");
	}


	private static void testPerformanceOf(
		final MsxCharsetOptimizer msxCharsetOptimizer, final String filename, final String label)
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

		referenceCharset = MsxCharset.of(chrBytes, clrBytes);

		referenceChrZx0Size = zx0(referenceCharset.chrtbl()).length;
		referenceClrZx0Size = zx0(referenceCharset.clrtbl()).length;
		referenceZx0Size = referenceChrZx0Size + referenceClrZx0Size;

		// When

		final MsxCharset optimizedCharset = msxCharsetOptimizer.optimize(referenceCharset);

		// Then

		final int chrZx0Size = zx0(optimizedCharset.chrtbl()).length;
		final int clrZx0Size = zx0(optimizedCharset.clrtbl()).length;
		final int zx0Size = chrZx0Size + clrZx0Size;

		final int chrZx0Delta = chrZx0Size - referenceChrZx0Size;
		final int clrZx0Delta = clrZx0Size - referenceClrZx0Size;
		final int zx0Delta = zx0Size - referenceZx0Size;

		Logger.info(String.format("ref %4d (%4d+%4d) :: opt %4d (%4d+%4d) :: delta %+5d (%+5d%+5d) :: %s :: %s",
				referenceZx0Size, referenceChrZx0Size, referenceClrZx0Size,
				zx0Size, chrZx0Size, clrZx0Size,
				zx0Delta, chrZx0Delta, clrZx0Delta,
				label, filename
			));
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
