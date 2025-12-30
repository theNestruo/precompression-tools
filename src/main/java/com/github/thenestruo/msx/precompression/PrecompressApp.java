package com.github.thenestruo.msx.precompression;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.help.HelpFormatter;
import org.apache.commons.io.file.PathUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.lang3.tuple.Pair;
import org.tinylog.Logger;
import org.tinylog.configuration.Configuration;

import com.github.thenestruo.msx.precompression.impl.MsxCharsetOptimizerImpl;
import com.github.thenestruo.msx.precompression.model.MsxCharset;

public class PrecompressApp {

	private static final String HELP = "help";
	private static final String VERBOSE = "verbose";
	private static final String PATTERN_NO = "no-pattern";
	private static final String PATTERN_YES = "pattern";
	private static final String PATTERN_AND_COLOR = "pattern-and-color";
	private static final String COLOR_NO = "no-color";
	private static final String COLOR_YES = "color";
	private static final String COLOR_AND_PATTERN = "color-and-pattern";
	private static final String PRIORITIZE_BALANCED = "no-prioritize";
	private static final String PRIORITIZE_PATTERN = "prioritize-pattern";
	private static final String PRIORITIZE_COLOR = "prioritize-color";
	private static final String EXCLUDE = "exclude";

	public static void main(final String[] args) throws ParseException, IOException {

		// Parses the command line
		final Options options = options();
		final CommandLine command;
		try {
			command = new DefaultParser().parse(options, args);
		} catch (final MissingOptionException e) {
			showUsage(options);
			return;
		}

		// (before using tinylog)
		setVerbose(command);

		// Main options
		if (showUsage(command, options)) {
			return;
		}

		// Reads the binary files
		final Pair<Path, byte[]> chrPair = readChrtblBinary(command);
		final Path chrInputPath = chrPair.getLeft();
		if (chrInputPath == null) {
			return;
		}

		final Pair<Path, byte[]> clrPair = readClrtblBinary(command, chrInputPath);
		final Path clrInputPath = clrPair.getLeft();
		if (clrInputPath == null) {
			return;
		}

		final long chrSize = Files.size(chrInputPath);
		final long clrSize = Files.size(clrInputPath);
		Logger.debug("Binary files read: {} bytes, {} bytes", chrSize, clrSize);
		if ((chrSize == 0) || (clrSize == 0) || (chrSize != clrSize)) {
			return;
		}

		final MsxCharset charset = new MsxCharset(chrPair.getRight(), clrPair.getRight());

		configureRleOptimizer(new MsxCharsetOptimizerImpl(), command)
				.optimize(charset);

		// Writes the optimized file
		final Path chrOutputPath = chrInputPath.resolveSibling(String.format("%s.opt", chrInputPath.getFileName()));
		final Path clrOutputPath = clrInputPath.resolveSibling(String.format("%s.opt", clrInputPath.getFileName()));
		Logger.debug("Binary files to be written: {}, {}", chrOutputPath, clrOutputPath);

		Files.write(chrOutputPath, charset.chrtbl(), StandardOpenOption.CREATE);
		Files.write(clrOutputPath, charset.clrtbl(), StandardOpenOption.CREATE);
		Logger.debug("Binary files {}, {} written", chrOutputPath, clrOutputPath);
	}

	private static Options options() {

		final Options options = new Options();
		options.addOption(HELP, "Shows usage");
		options.addOption(VERBOSE, "Verbose execution");
		options.addOption(EXCLUDE, true, "Excluded range of addresses: <from>..<to>");
		options.addOption(PATTERN_NO, "Do not use pattern optimizations (default)");
		options.addOption(PATTERN_YES, "Use basic pattern optimizations");
		options.addOption(PATTERN_AND_COLOR, "Use pattern optimizations with color changes");
		options.addOption(COLOR_NO, "Do not use color optimizations");
		options.addOption(COLOR_YES, "Use basic color optimizations");
		options.addOption(COLOR_AND_PATTERN, "Use color optimizations with pattern changes (default)");
		options.addOption(PRIORITIZE_BALANCED, "Merge optimizations without prioritization");
		options.addOption(PRIORITIZE_PATTERN, "Prioritize pattern optimizations");
		options.addOption(PRIORITIZE_COLOR, "Prioritize color optimizations (default)");
		return options;
	}

	private static boolean showUsage(final CommandLine command, final Options options) throws IOException {

		return command.hasOption(HELP) && showUsage(options);
	}

	private static boolean showUsage(final Options options) throws IOException {

		// (prints in proper order)
		HelpFormatter.builder().setShowSince(false).get().printHelp(
				"java -jar precompression.jar <input1> <input2>",
				"with:"
					+ "\n<input1>  Binary input file (CHRTBL)"
					+ "\n<input2>  Binary input file (CLRTBL)",
				options.getOptions(),
				null,
				true);

		return true;
	}

	private static boolean setVerbose(final CommandLine command) {

		if (!command.hasOption(VERBOSE)) {
			return false;
		}

		Configuration.set("writer.level", "debug");
		return true;
	}

	private static Pair<Path, byte[]> readChrtblBinary(final CommandLine command) throws IOException {

		return readBinary(command, null);
	}

	private static Pair<Path, byte[]> readClrtblBinary(final CommandLine command, final Path chrtblPath)
			throws IOException {

		final String defaultClrtblPathValue = Strings.CI.equals(PathUtils.getExtension(chrtblPath), "chr")
				? String.format("%s.clr", PathUtils.getBaseName(chrtblPath))
				: null;
		return readBinary(command, defaultClrtblPathValue);
	}

	private static Pair<Path, byte[]> readBinary(final CommandLine command, final String defaultNextPathValue)
			throws IOException {

		final String path = nextPath(command, defaultNextPathValue);
		if (path == null) {
			return Pair.of(null, null);
		}
		final Path file = Path.of(path);
		if (!Files.exists(file)) {
			Logger.warn("Binary input file {} does not exist", file.toAbsolutePath());
			return Pair.of(null, null);
		}

		Logger.debug("Binary input file {} will be read", file.toAbsolutePath());
		return Pair.of(file, Files.readAllBytes(file));
	}

	private static String nextPath(final CommandLine command, final String defaultValue) {

		final List<String> argList = command.getArgList();
		return argList.isEmpty() ? defaultValue : argList.remove(0);
	}

	private static MsxCharsetOptimizer configureRleOptimizer(final MsxCharsetOptimizer rleOptimizer, final CommandLine command) {

		if (!command.hasOption(EXCLUDE)) {
			return rleOptimizer;
		}

		final String optionValue = command.getOptionValue(EXCLUDE);
		final String[] values = StringUtils.splitByWholeSeparator(optionValue, "..");
		if (values.length != 2) {
			return rleOptimizer;
		}
		try {
			final int from = Integer.decode(values[0]);
			final int to = Integer.decode(values[1]);
			if ((from < 0) || (to < from)) {
				return rleOptimizer;
			}

			return rleOptimizer.withExclusion(from, to);

		} catch (final NumberFormatException e) {
			return rleOptimizer;
		}
	}
}
