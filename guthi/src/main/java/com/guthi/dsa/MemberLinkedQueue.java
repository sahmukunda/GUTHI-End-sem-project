package com.guthi.dsa;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Singly linked list of community members. A volunteer who declines is unlinked from the middle of
 * the active list, and the next person still waiting on the master queue is appended at the tail.
 */
public final class MemberLinkedQueue {

    private static final class Node {
        private final String userId;
        private Node next;

        private Node(String userId) {
            this.userId = userId;
        }
    }

    private Node head;
    private Node tail;

    public static MemberLinkedQueue of(List<String> userIds) {
        MemberLinkedQueue queue = new MemberLinkedQueue();
        if (userIds == null) {
            return queue;
        }
        for (String userId : userIds) {
            if (userId != null && !userId.isBlank()) {
                queue.append(userId);
            }
        }
        return queue;
    }

    public void append(String userId) {
        Node node = new Node(userId);
        if (head == null) {
            head = node;
            tail = node;
            return;
        }
        tail.next = node;
        tail = node;
    }

    /** Unlink {@code userId} wherever it sits, including the middle of the list. */
    public boolean remove(String userId) {
        if (head == null || userId == null) {
            return false;
        }
        if (userId.equals(head.userId)) {
            head = head.next;
            if (head == null) {
                tail = null;
            }
            return true;
        }
        Node previous = head;
        while (previous.next != null) {
            if (userId.equals(previous.next.userId)) {
                if (previous.next == tail) {
                    tail = previous;
                }
                previous.next = previous.next.next;
                return true;
            }
            previous = previous.next;
        }
        return false;
    }

    /** Walk from the head and collect up to {@code count} ids that are not in {@code skip}. */
    public List<String> firstAvailable(int count, Set<String> skip) {
        List<String> picked = new ArrayList<>();
        if (count <= 0) {
            return picked;
        }
        Set<String> blocked = skip == null ? Set.of() : skip;
        for (Node node = head; node != null && picked.size() < count; node = node.next) {
            if (!blocked.contains(node.userId)) {
                picked.add(node.userId);
            }
        }
        return picked;
    }

    public List<String> toList() {
        List<String> ids = new ArrayList<>();
        for (Node node = head; node != null; node = node.next) {
            ids.add(node.userId);
        }
        return ids;
    }

    /**
     * Drop the member who declined, then append the next unused member from {@code masterOrder}.
     * Returns that new member, or null when the queue has nobody left.
     */
    public static String replaceDeclined(
            List<String> activeUserIds, String declinedUserId, List<String> masterOrder, Set<String> alreadyUsed) {
        MemberLinkedQueue active = of(activeUserIds);
        active.remove(declinedUserId);
        Set<String> skip = new HashSet<>();
        if (alreadyUsed != null) {
            skip.addAll(alreadyUsed);
        }
        if (declinedUserId != null) {
            skip.add(declinedUserId);
        }
        List<String> next = of(masterOrder).firstAvailable(1, skip);
        if (next.isEmpty()) {
            return null;
        }
        active.append(next.get(0));
        return next.get(0);
    }
}
