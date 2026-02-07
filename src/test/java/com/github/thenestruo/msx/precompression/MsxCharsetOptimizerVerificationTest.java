package com.github.thenestruo.msx.precompression;

import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.thenestruo.commons.io.ClassPathResource;
import com.github.thenestruo.commons.msx.MsxCharset;

public class MsxCharsetOptimizerVerificationTest {

	@ParameterizedTest
	@MethodSource("verificationTestArguments")
	void verificationTest(final String filename) throws IOException {

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

		final MsxCharset referenceCharset = MsxCharset.of(chrBytes, clrBytes);

		// When

		final MsxCharset optimizedCharset = new MsxCharsetOptimizer()
				.optimize(referenceCharset);

		// Then

		for (int i = 0, n = referenceCharset.size(); i < n; i++) {
			Assertions.assertTrue(
					referenceCharset.get(i).isEquivalentTo(optimizedCharset.get(i)),
					"Expected " + referenceCharset.get(i) + ", but found: " + optimizedCharset.get(i));
		}
	}

	private static Stream<Arguments> verificationTestArguments() {

		return Stream.of(
			Arguments.of("ninjasenki.png"),
			Arguments.of("pyramidwarpex.png"),
			Arguments.of("stevedore.png"),
			Arguments.of("youkaiyashiki.png")
		);
	}
}
