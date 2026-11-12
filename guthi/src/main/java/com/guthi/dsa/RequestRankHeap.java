package com.guthi.dsa;

import com.guthi.enums.RequestType;
import com.guthi.model.CommunityRequest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Min-heap of pending community requests. The root is the request the Guthi should look at first.
 * Order is type urgency (ritual with people, ritual fund, infrastructure, land), then who has waited
 * longest, then document id.
 */
public final class RequestRankHeap {

    private final List<CommunityRequest> heap = new ArrayList<>();

    public void offer(CommunityRequest request) {
        heap.add(request);
        siftUp(heap.size() - 1);
    }

    public CommunityRequest poll() {
        if (heap.isEmpty()) {
            return null;
        }
        CommunityRequest root = heap.get(0);
        CommunityRequest last = heap.remove(heap.size() - 1);
        if (!heap.isEmpty()) {
            heap.set(0, last);
            siftDown(0);
        }
        return root;
    }

    public int size() {
        return heap.size();
    }

    private void siftUp(int index) {
        while (index > 0) {
            int parent = (index - 1) / 2;
            if (comesBefore(heap.get(index), heap.get(parent))) {
                swap(index, parent);
                index = parent;
            } else {
                break;
            }
        }
    }

    private void siftDown(int index) {
        int size = heap.size();
        while (true) {
            int left = index * 2 + 1;
            int right = left + 1;
            int smallest = index;
            if (left < size && comesBefore(heap.get(left), heap.get(smallest))) {
                smallest = left;
            }
            if (right < size && comesBefore(heap.get(right), heap.get(smallest))) {
                smallest = right;
            }
            if (smallest == index) {
                return;
            }
            swap(index, smallest);
            index = smallest;
        }
    }

    private void swap(int i, int j) {
        CommunityRequest tmp = heap.get(i);
        heap.set(i, heap.get(j));
        heap.set(j, tmp);
    }

    /** True when {@code a} should be served before {@code b}. */
    static boolean comesBefore(CommunityRequest a, CommunityRequest b) {
        int byType = Integer.compare(urgency(a.getType()), urgency(b.getType()));
        if (byType != 0) {
            return byType < 0;
        }
        Instant aAt = a.getCreatedAt();
        Instant bAt = b.getCreatedAt();
        if (aAt == null || bAt == null) {
            if (aAt == null && bAt == null) {
                return idOf(a).compareTo(idOf(b)) < 0;
            }
            return aAt != null;
        }
        int byTime = aAt.compareTo(bAt);
        if (byTime != 0) {
            return byTime < 0;
        }
        return idOf(a).compareTo(idOf(b)) < 0;
    }

    static int urgency(RequestType type) {
        if (type == null) {
            return 4;
        }
        return switch (type) {
            case RITUAL_HELP_PEOPLE -> 0;
            case RITUAL_HELP_FUND -> 1;
            case INFRASTRUCTURE_FUND -> 2;
            case LAND_RENT -> 3;
        };
    }

    private static String idOf(CommunityRequest request) {
        return request.getId() == null ? "" : request.getId();
    }
}
