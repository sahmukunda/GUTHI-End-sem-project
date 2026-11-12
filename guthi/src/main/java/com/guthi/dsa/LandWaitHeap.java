package com.guthi.dsa;

import com.guthi.model.CommunityRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * Min-heap of pending Guthi-land requests. The root is the member who has waited the longest
 * ({@code createdAt} earliest). Ties break by document id so the order is stable.
 */
public final class LandWaitHeap {

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
        int byTime = a.getCreatedAt().compareTo(b.getCreatedAt());
        if (byTime != 0) {
            return byTime < 0;
        }
        String aId = a.getId() == null ? "" : a.getId();
        String bId = b.getId() == null ? "" : b.getId();
        return aId.compareTo(bId) < 0;
    }
}
