package com.guthi.service;

import com.guthi.dto.request.CreateDonationClaimRequest;
import com.guthi.dto.response.AllocationDto;
import com.guthi.dto.response.DonationClaimResponse;
import com.guthi.dto.response.DonationPersonalBroadcast;
import com.guthi.dto.response.DonationPoolBroadcast;
import com.guthi.dto.response.DonationSummaryResponse;
import com.guthi.enums.DonationClaimStatus;
import com.guthi.enums.RequestStatus;
import com.guthi.enums.RequestType;
import com.guthi.exception.BadRequestException;
import com.guthi.exception.ResourceNotFoundException;
import com.guthi.live.LiveUpdatePublisher;
import com.guthi.model.CommunityRequest;
import com.guthi.model.Donation;
import com.guthi.model.DonationClaim;
import com.guthi.model.User;
import com.guthi.repository.DonationAllocationRepository;
import com.guthi.repository.DonationClaimRepository;
import com.guthi.repository.DonationRepository;
import com.guthi.repository.MongoFinancialAggregations;
import com.guthi.repository.RequestRepository;
import com.guthi.repository.UserRepository;
import com.guthi.security.SecurityUtils;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DonationService {

    private static final Set<BigDecimal> ALLOWED_PRESET_AMOUNTS =
            Set.of(
                    new BigDecimal("50"),
                    new BigDecimal("1000"),
                    new BigDecimal("2000"),
                    new BigDecimal("3000"),
                    new BigDecimal("4000"),
                    new BigDecimal("5000"));

    private final DonationRepository donationRepository;
    private final DonationAllocationRepository allocationRepository;
    private final MongoFinancialAggregations financialAggregations;
    private final UserRepository userRepository;
    private final DonationClaimRepository donationClaimRepository;
    private final LiveUpdatePublisher liveUpdatePublisher;
    private final RequestRepository requestRepository;

    private static final String DEV_WALLET_MARKER = "GUTHI_DEV_WALLET_SEED";

    public void submitClaim(CreateDonationClaimRequest request) {
        User donor = SecurityUtils.requireCurrentUser();
        if (!isAllowedPresetAmount(request.getAmount())) {
            throw new BadRequestException("Amount must be one of the preset donation tiers");
        }
        String txnId = request.getUpiTransactionId().trim();
        if (donationClaimRepository.existsByUpiTransactionId(txnId)) {
            throw new BadRequestException("This transaction ID was already submitted");
        }
        DonationClaim claim =
                DonationClaim.builder()
                        .donorId(donor.getId())
                        .amount(request.getAmount())
                        .note(request.getNote())
                        .upiTransactionId(txnId)
                        .status(DonationClaimStatus.PENDING)
                        .submittedAt(Instant.now())
                        .build();
        donationClaimRepository.save(claim);
        List<DonationClaimResponse> claimsSnap = listPendingClaimsForAdmin();
        DonationPersonalBroadcast donorPersonal = getPersonalBroadcastForUser(donor);
        liveUpdatePublisher.runAfterCommit(
                () -> {
                    liveUpdatePublisher.publishAdminDonationClaims(claimsSnap);
                    liveUpdatePublisher.publishDonationPersonal(donor.getEmail(), donorPersonal);
                });
    }

    public List<DonationClaimResponse> listPendingClaimsForAdmin() {
        return donationClaimRepository.findByStatusOrderBySubmittedAtAsc(DonationClaimStatus.PENDING).stream()
                .map(this::toClaimResponse)
                .toList();
    }

    public DonationClaimResponse approveClaim(String claimId, User admin) {
        DonationClaim claim =
                donationClaimRepository
                        .findById(claimId)
                        .orElseThrow(() -> new ResourceNotFoundException("Donation claim not found"));
        if (claim.getStatus() != DonationClaimStatus.PENDING) {
            throw new BadRequestException("Claim is not pending");
        }
        User donor =
                userRepository
                        .findById(claim.getDonorId())
                        .orElseThrow(() -> new ResourceNotFoundException("Donor not found"));
        Donation donation =
                Donation.builder()
                        .donorId(donor.getId())
                        .amount(claim.getAmount())
                        .note(claim.getNote())
                        .donatedAt(Instant.now())
                        .build();
        donationRepository.save(donation);
        claim.setStatus(DonationClaimStatus.APPROVED);
        claim.setResolvedAt(Instant.now());
        claim.setResolvedById(admin.getId());
        donationClaimRepository.save(claim);
        DonationPoolBroadcast poolSnap = getPoolBroadcast();
        DonationPersonalBroadcast donorPersonal = getPersonalBroadcastForUser(donor);
        List<DonationClaimResponse> claimsSnap = listPendingClaimsForAdmin();
        liveUpdatePublisher.runAfterCommit(
                () -> {
                    liveUpdatePublisher.publishDonationPool(poolSnap);
                    liveUpdatePublisher.publishDonationPersonal(donor.getEmail(), donorPersonal);
                    liveUpdatePublisher.publishAdminDonationClaims(claimsSnap);
                });
        return toClaimResponse(claim);
    }

    public DonationClaimResponse rejectClaim(String claimId, User admin) {
        DonationClaim claim =
                donationClaimRepository
                        .findById(claimId)
                        .orElseThrow(() -> new ResourceNotFoundException("Donation claim not found"));
        if (claim.getStatus() != DonationClaimStatus.PENDING) {
            throw new BadRequestException("Claim is not pending");
        }
        User donor =
                userRepository
                        .findById(claim.getDonorId())
                        .orElseThrow(() -> new ResourceNotFoundException("Donor not found"));
        claim.setStatus(DonationClaimStatus.REJECTED);
        claim.setResolvedAt(Instant.now());
        claim.setResolvedById(admin.getId());
        donationClaimRepository.save(claim);
        List<DonationClaimResponse> claimsSnap = listPendingClaimsForAdmin();
        DonationPersonalBroadcast donorPersonal = getPersonalBroadcastForUser(donor);
        liveUpdatePublisher.runAfterCommit(
                () -> {
                    liveUpdatePublisher.publishAdminDonationClaims(claimsSnap);
                    liveUpdatePublisher.publishDonationPersonal(donor.getEmail(), donorPersonal);
                });
        return toClaimResponse(claim);
    }

    public DonationSummaryResponse getSummary() {
        DonationPoolBroadcast pool = getPoolBroadcast();
        Optional<String> memberId = currentMemberId(SecurityContextHolder.getContext().getAuthentication());
        DonationPersonalBroadcast personal =
                memberId.flatMap(userRepository::findById)
                        .map(this::getPersonalBroadcastForUser)
                        .orElse(
                                DonationPersonalBroadcast.builder()
                                        .myContributionsTotal(BigDecimal.ZERO)
                                        .myPendingClaimsTotal(BigDecimal.ZERO)
                                        .myUsedFromPoolTotal(BigDecimal.ZERO)
                                        .myNetContribution(BigDecimal.ZERO)
                                        .build());
        return DonationSummaryResponse.builder()
                .totalDonated(pool.getTotalDonated())
                .totalAllocated(pool.getTotalAllocated())
                .availablePool(pool.getAvailablePool())
                .myContributionsTotal(personal.getMyContributionsTotal())
                .myPendingClaimsTotal(personal.getMyPendingClaimsTotal())
                .myUsedFromPoolTotal(personal.getMyUsedFromPoolTotal())
                .myNetContribution(personal.getMyNetContribution())
                .allocations(pool.getAllocations())
                .build();
    }

    public DonationPoolBroadcast getPoolBroadcast() {
        BigDecimal totalDonated = nz(financialAggregations.sumTotalDonated());
        BigDecimal totalAllocated = nz(financialAggregations.sumTotalAllocated());
        BigDecimal pool = totalDonated.subtract(totalAllocated);
        var allocations =
                allocationRepository.findAllByOrderByAllocatedAtDesc().stream()
                        .map(
                                a ->
                                        AllocationDto.builder()
                                                .purpose(a.getPurpose())
                                                .amountUsed(a.getAmountUsed())
                                                .date(
                                                        a.getAllocatedAt()
                                                                .atZone(ZoneId.systemDefault())
                                                                .toLocalDate())
                                                .build())
                        .toList();
        return DonationPoolBroadcast.builder()
                .totalDonated(totalDonated)
                .totalAllocated(totalAllocated)
                .availablePool(pool)
                .allocations(allocations)
                .build();
    }

    public DonationPersonalBroadcast getPersonalBroadcastForUser(User user) {
        String uid = user.getId();
        BigDecimal myContributions = nz(financialAggregations.sumTotalAmountByDonorId(uid));
        BigDecimal myUsedFromPool =
                nz(financialAggregations.sumApprovedFundByRequesterId(uid, RequestStatus.APPROVED));
        BigDecimal myPendingClaims =
                nz(financialAggregations.sumPendingAmountByDonorId(uid, DonationClaimStatus.PENDING));
        BigDecimal myNet = myContributions.subtract(myUsedFromPool);
        return DonationPersonalBroadcast.builder()
                .myContributionsTotal(myContributions)
                .myPendingClaimsTotal(myPendingClaims)
                .myUsedFromPoolTotal(myUsedFromPool)
                .myNetContribution(myNet)
                .build();
    }

    public BigDecimal getTotalAvailablePool() {
        return nz(financialAggregations.sumTotalDonated()).subtract(nz(financialAggregations.sumTotalAllocated()));
    }

    /**
     * Dev-only: insert one verified donation and one approved fund request for the signed-in user so
     * the home summary is non-zero (covers self-registered accounts that never ran full demo seed).
     */
    public void seedDevWalletForCurrentUser() {
        User user = SecurityUtils.requireCurrentUser();
        if (donationRepository.existsByDonorIdAndNote(user.getId(), DEV_WALLET_MARKER)) {
            throw new BadRequestException("Sample wallet data was already added for this account.");
        }
        Instant now = Instant.now();
        donationRepository.save(
                Donation.builder()
                        .donorId(user.getId())
                        .amount(new BigDecimal("15000"))
                        .note(DEV_WALLET_MARKER)
                        .donatedAt(now)
                        .build());
        requestRepository.save(
                CommunityRequest.builder()
                        .requesterId(user.getId())
                        .type(RequestType.RITUAL_HELP_FUND)
                        .description("Demo: approved ritual fund request (dev sample).")
                        .fundAmountRequested(new BigDecimal("3500"))
                        .status(RequestStatus.APPROVED)
                        .adminNote("Auto-approved (dev sample wallet).")
                        .createdAt(now.minus(2, ChronoUnit.DAYS))
                        .resolvedAt(now.minus(1, ChronoUnit.DAYS))
                        .build());
        DonationPoolBroadcast poolSnap = getPoolBroadcast();
        DonationPersonalBroadcast personal = getPersonalBroadcastForUser(user);
        liveUpdatePublisher.runAfterCommit(
                () -> {
                    liveUpdatePublisher.publishDonationPool(poolSnap);
                    liveUpdatePublisher.publishDonationPersonal(user.getEmail(), personal);
                });
    }

    private DonationClaimResponse toClaimResponse(DonationClaim c) {
        User donor =
                userRepository
                        .findById(c.getDonorId())
                        .orElseThrow(() -> new ResourceNotFoundException("Donor not found"));
        return DonationClaimResponse.builder()
                .id(c.getId())
                .donorName(donor.getFullName())
                .donorEmail(donor.getEmail())
                .amount(c.getAmount())
                .note(c.getNote())
                .upiTransactionId(c.getUpiTransactionId())
                .status(c.getStatus())
                .submittedAt(c.getSubmittedAt())
                .resolvedAt(c.getResolvedAt())
                .build();
    }

    private static boolean isAllowedPresetAmount(BigDecimal amount) {
        for (BigDecimal allowed : ALLOWED_PRESET_AMOUNTS) {
            if (allowed.compareTo(amount) == 0) {
                return true;
            }
        }
        return false;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private Optional<String> currentMemberId(Authentication auth) {
        if (auth == null
                || !auth.isAuthenticated()
                || auth instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        String email = auth.getName();
        if (email == null || email.isBlank() || "anonymousUser".equals(email)) {
            return Optional.empty();
        }
        return userRepository.findByEmailIgnoreCase(email).map(User::getId);
    }
}
