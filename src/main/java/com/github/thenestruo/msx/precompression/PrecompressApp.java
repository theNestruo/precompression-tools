package com.github.thenestruo.msx.precompression;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Callable;

import org.tinylog.Logger;
import org.tinylog.configuration.Configuration;

import com.github.thenestruo.commons.io.Paths;
import com.github.thenestruo.commons.math.Range;
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

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "precompress", sortOptions = false)
public class PrecompressApp implements Callable<Integer> {

	public static void main(final String... args) {
		System.exit(new CommandLine(new PrecompressApp()).execute(args));
	}

	@Option(names = { "-h", "--help" }, usageHelp = true, description = "shows usage")
	private boolean help;

	@Option(names = { "-v", "--verbose" }, description = "verbose execution")
	private boolean verbose;

	@Parameters(index = "0", arity = "1", paramLabel = "chrtbl", description = "binary input file(s): CHRTBL")
	private Path chrtblInputPath;

	@Parameters(index = "1", arity = "0..1", paramLabel = "clrtbl", description = "binary input file(s): CLRTBL")
	private Path clrtblInputPath;

	@Option(names = { "-e", "--exclude" },
			converter = ExclusionTypeConverter.class,
			description = "Excluded range of addresses: <from>..<to>")
	private Range<Integer> exclusionRange;

	private static class ExclusionTypeConverter implements ITypeConverter<Range<Integer>> {

		@Override
		public Range<Integer> convert(final String value) throws Exception {

			final String[] values = value.split("\\.{2}");
			if (values.length != 2) {
				return null;
			}
			try {
				final int from = Integer.decode(values[0]);
				final int to = Integer.decode(values[1]);
				return new Range<>(from, to);

			} catch (final NumberFormatException e) {
				return null;
			}
		}
	}

	@Option(names = { "-p", "--pattern" },
			converter = PatternMsxLineOptimizerTypeConverter.class,
			description = "Pattern optimizations: no (default), pattern, patternAndColor",
			defaultValue = "no")
	private MsxLineOptimizer patternOptimizer;

	private static class PatternMsxLineOptimizerTypeConverter implements ITypeConverter<MsxLineOptimizer> {

		@Override
		public MsxLineOptimizer convert(final String value) throws Exception {
			switch (value) {
			case "pattern":
				return PatternOnlyMsxLineOptimizer.INSTANCE;
			case "patternAndColor":
				return PatternAndColorMsxLineOptimizer.INSTANCE;
			case "no":
			default:
				return NullMsxLineOptimizer.INSTANCE;
			}
		}
	}

	@Option(names = { "-c", "--color" },
			converter = ColorMsxLineOptimizerTypeConverter.class,
			description = "Color optimizations: no, color, colorAndPattern (default)",
			defaultValue = "colorAndPattern")
	private MsxLineOptimizer colorOptimizer;

	private static class ColorMsxLineOptimizerTypeConverter implements ITypeConverter<MsxLineOptimizer> {

		@Override
		public MsxLineOptimizer convert(final String value) throws Exception {
			switch (value) {
			case "color":
				return ColorOnlyMsxLineOptimizer.INSTANCE;
			case "colorAndPattern":
				return ColorAndPatternMsxLineOptimizer.INSTANCE;
			case "no":
			default:
				return NullMsxLineOptimizer.INSTANCE;
			}
		}
	}

	@Option(names = { "-m", "--merger" },
			converter = OptimizationMergerTypeConverter.class,
			description = "Merge optimizations: prioritizePattern, prioritizeColor, default (default)",
			defaultValue = "default")
	private OptimizationMerger optimizationMerger;

	private static class OptimizationMergerTypeConverter implements ITypeConverter<OptimizationMerger> {

		@Override
		public OptimizationMerger convert(final String value) throws Exception {
			switch (value) {
			case "prioritizePattern":
				return PrioritizePatternOptimizationMerger.INSTANCE;
			case "prioritizeColor":
				return PrioritizeColorOptimizationMerger.INSTANCE;
			case "default":
			default:
				return DefaultOptimizationMerger.INSTANCE;
			}
		}
	}

	@Override
	public Integer call() throws IOException {

		// (before using tinylog)
		this.handleVerbose();

		// Reads the binary files
		final byte[] chrtblBytes = this.readBinary(this.chrtblInputPath);
		if (chrtblBytes == null) {
			return 10;
		}

		final byte[] clrtblBytes = this.readBinary(this.clrtblInputPath());
		if (clrtblBytes == null) {
			return 20;
		}

		final long chrSize = chrtblBytes.length;
		final long clrSize = clrtblBytes.length;
		Logger.debug("Binary files read: {} bytes, {} bytes", chrSize, clrSize);
		if ((chrSize == 0) || (clrSize == 0) || (chrSize != clrSize)) {
			return 30;
		}

		final MsxCharset optimizedCharset = new MsxCharsetOptimizerImpl().setPatternOptimizer(this.patternOptimizer)
				.setColorOptimizer(this.colorOptimizer).setMerger(this.optimizationMerger)
				.setExclusion(this.exclusionRange).optimize(new MsxCharset(chrtblBytes, clrtblBytes));

		// Writes the optimized file
		Logger.debug("Binary files to be written: {}, {}", this.chrtblOutputPath(), this.clrtblOutputPath());
		Files.write(this.chrtblOutputPath(), optimizedCharset.chrtbl(), StandardOpenOption.CREATE);
		Files.write(this.clrtblOutputPath(), optimizedCharset.clrtbl(), StandardOpenOption.CREATE);
		Logger.debug("Binary files {}, {} written", this.chrtblOutputPath(), this.clrtblOutputPath());

		return 0;
	}

	private void handleVerbose() {

		if (this.verbose) {
			Configuration.set("writer.level", "debug");
		}
	}

	private Path clrtblInputPath() {

		if (this.clrtblInputPath != null) {
			return this.clrtblInputPath;
		}

		if (Paths.endsWith(this.chrtblInputPath, ".chr")) {
			return Paths.append(Paths.removeEnd(this.chrtblInputPath, ".chr"), ".clr");
		}

		return null;
	}

	private Path chrtblOutputPath() {

		return this.chrtblInputPath.resolveSibling(String.format("%s.opt", this.chrtblInputPath.getFileName()));
	}

	private Path clrtblOutputPath() {

		return this.clrtblInputPath().resolveSibling(String.format("%s.opt", this.clrtblInputPath().getFileName()));
	}

	private byte[] readBinary(final Path path) throws IOException {

		// (sanity check)
		if (path == null) {
			return null;
		}

		if (!Files.exists(path)) {
			Logger.warn("Binary input file {} does not exist", path);
			return null;
		}

		Logger.debug("Binary input file {} will be read", path);
		return Files.readAllBytes(path);
	}
}
