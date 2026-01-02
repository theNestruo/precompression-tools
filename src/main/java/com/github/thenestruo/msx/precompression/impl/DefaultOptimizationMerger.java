package com.github.thenestruo.msx.precompression.impl;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;

import com.github.thenestruo.msx.precompression.OptimizationMerger;
import com.github.thenestruo.msx.precompression.model.Optimization;

public class DefaultOptimizationMerger implements OptimizationMerger {

	/**
	 * Comparator to choose amongst overlapped, non-mergeable optimizations:
	 * prefers the larger optimization, or the one that covers more entropy
	 * if both have the same size
	 */
	public static final Comparator<Optimization> SIZE_THEN_ENTROPY =
			Comparator.comparingInt(Optimization::size)
				.thenComparing(Comparator.comparingInt(Optimization::entropy));

	/**
	 * Comparator to choose amongst overlapped, non-mergeable optimizations:
	 * prefers the optimization that covers more entropy, or the larger optimization
	 * if both have the same size
	 */
	public static final Comparator<Optimization> ENTROPY_THEN_SIZE =
			Comparator.comparingInt(Optimization::entropy)
				.thenComparing(Comparator.comparingInt(Optimization::size));

	/** The sensible default for the comparator to choose amongst overlapped, non-mergeable optimizations */
	private static final Comparator<Optimization> DEFAULT_NON_MERGEABLE_COMPARATOR =
			ENTROPY_THEN_SIZE;

	/**
	 * Default configuration instance,
	 * with the default comparator to choose amongst overlapped, non-mergeable optimizations
	 */
	public static final DefaultOptimizationMerger INSTANCE = new DefaultOptimizationMerger(null);

	//

	/** The comparator to choose amongst overlapped, non-mergeable optimizations */
	private Comparator<Optimization> nonMergeableComparator = DEFAULT_NON_MERGEABLE_COMPARATOR;

	public DefaultOptimizationMerger(
			final Comparator<Optimization> nonMergeableComparator) {
		super();

		this.nonMergeableComparator =
				Objects.requireNonNullElse(nonMergeableComparator, DEFAULT_NON_MERGEABLE_COMPARATOR);
	}

	@Override
	public List<Optimization> merge(
			final List<Optimization> forwardList, final List<Optimization> backwardsList) {

		// (sanity checks)
		if (CollectionUtils.isEmpty(forwardList)) {
			return CollectionUtils.isEmpty(backwardsList)
					? Collections.emptyList()
					: backwardsList;
		}
		if (CollectionUtils.isEmpty(backwardsList)) {
			return forwardList;
		}

		final List<Optimization> mergedList = new ArrayList<>();

		for (final Deque<Optimization> queueA = new ArrayDeque<>(forwardList),
				queueB = new ArrayDeque<>(backwardsList);
				!queueA.isEmpty() || !queueB.isEmpty(); ) {

			// (discards already covered optimizations)
			this.consumeAlreadyCoveredBy(queueA, mergedList);
			this.consumeAlreadyCoveredBy(queueB, mergedList);

			// (one list exhausted: keeps the remaining items from the other list)
			if (queueA.isEmpty()) {
				mergedList.addAll(queueB);
				break;
			}
			if (queueB.isEmpty()) {
				mergedList.addAll(queueA);
				break;
			}

			final Optimization a = queueA.peek();
			final Optimization b = queueB.peek();

			// (non-overlapped: keeps the optimization that comes first)
			if (a.isBeforeRange(b)) {
				mergedList.add(queueA.poll()); // (keeps)
				continue;
			}
			if (b.isBeforeRange(a)) {
				mergedList.add(queueB.poll()); // (keeps)
				continue;
			}

			// (overlapped, contained: discards the smaller, contained optimization)
			if (a.containsRange(b)) {
				queueB.poll(); // (discards)
				continue;
			}
			if (b.containsRange(a)) {
				queueA.poll(); // (discards)
				continue;
			}

			// (overlapped, mergeable: merges and replaces the pre-merge optimization)
			if ((a.getMinimum() <= b.getMinimum()) && a.isMergeableWith(b)) {
				final Optimization mergedItem = a.mergeWith(b);
				while (!queueA.isEmpty() && mergedItem.containsRange(queueA.peek())) {
					queueA.poll(); // (discards...)
				}
				queueA.addFirst(mergedItem); // (...but replaces with merged)
				continue;
			}
			if ((b.getMinimum() <= a.getMinimum()) && b.isMergeableWith(a)) {
				final Optimization mergedItem = b.mergeWith(a);
				while (!queueB.isEmpty() && mergedItem.containsRange(queueB.peek())) {
					queueB.poll(); // (discards...)
				}
				queueB.addFirst(mergedItem); // (...but replaces with merged)
				continue;
			}

			// (overlapped, non-mergeable)
			if (this.nonMergeableComparator.compare(a, b) >= 0) {
				mergedList.add(queueA.poll()); // (keeps)
				queueB.poll(); // (discards)
			} else {
				mergedList.add(queueB.poll()); // (keeps)
				queueA.poll(); // (discards)
			}
		}

		// Ensures proper order
		Collections.sort(mergedList, Optimization.MINIMUM_COMPARATOR);
		return mergedList;
	}

	private void consumeAlreadyCoveredBy(final Deque<Optimization> queue, final List<Optimization> list) {

		if (queue.isEmpty() || list.isEmpty()) {
			return;
		}

		final Optimization last = list.getLast();
		for (Optimization a = queue.peek(); (a != null) && (a.getMaximum() < last.getMaximum()); a = queue.peek()) {
			queue.poll(); // (discard)
		}
	}

}
