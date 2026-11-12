package com.guthi.dsa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guthi.enums.RequestType;
import com.guthi.model.CommunityRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DsaRankingTest {

    @Test
    void priorityHeapServesRitualBeforeOlderLand() {
        CommunityRequest land = request("land", RequestType.LAND_RENT, Instant.parse("2026-01-01T00:00:00Z"));
        CommunityRequest ritual = request("ritual", RequestType.RITUAL_HELP_PEOPLE, Instant.parse("2026-06-01T00:00:00Z"));
        CommunityRequest infra = request("infra", RequestType.INFRASTRUCTURE_FUND, Instant.parse("2026-03-01T00:00:00Z"));
        RequestRankHeap heap = new RequestRankHeap();
        heap.offer(land);
        heap.offer(infra);
        heap.offer(ritual);
        assertEquals("ritual", heap.poll().getId());
        assertEquals("infra", heap.poll().getId());
        assertEquals("land", heap.poll().getId());
        assertNull(heap.poll());
    }

    @Test
    void sameTypeBreaksTiesByWait() {
        CommunityRequest later = request("later", RequestType.RITUAL_HELP_FUND, Instant.parse("2026-05-01T00:00:00Z"));
        CommunityRequest earlier = request("earlier", RequestType.RITUAL_HELP_FUND, Instant.parse("2026-04-01T00:00:00Z"));
        RequestRankHeap heap = new RequestRankHeap();
        heap.offer(later);
        heap.offer(earlier);
        assertEquals("earlier", heap.poll().getId());
        assertEquals("later", heap.poll().getId());
    }

    @Test
    void occupancyBlocksApprovedOverlapAndGreedyKeepsEarlierFinish() {
        LocalDate jan1 = LocalDate.of(2026, 1, 1);
        LocalDate jan10 = LocalDate.of(2026, 1, 10);
        LandOccupancyCalendar.Span approved = new LandOccupancyCalendar.Span("approved", jan1, jan10);
        LandOccupancyCalendar.Span clash =
                new LandOccupancyCalendar.Span("clash", LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 8));
        LandOccupancyCalendar.Span earlyFinish =
                new LandOccupancyCalendar.Span("early", LocalDate.of(2026, 1, 11), LocalDate.of(2026, 1, 20));
        LandOccupancyCalendar.Span laterFinish =
                new LandOccupancyCalendar.Span("later", LocalDate.of(2026, 1, 15), LocalDate.of(2026, 1, 25));

        assertEquals(approved, LandOccupancyCalendar.approvedConflict(clash, List.of(approved)));
        assertNull(LandOccupancyCalendar.approvedConflict(earlyFinish, List.of(approved)));

        Set<String> picks = LandOccupancyCalendar.selectFitting(List.of(laterFinish, earlyFinish, clash), List.of(approved));
        assertTrue(picks.contains("early"));
        assertFalse(picks.contains("later"));
        assertFalse(picks.contains("clash"));
    }

    @Test
    void knapsackPrefersTwoSmallerRequestsOverOneLarger() {
        FundKnapsack.Plan plan =
                FundKnapsack.solve(
                        List.of(
                                new FundKnapsack.Item("big", 6, 6),
                                new FundKnapsack.Item("a", 5, 5),
                                new FundKnapsack.Item("b", 5, 5)),
                        10);
        assertEquals(Set.of("a", "b"), plan.chosenIds());
        assertEquals(10, plan.spent());
        assertEquals(10, plan.score());
    }

    @Test
    void declineInTheMiddlePullsTheNextMemberOntoTheTail() {
        List<String> master = List.of("A", "B", "C", "D", "E", "F");
        List<String> firstFive = MemberLinkedQueue.of(master).firstAvailable(5, java.util.Set.of());
        assertEquals(List.of("A", "B", "C", "D", "E"), firstFive);

        MemberLinkedQueue active = MemberLinkedQueue.of(firstFive);
        assertTrue(active.remove("C"));
        assertEquals(List.of("A", "B", "D", "E"), active.toList());

        String replacement =
                MemberLinkedQueue.replaceDeclined(firstFive, "C", master, java.util.Set.of("A", "B", "C", "D", "E"));
        assertEquals("F", replacement);
    }

    private static CommunityRequest request(String id, RequestType type, Instant createdAt) {
        return CommunityRequest.builder().id(id).type(type).createdAt(createdAt).build();
    }
}
