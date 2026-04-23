package com.guthi.service;

import com.guthi.dto.request.AdminDecisionRequest;
import com.guthi.dto.request.CreateCommunityRequestDto;
import com.guthi.dto.response.CommunityRequestResponse;
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
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RequestService {

    private final RequestRepository requestRepository;
    private final DonationAllocationRepository allocationRepository;
    private final UserRepository userRepository;
    private final DonationService donationService;
    private final LiveUpdatePublisher liveUpdatePublisher;

    public CommunityRequestResponse create(CreateCommunityRequestDto dto) {
        User requester = SecurityUtils.requireCurrentUser();
        validateAndNormalize(dto);
        assertRequestedFundWithinAvailablePool(dto);
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

    private void validateAndNormalize(CreateCommunityRequestDto dto) {
        RequestType t = dto.getType();
        if (t == RequestType.LAND_RENT) {
            if (dto.getLandName() == null || dto.getLandName().isBlank()) {
                throw new BadRequestException("Land name is required for land rent requests");
            }
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
        return requestRepository.findByRequesterIdOrderByCreatedAtDesc(me.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<CommunityRequestResponse> listAllForAdmin() {
        return requestRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    public CommunityRequestResponse approve(String id, AdminDecisionRequest body, User admin) {
        CommunityRequest req =
                requestRepository
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        if (req.getStatus() != RequestStatus.PENDING) {
            throw new BadRequestException("Request is not pending");
        }
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
                });
        return toResponse(req);
    }

    public CommunityRequestResponse reject(String id, AdminDecisionRequest body) {
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

    private List<CommunityRequestResponse> requestsForUser(User user) {
        return requestRepository.findByRequesterIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toResponse)
                .toList();
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
        User requester =
                userRepository
                        .findById(r.getRequesterId())
                        .orElseThrow(() -> new ResourceNotFoundException("Requester not found"));
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
                .build();
    }
}
