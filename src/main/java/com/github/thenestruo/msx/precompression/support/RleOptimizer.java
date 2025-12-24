package com.github.thenestruo.msx.precompression.support;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

public class RleOptimizer {

	private final MsxCharset charset;

	public RleOptimizer(final MsxCharset charset) {
		this.charset = charset;
	}

	public void optimizeCharset() {

		for (final OptimizationRange optimizationRange : this.findOptimizationRanges()) {
			optimizationRange.applyTo(this.charset);
		}
	}

	private List<OptimizationRange> findOptimizationRanges() {

		final List<OptimizationRange> patternRanges =
				this.mergeOptimizationRanges(
					this.findForwardOptimizationRanges(OptimizationRange.Type.PATTERN),
					this.findBackwardsOptimizationRanges(OptimizationRange.Type.PATTERN));

		final List<OptimizationRange> colorRanges =
				this.mergeOptimizationRanges(
					this.findForwardOptimizationRanges(OptimizationRange.Type.COLOR),
					this.findBackwardsOptimizationRanges(OptimizationRange.Type.COLOR));

		final List<OptimizationRange> ranges =
				this.mergeOptimizationRanges(
					patternRanges,
					colorRanges);

		return ranges;
	}

	private List<OptimizationRange> findForwardOptimizationRanges(final OptimizationRange.Type type) {

		final List<OptimizationRange> ranges = new ArrayList<>();

		final int n = this.charset.size();
		int i = 0;
		MsxLine reference = this.charset.get(i++);
		Integer from = null;
		for (; i < n; i++) {
			final MsxLine candidate = this.charset.get(i);
			final MsxLine optimized = type.getOptimizer().apply(candidate, reference);

			if (optimized != null) {
				reference = optimized;
				if (from == null) {
					// The new range starts in the previous index (i-1)
					from = i - 1;
				}

			} else {
				if (from != null) {
					// The range ends in the previous index (i+1)
					ranges.add(new OptimizationRange(type, reference, from, i - 1));
				}
				reference = candidate;
				from = null;
			}
		}

		if (from != null) {
			// The range ends in the previous index (i+1)
			ranges.add(new OptimizationRange(type, reference, from, i - 1));
		}

		Collections.sort(ranges, OptimizationRange.COMPARATOR_BY_FROM);
		return ranges;
	}

	private List<OptimizationRange> findBackwardsOptimizationRanges(final OptimizationRange.Type type) {

		final List<OptimizationRange> ranges = new ArrayList<>();

		final int n = this.charset.size();
		int i = n - 1;
		MsxLine reference = this.charset.get(i--);
		Integer from = null;
		for (; i >= 0; i--) {
			final MsxLine candidate = this.charset.get(i);
			final MsxLine optimized = type.getOptimizer().apply(candidate, reference);

			if (optimized != null) {
				reference = optimized;
				if (from == null) {
					// The new range starts in the previous index (i+1)
					from = i + 1;
				}

			} else {
				if (from != null) {
					// The range ends in the previous index (i+1)
					ranges.add(new OptimizationRange(type, reference, from, i + 1));
				}
				reference = candidate;
				from = null;
			}
		}

		if (from != null) {
			// The range ends in the previous index (i+1)
			ranges.add(new OptimizationRange(type, reference, from, i + 1));
		}

		Collections.sort(ranges, OptimizationRange.COMPARATOR_BY_FROM);
		return ranges;
	}

	private List<OptimizationRange> mergeOptimizationRanges(
			final List<OptimizationRange> listA, final List<OptimizationRange> listB) {

		// (sanity checks)
		if (listA.isEmpty()) {
			return listB;
		}
		if (listB.isEmpty()) {
			return listA;
		}

		final Deque<OptimizationRange> queueA = new ArrayDeque<>(listA);
		final Deque<OptimizationRange> queueB = new ArrayDeque<>(listB);

		final List<OptimizationRange> mergedList = new ArrayList<>();

		while (!queueA.isEmpty() || !queueB.isEmpty()) {

			// (discards already covered ranges)
			if (!mergedList.isEmpty()) {
				final OptimizationRange last = mergedList.getLast();
				for (OptimizationRange a = queueA.peek(); a != null && (a.isStrictlyBefore(last) || last.contains(a)); a = queueA.peek()) {
					queueA.poll(); // (discard)
				}
				for (OptimizationRange b = queueB.peek(); b != null && (b.isStrictlyBefore(last) || last.contains(b)); b = queueB.peek()) {
					queueB.poll(); // (discard)
				}
			}

			// (one list exhausted: keeps the remaining items from the other list)
			if (queueA.isEmpty()) {
				while (!queueB.isEmpty()) {
					mergedList.add(queueB.poll());
				}
				continue;
			}
			if (queueB.isEmpty()) {
				while (!queueA.isEmpty()) {
					mergedList.add(queueA.poll());
				}
				continue;
			}

			final OptimizationRange a = queueA.peek();
			final OptimizationRange b = queueB.peek();

			// (non-overlapped: keeps the first item)
			if (a.isStrictlyBefore(b)) {
				mergedList.add(queueA.poll()); // (keeps)
				continue;
			}
			if (b.isStrictlyBefore(a)) {
				mergedList.add(queueB.poll()); // (keeps)
				continue;
			}

			// (overlapped, contained: discards the contained range to prefer the bigger one)
			if (a.contains(b)) {
				queueB.poll(); // (skip)
				continue;
			}
			if (b.contains(a)) {
				queueA.poll(); // (skip)
				continue;
			}

			// (overlapped, mergeable)
			if (OptimizationRange.COMPARATOR_BY_FROM.compare(a, b) < 0) {
				// (a comes first)
				if (a.isMergeableWith(b)) {
					final OptimizationRange mergedItem = a.mergeWith(b);
					while (!queueA.isEmpty() && mergedItem.contains(queueA.peek())) {
						queueA.poll();
					}
					queueA.addFirst(mergedItem); // (replace)
					continue;
				}
			} else {
				// (b comes first)
				if (b.isMergeableWith(a)) {
					final OptimizationRange mergedItem = b.mergeWith(a);
					while (!queueB.isEmpty() && mergedItem.contains(queueB.peek())) {
						queueB.poll();
					}
					queueB.addFirst(mergedItem); // (replace)
					continue;
				}
			}

			// (overlapped, not-mergeable: keeps the larger, skips the smaller)
			if (a.size() > b.size()) {
				mergedList.add(queueA.poll()); // (keeps)
				queueB.poll(); // (skips)
			} else {
				mergedList.add(queueB.poll()); // (keeps)
				queueA.poll(); // (skips)
			}
		}

		Collections.sort(mergedList, OptimizationRange.COMPARATOR_BY_FROM); // (unnecessary?)
		return mergedList;
	}
}
