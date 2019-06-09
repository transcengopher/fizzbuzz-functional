package com.github.tgopher;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Utility methods.
 */
public final class Util {
	/**
	 * Infinitely repeat passed in elements.
	 */
	@SafeVarargs
	public static <E> Stream repeat(E... elements) {
		return Stream.generate(() -> elements).flatMap(Arrays::stream);
	}

	private Util() { throw new AssertionError("no instances"); }
}
