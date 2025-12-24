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
import org.apache.commons.lang3.tuple.Pair;
import org.tinylog.Logger;

import com.github.thenestruo.msx.precompression.support.MsxCharset;
import com.github.thenestruo.msx.precompression.support.RleOptimizer;

public class PrecompressApp {

    private static final String HELP = "help";

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

        // Main options
        if (showUsage(command, options)) {
            return;
        }

        // Reads the binary files
        final Pair<Path, byte[]> chrPair = readBinary(command);
        final Path chrInputPath = chrPair.getLeft();
        if (chrInputPath == null) {
            return;
        }

        final Pair<Path, byte[]> clrPair = readBinary(command);
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

        new RleOptimizer(charset).optimizeCharset();

        // Writes the optimized file
        final Path chrOutputPath = chrInputPath.resolveSibling(String.format("%s.opt", chrInputPath.getFileName()));
        final Path clrOutputPath = clrInputPath.resolveSibling(String.format("%s.opt", clrInputPath.getFileName()));
        Logger.debug("Binary files to be written: {}, {}", chrOutputPath, clrOutputPath);
        Files.write(chrOutputPath, charset.getChrtbl(), StandardOpenOption.CREATE);
        Files.write(clrOutputPath, charset.getClrtbl(), StandardOpenOption.CREATE);
        Logger.debug("Binary files {}, {} written", chrOutputPath, clrOutputPath);
    }

    private static Options options() {

        final Options options = new Options();
        options.addOption(HELP, "Shows usage");
        return options;
    }

    private static boolean showUsage(final CommandLine command, final Options options) throws IOException  {

        return command.hasOption(HELP) && showUsage(options);
    }

    private static boolean showUsage(final Options options) throws IOException {

        // (prints in proper order)
        HelpFormatter.builder()
                .setShowSince(false)
                .get().printHelp(
                        "java -jar precompression.jar <input1> <input2>",
                        "with:"
                                + "\n<input1>  Binary input file (CHRTBL)"
                                + "\n<input2>  Binary input file (CLRTBL)",
                        options.getOptions(),
                        null,
                        true);

        return true;
    }

    private static Pair<Path, byte[]> readBinary(final CommandLine command) throws IOException {

        final String path = nextPath(command, null);
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
}
