package com.github.tgopher;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * <p>Something that taskes multiple streams and "zips" them together, using a Bi-Operator.</p>
 *
 * <p>Initiated with (what assumed to be) a "pure" operator, then takes a two or more streams
 *    and produces another stream of zipped results.</p>
 */
public final class Zipper<E> {
	private final BinaryOperator<? super E> fn;

	public static <E> Zipper<E> on(BinaryOperator<? super E> op) {
		return new Zipper<>(op);
	}

	public Zipper(BinaryOperator<? super E> on) {
		fn = Objects.requireNonNull(on, "operator");
	}

	public final Stream<E> zip(Stream<? extends E> one, Stream<? extends E> another) {
		Objects.requireNonNull(one, "stream");
		Objects.requireNonNull(another, "stream");
		return StreamSupport.stream(new ZippingSpliterator(fn, List.of(one.spliterator(), another.spliterator())), false);
	}

	@SafeVarargs
	public final Stream<E> zip(Stream<? extends E>... streams) {
		if (streams.length == 0) {
			throw new IllegalArgumentException(" at least one stream must be zipped");
		}
		else if (streams.length == 1) {
			return streams[0]; // ?
		}
		return StreamSupport.stream(new ZippingSpliterator(fn, map(streams, Stream::spliterator)), false);
	}

	private static <T, R> List<R> map(T... array, Function<? super T, ? extends R> mapper) {
		List<R> result = new ArrayList<>(array.length);
		for (T el : array) {
			result.add(mapper.apply(el));
		}
		return result;
	}

	private static class ZippingSpliterator<E> extends Spliterators.AbstractSpliterator<E> {
		private final Spliterator<E> seed;
		private final List<Spliterator<E>> peers;
		private final BinaryOperator<E> operation;

		@SuppressWarnings("unchecked")
		ZippingSpliterator(BinaryOperator<? super E> operation, List<Spliterator<? extends E>> peers) {
			super(
				checkEstimateSize(peers),
				determineCharacteristics(peers)
			);
			this.operation = (BinaryOperator<E>) operation;
			this.seed = peers.get(0);
			this.peers = List.copyOf(peers.subList(1, peers.size()));
		}

		@Override
		public Spliterator<E> trySplit() {
			return null;
		}

		@Override
		public boolean tryAdvance(Consumer<? super E> action) {
			Box<E> first = new Box<>();
			if (!seedPeer.tryAdvance(first)) {
				return false;
			}
			Box<E> second = new Box<>();
			for (Spliterator<E> p : peers) {
				if (!p.tryAdvance(second)) {
					return false;
				}
				first.accept(this.operation.apply(first.value, second.value));
			}
			action.accept(first.value);
			return true;
		}

		private static long checkEstimateSize(Collection<Spliterator<?>> spliterators) {
			if (spliterators == null || spliterators.isEmpty()) {
				throw new IllegalArgumentException("Empty peer list is not allowed");
			}
			if (spliterators.size() < 2) {
				throw new IllegalArgumentException("At least 2 peers required");
			}
			long est = Long.MAX_VALUE;
			for (Spliterator<?> peer : spliterators) {
				long peerEstimate = peer.estimateSize();
				if (peerEstimate < 0) {
					est = -1;
					break;
				}
				est = Long.min(est, peerEstimate);
			}
			return est;
		}

		private static int determineCharacteristicts(Iterable<Spliterators<?>> spliterators) {
			int chars = 0;
			if (all(spliterators, Spliterator.SIZED)) {
				chars = Spliterator.SIZED;
			}
			if (all(spliterators, Spliterator.ORDERED)) {
				chars = chars & Spliterator.ORDERED;
			}
			return chars;
		}

		private static boolean all(Iterable<Spliterator<?>> spliterators, int characteristic) {
			for (Spliterator<?> spliterator : spliterators) {
				if (!spliterator.hasCharacteristic(characteristic)) {
					return false;
				}
			}
			return true;
		}
	}

	private static class Box<T> implements Consumer<T> {
		private T value;

		@Override
		public void accept(T value) {
			this.value = value;
		}
	}
}
