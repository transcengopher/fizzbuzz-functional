package com.github.tgopher;

import static com.github.tgopher.Util.repeat;

import java.util.stream.LongStream;
import java.util.stream.Stream;

public class Main {
	private static final long DEFAULT_LENGTH = 100;

	private long howMany(String[] args) {
		if (args.length == 0) {
			return DEFAULT_LENGTH;
		}
		String lengthArg = args[0];
		if (lengthArg.isBlank()) {
			return DEFAULT_LENGTH;
		}
		return Long.parseLong(lengthArg);
	}

	public static void main(String[] args) {
		
		Zipper<String> concat = Zipper.on(String::concat);
		Zipper<String> chooseNonEmpty = Zipper.on((s1, s2) -> s1.isEmpty() ? s2 : s1);

		Stream<String> words = concat.zip(
			Stream.generate(() -> ""),
			repeat("", "", "fizz"),
			repeat("", "", "", "", "buzz")
		);

		Stream<String> numbers = LongStream.iterate(1, i -> i + 1).mapToObj(Long::toString);

		Stream<String> fizzBuzz = chooseNonEmpty.zip(words, numbers);

		fizzBuzz.limit(howMany(args)).forEachOrdered(System.out.println);
	}
}
