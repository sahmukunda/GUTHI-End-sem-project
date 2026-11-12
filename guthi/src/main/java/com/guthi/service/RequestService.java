package com.guthi.service;

import com.guthi.dsa.FundKnapsack;
import com.guthi.dsa.GuthiLands;
import com.guthi.dsa.LandOccupancyCalendar;
import com.guthi.dsa.LandWaitHeap;
import com.guthi.dsa.RequestRankHeap;
import com.guthi.dto.request.AdminDecisionRequest;
import com.guthi.dto.request.CreateCommunityRequestDto;
import com.guthi.dto.response.CommunityRequestResponse;
import com.guthi.dto.response.VolunteerRosterResponse;
import com.guthi.enums.RequestStatus;
import com.guthi.enums.RequestType;
import com.guthi.exception.BadRequestException;
import com.guthi.exception.InsufficientFundsException;
import com.guthi.exception.ResourceNotFoundException;
import com.guthi.live.LiveUpdatePublisher;
import com.guthi.model.CommunityRequest;
import com.guthi.model.DonationAllocation;
import com.guthi.model.User;
import com.guthi.repository.DonationAllocationRepository;
import com.guthi.repository.RequestRepository;
import com.guthi.repository.UserRepository;
import com.guthi.security.SecurityUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class RequestService {

    private final RequestRepository requestRepository;
    private final DonationAllocationRepository allocationRepository;
    private final UserRepository userRepository;
    private final DonationService donationService;
    private final LiveUpdatePublisher liveUpdatePublisher;
    private final RequestVolunteerService volunteerService;
    private final TransactionTemplate transactionTemplate;

    /**
     * Held around approve and reject until the Mongo transaction has committed. Two admins can
     * otherwise both read the same pool balance or the same free land dates and both succeed.
     */
    private final Object approvalLock = new Object();

    public CommunityRequestResponse create(CreateCommunityRequestDto dto) {
        User requester = SecurityUtils.requireCurrentUser();
        validateAndNormalize(dto);
        assertRequestedFundWithinAvailablePool(dto);
        assertLandDatesFree(dto.getType(), null, dto.getLandName(), dto.getRentPeriodFrom(), dto.getRentPeriodTo());
        String description = buildStoredDescription(dto);
        CommunityRequest req =
                CommunityRequest.builder()
                        .requesterId(requester.getId())
                        .type(dto.getType())
                        .description(description)
                        .landName(dto.getLandName() != null ? dto.getLandName().trim() : null)
                        .rentPeriodFrom(dto.getRentPeriodFrom())
                        .rentPeriodTo(dto.getRentPeriodTo())
                        .fundAmountRequested(dto.getFundAmountRequested())
                        .peopleRequested(dto.getPeopleRequested())
                        .status(RequestStatus.PENDING)
                        .createdAt(Instant.now())
                        .build();
        requestRepository.save(req);
        List<CommunityRequestResponse> adminSnap = listAllForAdmin();
        List<CommunityRequestResponse> mySnap = requestsForUser(requester);
        liveUpdatePublisher.runAfterCommit(
                () -> {
                    liveUpdatePublisher.publishAdminRequests(adminSnap);
                    liveUpdatePublisher.publishUserRequestsMy(requester.getEmail(), mySnap);
                });
        return toResponse(req);
    }

    public CommunityRequestResponse update(String id, CreateCommunityRequestDto dto) {
        User requester = requireOwnPending(id);
        CommunityRequest req =
                requestRepository
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        validateAndNormalize(dto);
        assertRequestedFundWithinAvailablePool(dto);
        assertLandDatesFree(dto.getType(), id, dto.getLandName(), dto.getRentPeriodFrom(), dto.getRentPeriodTo());
        req.setType(dto.getType());
        req.setDescription(buildStoredDescription(dto));
        if (dto.getType() == RequestType.LAND_RENT) {
            req.setLandName(dto.getLandName());
            req.setRentPeriodFrom(dto.getRentPeriodFrom());
            req.setRentPeriodTo(dto.getRentPeriodTo());
        } else {
            req.setLandName(null);
            req.setRentPeriodFrom(null);
            req.setRentPeriodTo(null);
        }
        req.setFundAmountRequested(dto.getFundAmountRequested());
        req.setPeopleRequested(dto.getPeopleRequested());
        requestRepository.save(req);
        publishRequestLists(requester);
        return toResponse(req);
    }

    public void delete(String id) {
        User requester = requireOwnPending(id);
        requestRepository.deleteById(id);
        publishRequestLists(requester);
    }

    private User requireOwnPending(String id) {
        User me = SecurityUtils.requireCurrentUser();
        CommunityRequest req =
                requestRepository
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        if (!me.getId().equals(req.getRequesterId())) {
            throw new BadRequestException("You can only change your own request");
        }
        if (req.getStatus() != RequestStatus.PENDING) {
            throw new BadRequestException("Only a pending request can be changed or withdrawn");
        }
        return me;
    }

    private void publishRequestLists(User requester) {
        List<CommunityRequestResponse> adminSnap = listAllForAdmin();
        List<CommunityRequestResponse> mySnap = requestsForUser(requester);
        liveUpdatePublisher.runAfterCommit(
                () -> {
                    liveUpdatePublisher.publishAdminRequests(adminSnap);
                    liveUpdatePublisher.publishUserRequestsMy(requester.getEmail(), mySnap);
                });
    }

    private void validateAndNormalize(CreateCommunityRequestDto dto) {
        if (dto.getPeopleRequested() != null && dto.getPeopleRequested() < 1) {
            throw new BadRequestException("Volunteers needed must be at least 1 when provided");
        }
        RequestType t = dto.getType();
        if (t == RequestType.LAND_RENT) {
            if (dto.getLandName() == null || dto.getLandName().isBlank()) {
                throw new BadRequestException("Choose a Guthi plot for the land rent request");
            }
            if (GuthiLands.canonical(dto.getLandName()) == null) {
                throw new BadRequestException("Choose one of the Guthi plots from the list");
            }
            dto.setLandName(GuthiLands.canonical(dto.getLandName()));
            if (dto.getRentPeriodFrom() == null || dto.getRentPeriodTo() == null) {
                throw new BadRequestException("Rent period from and to are required for land rent");
            }
            if (dto.getRentPeriodTo().isBefore(dto.getRentPeriodFrom())) {
                throw new BadRequestException("Rent period end must be on or after the start date");
            }
            return;
        }
        if (t == RequestType.RITUAL_HELP_PEOPLE) {
            if (dto.getDescription() == null || dto.getDescription().isBlank()) {
                throw new BadRequestException("Ritual description is required");
            }
            if (dto.getPeopleRequested() != null && dto.getPeopleRequested() < 1) {
                throw new BadRequestException("Number of people must be at least 1 when provided");
            }
            if (dto.getFundAmountRequested() != null
                    && dto.getFundAmountRequested().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Fund amount must be greater than zero when provided");
            }
            return;
        }
        if (t == RequestType.RITUAL_HELP_FUND) {
            if (dto.getDescription() == null || dto.getDescription().isBlank()) {
                throw new BadRequestException("Ritual description is required");
            }
            if (dto.getFundAmountRequested() == null
                    || dto.getFundAmountRequested().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Fund amount is required for ritual help (fund)");
            }
            return;
        }
        if (t == RequestType.INFRASTRUCTURE_FUND) {
            if (dto.getDescription() == null || dto.getDescription().isBlank()) {
                throw new BadRequestException("Description is required for infrastructure fund requests");
            }
            if (dto.getFundAmountRequested() == null
                    || dto.getFundAmountRequested().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Funds required is required for infrastructure fund requests");
            }
        }
    }

    private void assertRequestedFundWithinAvailablePool(CreateCommunityRequestDto dto) {
        BigDecimal requested = dto.getFundAmountRequested();
        if (requested == null || requested.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal pool = donationService.getTotalAvailablePool();
        if (pool.compareTo(BigDecimal.ZERO) < 0) {
            pool = BigDecimal.ZERO;
        }
        if (requested.compareTo(pool) > 0) {
            String reqStr = requested.stripTrailingZeros().toPlainString();
            String poolStr = pool.stripTrailingZeros().toPlainString();
            throw new InsufficientFundsException(
                    "Insufficient funds: you asked for "
                            + reqStr
                            + " but the community donation pool only has "
                            + poolStr
                            + " available right now. Lower the amount or try again after more donations. Your request was not sent to admins.");
        }
    }

    private String buildStoredDescription(CreateCommunityRequestDto dto) {
        RequestType t = dto.getType();
        if (t == RequestType.LAND_RENT) {
            String base =
                    "Land: "
                            + dto.getLandName().trim()
                            + ". Rent period: "
                            + dto.getRentPeriodFrom()
                            + " to "
                            + dto.getRentPeriodTo()
                            + ".";
            String extra = dto.getDescription() != null ? dto.getDescription().trim() : "";
            if (!extra.isEmpty()) {
                return base + " Notes: " + extra;
            }
            return base;
        }
        return Objects.requireNonNullElse(dto.getDescription(), "").trim();
    }

    public List<CommunityRequestResponse> myRequests() {
        User me = SecurityUtils.requireCurrentUser();
        return requestsForUser(me);
    }

    public List<CommunityRequestResponse> listAllForAdmin() {
        List<CommunityRequest> all = requestRepository.findAllByOrderByCreatedAtDesc();
        Ranking ranking = rank(all);
        Map<String, VolunteerRosterResponse> volunteerNotes = volunteerService.notesFor(all);
        return all.stream()
                .map(r -> toResponse(r, ranking, true, volunteerNotes))
                .sorted(this::adminRankOrder)
                .toList();
    }

    public CommunityRequestResponse approve(String id, AdminDecisionRequest body, User admin) {
        synchronized (approvalLock) {
            return transactionTemplate.execute(status -> approveWithinTransaction(id, body, admin));
        }
    }

    private CommunityRequestResponse approveWithinTransaction(String id, AdminDecisionRequest body, User admin) {
        CommunityRequest req =
                requestRepository
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        if (req.getStatus() != RequestStatus.PENDING) {
            throw new BadRequestException("Request is not pending");
        }
        assertLandDatesFree(
                req.getType(), req.getId(), req.getLandName(), req.getRentPeriodFrom(), req.getRentPeriodTo());
        if (req.getFundAmountRequested() != null
                && req.getFundAmountRequested().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal pool = donationService.getTotalAvailablePool();
            if (pool.compareTo(req.getFundAmountRequested()) < 0) {
                throw new InsufficientFundsException("Donation pool insufficient");
            }
            String purpose = allocationPurpose(req);
            DonationAllocation allocation =
                    DonationAllocation.builder()
                            .purpose(purpose)
                            .amountUsed(req.getFundAmountRequested())
                            .allocatedAt(LocalDateTime.now())
                            .approvedById(admin.getId())
                            .build();
            allocationRepository.save(allocation);
        }
        req.setStatus(RequestStatus.APPROVED);
        req.setAdminNote(body != null ? body.getAdminNote() : null);
        req.setResolvedAt(Instant.now());
        requestRepository.save(req);
        List<String> called = volunteerService.assignIfNeeded(req);
        User requester =
                userRepository
                        .findById(req.getRequesterId())
                        .orElseThrow(() -> new ResourceNotFoundException("Requester not found"));
        boolean allocated =
                req.getFundAmountRequested() != null
                        && req.getFundAmountRequested().compareTo(BigDecimal.ZERO) > 0;
        List<CommunityRequestResponse> adminSnap = listAllForAdmin();
        List<CommunityRequestResponse> mySnap = requestsForUser(requester);
        var poolSnap = allocated ? donationService.getPoolBroadcast() : null;
        var personalSnap =
                allocated ? donationService.getPersonalBroadcastForUser(requester) : null;
        liveUpdatePublisher.runAfterCommit(
                () -> {
                    liveUpdatePublisher.publishAdminRequests(adminSnap);
                    liveUpdatePublisher.publishUserRequestsMy(requester.getEmail(), mySnap);
                    if (allocated && poolSnap != null && personalSnap != null) {
                        liveUpdatePublisher.publishDonationPool(poolSnap);
                        liveUpdatePublisher.publishDonationPersonal(requester.getEmail(), personalSnap);
                    }
                    volunteerService.publishForUsers(called);
                });
        return toResponse(req);
    }

    public CommunityRequestResponse reject(String id, AdminDecisionRequest body) {
        synchronized (approvalLock) {
            return rejectWithinLock(id, body);
        }
    }

    private CommunityRequestResponse rejectWithinLock(String id, AdminDecisionRequest body) {
        CommunityRequest req =
                requestRepository
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        if (req.getStatus() != RequestStatus.PENDING) {
            throw new BadRequestException("Request is not pending");
        }
        req.setStatus(RequestStatus.REJECTED);
        req.setAdminNote(body != null ? body.getAdminNote() : null);
        req.setResolvedAt(Instant.now());
        requestRepository.save(req);
        User requester =
                userRepository
                        .findById(req.getRequesterId())
                        .orElseThrow(() -> new ResourceNotFoundException("Requester not found"));
        List<CommunityRequestResponse> adminSnap = listAllForAdmin();
        List<CommunityRequestResponse> mySnap = requestsForUser(requester);
        liveUpdatePublisher.runAfterCommit(
                () -> {
                    liveUpdatePublisher.publishAdminRequests(adminSnap);
                    liveUpdatePublisher.publishUserRequestsMy(requester.getEmail(), mySnap);
                });
        return toResponse(req);
    }

    /** Admin request list, including volunteer roster, after a member accepts or declines a call. */
    public void publishAdminRequestList() {
        liveUpdatePublisher.publishAdminRequests(listAllForAdmin());
    }

    private List<CommunityRequestResponse> requestsForUser(User user) {
        List<CommunityRequest> all = requestRepository.findAllByOrderByCreatedAtDesc();
        Ranking ranking = rank(all);
        Map<String, VolunteerRosterResponse> volunteerNotes = volunteerService.notesFor(all);
        return requestRepository.findByRequesterIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(r -> toResponse(r, ranking, false, volunteerNotes))
                .toList();
    }

    private void assertLandDatesFree(
            RequestType type, String requestId, String landName, LocalDate from, LocalDate to) {
        if (type != RequestType.LAND_RENT || from == null || to == null) {
            return;
        }
        LandOccupancyCalendar.Span conflict =
                LandOccupancyCalendar.approvedConflict(
                        new LandOccupancyCalendar.Span(requestId, from, to),
                        approvedSpans(plotKey(landName), requestId));
        if (conflict == null) {
            return;
        }
        throw new BadRequestException(
                "These dates overlap an approved rental of "
                        + (landName == null ? "this plot" : landName.trim())
                        + " ("
                        + conflict.from()
                        + " to "
                        + conflict.to()
                        + "). Pick dates that do not overlap that booking.");
    }

    private List<LandOccupancyCalendar.Span> approvedSpans(String plot, String ignoreId) {
        List<LandOccupancyCalendar.Span> spans = new ArrayList<>();
        for (CommunityRequest request : requestRepository.findAllByOrderByCreatedAtDesc()) {
            if (!isApprovedLand(request) || !plot.equals(plotKey(request.getLandName()))) {
                continue;
            }
            if (ignoreId != null && ignoreId.equals(request.getId())) {
                continue;
            }
            spans.add(new LandOccupancyCalendar.Span(request.getId(), request.getRentPeriodFrom(), request.getRentPeriodTo()));
        }
        return spans;
    }

    private int adminRankOrder(CommunityRequestResponse a, CommunityRequestResponse b) {
        boolean aPending = a.getStatus() == RequestStatus.PENDING;
        boolean bPending = b.getStatus() == RequestStatus.PENDING;
        if (aPending != bPending) {
            return aPending ? -1 : 1;
        }
        if (aPending) {
            int byRank = Integer.compare(
                    a.getQueueRank() == null ? Integer.MAX_VALUE : a.getQueueRank(),
                    b.getQueueRank() == null ? Integer.MAX_VALUE : b.getQueueRank());
            if (byRank != 0) {
                return byRank;
            }
        }
        Instant aCreated = a.getCreatedAt();
        Instant bCreated = b.getCreatedAt();
        if (aCreated == null || bCreated == null) {
            return 0;
        }
        return bCreated.compareTo(aCreated);
    }

    private static String plotKey(String landName) {
        return landName == null ? "" : landName.trim().toLowerCase(Locale.ROOT);
    }

    private String allocationPurpose(CommunityRequest req) {
        if (req.getType() == RequestType.LAND_RENT && req.getLandName() != null && !req.getLandName().isBlank()) {
            String period =
                    (req.getRentPeriodFrom() != null && req.getRentPeriodTo() != null)
                            ? (" (" + req.getRentPeriodFrom() + " – " + req.getRentPeriodTo() + ")")
                            : "";
            return "Land rent: " + req.getLandName() + period;
        }
        return req.getDescription();
    }

    private CommunityRequestResponse toResponse(CommunityRequest r) {
        List<CommunityRequest> all = requestRepository.findAllByOrderByCreatedAtDesc();
        return toResponse(r, rank(all), false, volunteerService.notesFor(all));
    }

    private CommunityRequestResponse toResponse(
            CommunityRequest r, Ranking ranking, boolean forAdmin, Map<String, VolunteerRosterResponse> volunteerNotes) {
        User requester =
                userRepository
                        .findById(r.getRequesterId())
                        .orElseThrow(() -> new ResourceNotFoundException("Requester not found"));
        int[] place = ranking.landPlaces.get(r.getId());
        Integer queueRank = forAdmin ? ranking.queueRank.get(r.getId()) : null;
        return CommunityRequestResponse.builder()
                .id(r.getId())
                .requesterId(requester.getId())
                .requesterName(requester.getFullName())
                .type(r.getType())
                .description(r.getDescription())
                .landName(r.getLandName())
                .rentPeriodFrom(r.getRentPeriodFrom())
                .rentPeriodTo(r.getRentPeriodTo())
                .fundAmountRequested(r.getFundAmountRequested())
                .peopleRequested(r.getPeopleRequested())
                .status(r.getStatus())
                .adminNote(r.getAdminNote())
                .createdAt(r.getCreatedAt())
                .resolvedAt(r.getResolvedAt())
                .landQueueRank(place == null ? null : place[0])
                .landQueueSize(place == null ? null : place[1])
                .queueRank(queueRank)
                .queueSize(queueRank == null ? null : ranking.queueSize)
                .fundPlanSelected(forAdmin && ranking.fundPlan.contains(r.getId()) ? Boolean.TRUE : null)
                .landOccupied(ranking.landOccupied.contains(r.getId()) ? Boolean.TRUE : null)
                .landOccupancyNote(ranking.landNotes.get(r.getId()))
                .landCalendarPick(ranking.landPicks.contains(r.getId()) ? Boolean.TRUE : null)
                .volunteerRoster(volunteerNotes.get(r.getId()))
                .build();
    }

    /**
     * Per-plot wait heaps, one priority heap over every pending request, a land calendar per plot,
     * and a knapsack over fund requests that fit the current pool.
     */
    private Ranking rank(List<CommunityRequest> all) {
        Ranking ranking = new Ranking();
        ranking.landPlaces = landQueuePlaces(all);
        ranking.queueRank = globalRanks(all);
        ranking.queueSize = ranking.queueRank.size();
        Map<String, List<CommunityRequest>> landByPlot = new HashMap<>();
        for (CommunityRequest request : all) {
            if (request.getType() == RequestType.LAND_RENT && request.getLandName() != null) {
                landByPlot.computeIfAbsent(plotKey(request.getLandName()), key -> new ArrayList<>()).add(request);
            }
        }
        for (List<CommunityRequest> plotRequests : landByPlot.values()) {
            fillLandCalendar(plotRequests, ranking);
        }
        ranking.fundPlan = fundPlan(all);
        return ranking;
    }

    private Map<String, int[]> landQueuePlaces(List<CommunityRequest> all) {
        Map<String, LandWaitHeap> heaps = new HashMap<>();
        for (CommunityRequest request : all) {
            if (request.getType() != RequestType.LAND_RENT || request.getStatus() != RequestStatus.PENDING) {
                continue;
            }
            if (request.getCreatedAt() == null) {
                continue;
            }
            heaps.computeIfAbsent(plotKey(request.getLandName()), key -> new LandWaitHeap()).offer(request);
        }
        Map<String, int[]> places = new HashMap<>();
        for (LandWaitHeap heap : heaps.values()) {
            int size = heap.size();
            int rank = 1;
            CommunityRequest next;
            while ((next = heap.poll()) != null) {
                places.put(next.getId(), new int[] {rank, size});
                rank++;
            }
        }
        return places;
    }

    private Map<String, Integer> globalRanks(List<CommunityRequest> all) {
        RequestRankHeap heap = new RequestRankHeap();
        for (CommunityRequest request : all) {
            if (request.getStatus() == RequestStatus.PENDING && request.getId() != null) {
                heap.offer(request);
            }
        }
        Map<String, Integer> ranks = new HashMap<>();
        int rank = 1;
        CommunityRequest next;
        while ((next = heap.poll()) != null) {
            ranks.put(next.getId(), rank);
            rank++;
        }
        return ranks;
    }

    private void fillLandCalendar(List<CommunityRequest> plotRequests, Ranking ranking) {
        List<LandOccupancyCalendar.Span> approved = new ArrayList<>();
        List<LandOccupancyCalendar.Span> pending = new ArrayList<>();
        for (CommunityRequest request : plotRequests) {
            if (request.getRentPeriodFrom() == null || request.getRentPeriodTo() == null || request.getId() == null) {
                continue;
            }
            LandOccupancyCalendar.Span span =
                    new LandOccupancyCalendar.Span(request.getId(), request.getRentPeriodFrom(), request.getRentPeriodTo());
            if (isApprovedLand(request)) {
                approved.add(span);
            } else if (request.getStatus() == RequestStatus.PENDING) {
                pending.add(span);
            }
        }
        Set<String> picks = LandOccupancyCalendar.selectFitting(pending, approved);
        ranking.landPicks.addAll(picks);
        for (CommunityRequest request : plotRequests) {
            if (request.getStatus() != RequestStatus.PENDING || request.getId() == null) {
                continue;
            }
            LandOccupancyCalendar.Span conflict =
                    LandOccupancyCalendar.approvedConflict(
                            new LandOccupancyCalendar.Span(
                                    request.getId(), request.getRentPeriodFrom(), request.getRentPeriodTo()),
                            approved);
            if (conflict != null) {
                ranking.landOccupied.add(request.getId());
                ranking.landNotes.put(
                        request.getId(),
                        "Occupied by an approved rental from " + conflict.from() + " to " + conflict.to());
            } else if (picks.contains(request.getId())) {
                ranking.landNotes.put(request.getId(), "Free — fits the land calendar");
            } else if (request.getRentPeriodFrom() != null) {
                ranking.landNotes.put(
                        request.getId(),
                        "Free of approved rentals, but these dates overlap another pending request");
            }
        }
    }

    private Set<String> fundPlan(List<CommunityRequest> all) {
        BigDecimal pool = donationService.getTotalAvailablePool();
        if (pool == null || pool.compareTo(BigDecimal.ZERO) < 0) {
            pool = BigDecimal.ZERO;
        }
        long capacity = pool.setScale(0, RoundingMode.FLOOR).longValue();
        List<FundKnapsack.Item> items = new ArrayList<>();
        Instant now = Instant.now();
        for (CommunityRequest request : all) {
            if (request.getStatus() != RequestStatus.PENDING || request.getId() == null) {
                continue;
            }
            BigDecimal amount = request.getFundAmountRequested();
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            long weight = amount.setScale(0, RoundingMode.CEILING).longValue();
            if (weight <= 0) {
                weight = 1;
            }
            items.add(new FundKnapsack.Item(request.getId(), weight, fundValue(request, now)));
        }
        return new HashSet<>(FundKnapsack.solve(items, capacity).chosenIds());
    }

    private static long fundValue(CommunityRequest request, Instant now) {
        RequestType type = request.getType();
        long value =
                switch (type == null ? RequestType.INFRASTRUCTURE_FUND : type) {
                    case RITUAL_HELP_PEOPLE -> 3_000L;
                    case RITUAL_HELP_FUND -> 2_000L;
                    case INFRASTRUCTURE_FUND -> 1_000L;
                    case LAND_RENT -> 500L;
                };
        if (request.getPeopleRequested() != null && request.getPeopleRequested() > 0) {
            value += Math.min(request.getPeopleRequested(), 500) * 10L;
        }
        if (request.getCreatedAt() != null) {
            long days = Duration.between(request.getCreatedAt(), now).toDays();
            if (days > 0) {
                value += Math.min(days, 60);
            }
        }
        return value;
    }

    private static boolean isApprovedLand(CommunityRequest request) {
        return request.getType() == RequestType.LAND_RENT && request.getStatus() == RequestStatus.APPROVED;
    }

    private static final class Ranking {
        private Map<String, int[]> landPlaces = Map.of();
        private Map<String, Integer> queueRank = Map.of();
        private int queueSize;
        private Set<String> fundPlan = Set.of();
        private final Set<String> landOccupied = new HashSet<>();
        private final Map<String, String> landNotes = new HashMap<>();
        private final Set<String> landPicks = new HashSet<>();
    }
}
