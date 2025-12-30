package com.github.thenestruo.msx.precompression.impl;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

import com.github.thenestruo.msx.precompression.OptimizationMerger;
import com.github.thenestruo.msx.precompression.model.Optimization;

public enum DefaultOptimizationMerger implements OptimizationMerger {

	INSTANCE;

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

			// (overlapped, non-mergeable: keeps the larger optimization)
			if (a.size() > b.size()) {
				mergedList.add(queueA.poll()); // (keeps)
				queueB.poll(); // (discards)
				continue;
			}
			if (b.size() > a.size()) {
				mergedList.add(queueB.poll()); // (keeps)
				queueA.poll(); // (discards)
				continue;
			}

			// (overlapped, non-mergeable, same size: keeps the optimization that cover more entropy)
			if (a.entropy() >= b.entropy()) {
				mergedList.add(queueA.poll()); // (keeps)
				queueB.poll(); // (discards)
				continue;
			} else {
				mergedList.add(queueB.poll()); // (keeps)
				queueA.poll(); // (discards)
				continue;
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
