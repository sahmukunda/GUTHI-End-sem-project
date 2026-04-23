package com.guthi.config;

import com.guthi.enums.DonationClaimStatus;
import com.guthi.enums.RequestStatus;
import com.guthi.enums.RequestType;
import com.guthi.enums.Role;
import com.guthi.model.CommunityRequest;
import com.guthi.model.Donation;
import com.guthi.model.DonationAllocation;
import com.guthi.model.DonationClaim;
import com.guthi.model.Festival;
import com.guthi.model.Review;
import com.guthi.model.User;
import com.guthi.model.VolunteerEvent;
import com.guthi.model.VolunteerSignup;
import com.guthi.model.WorkAcceptance;
import com.guthi.model.WorkPost;
import com.guthi.repository.DonationAllocationRepository;
import com.guthi.repository.DonationClaimRepository;
import com.guthi.repository.DonationRepository;
import com.guthi.repository.FestivalRepository;
import com.guthi.repository.RequestRepository;
import com.guthi.repository.ReviewRepository;
import com.guthi.repository.UserRepository;
import com.guthi.repository.VolunteerEventRepository;
import com.guthi.repository.VolunteerEventReviewRepository;
import com.guthi.repository.VolunteerSignupRepository;
import com.guthi.repository.WorkAcceptanceRepository;
import com.guthi.repository.WorkPostRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Seeds a full set of realistic demo data for the `dev` profile so MongoDB Compass immediately has
 * something to look at. Idempotent: runs only when the admin user is not yet present.
 */
@Configuration
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DevDataLoader {

    private static final String ADMIN_EMAIL = "admin@guthi.local";
    private static final String DEFAULT_PASSWORD = "ChangeMe123!";

    private final UserRepository userRepository;
    private final DonationRepository donationRepository;
    private final DonationAllocationRepository donationAllocationRepository;
    private final DonationClaimRepository donationClaimRepository;
    private final RequestRepository requestRepository;
    private final FestivalRepository festivalRepository;
    private final ReviewRepository reviewRepository;
    private final VolunteerEventRepository volunteerEventRepository;
    private final VolunteerSignupRepository volunteerSignupRepository;
    private final VolunteerEventReviewRepository volunteerEventReviewRepository;
    private final WorkPostRepository workPostRepository;
    private final WorkAcceptanceRepository workAcceptanceRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner seedDemoData() {
        return args -> {
            ensureAdmin();
            if (donationRepository.count() > 0) {
                log.info("Dev seed skipped (demo data already present in MongoDB)");
                return;
            }
            runSeed();
        };
    }

    /** Drop all activity collections and re-seed. Safe to call at any time in dev. */
    public void forceSeed() {
        log.info("Force-reseed requested — clearing activity collections…");
        donationRepository.deleteAll();
        donationClaimRepository.deleteAll();
        donationAllocationRepository.deleteAll();
        requestRepository.deleteAll();
        festivalRepository.deleteAll();
        reviewRepository.deleteAll();
        volunteerEventRepository.deleteAll();
        volunteerSignupRepository.deleteAll();
        volunteerEventReviewRepository.deleteAll();
        workPostRepository.deleteAll();
        workAcceptanceRepository.deleteAll();
        runSeed();
    }

    private void runSeed() {
        log.info("Seeding Guthi demo data into MongoDB…");
        User admin = ensureAdmin();
        List<User> members = ensureMembers();

        List<Donation> donations = seedDonations(members);
        seedDonationClaims(members, admin);
        List<CommunityRequest> requests = seedCommunityRequests(members, admin);
        seedDonationAllocations(requests, admin, donations);

        List<Festival> festivals = seedFestivals();
        seedFestivalReviews(festivals, members);

        List<VolunteerEvent> events = seedVolunteerEvents();
        seedVolunteerSignups(events, members);

        List<WorkPost> posts = seedWorkPosts(members);
        seedWorkAcceptances(posts, members);

        log.info(
                "Dev seed complete — login as {} / {} (admin) or priya@guthi.local / {}",
                ADMIN_EMAIL,
                DEFAULT_PASSWORD,
                DEFAULT_PASSWORD);
    }

    private User ensureAdmin() {
        return userRepository
                .findByEmailIgnoreCase(ADMIN_EMAIL)
                .orElseGet(
                        () -> {
                            log.info("Seeding dev admin {} / {}", ADMIN_EMAIL, DEFAULT_PASSWORD);
                            return userRepository.save(
                                    createUser("Guthi Admin", ADMIN_EMAIL, "9800000000", Role.ADMIN));
                        });
    }

    private List<User> ensureMembers() {
        List<User> toCreate = new ArrayList<>();
        addIfMissing(toCreate, "Priya Shrestha", "priya@guthi.local", "9801111111");
        addIfMissing(toCreate, "Bikash Maharjan", "bikash@guthi.local", "9802222222");
        addIfMissing(toCreate, "Sita Tamang", "sita@guthi.local", "9803333333");
        addIfMissing(toCreate, "Rajan Gurung", "rajan@guthi.local", "9804444444");
        addIfMissing(toCreate, "Anita Karki", "anita@guthi.local", "9805555555");
        addIfMissing(toCreate, "Deepak Rai", "deepak@guthi.local", "9806666666");
        if (!toCreate.isEmpty()) {
            userRepository.saveAll(toCreate);
        }
        List<User> members = new ArrayList<>();
        for (String email : List.of(
                "priya@guthi.local",
                "bikash@guthi.local",
                "sita@guthi.local",
                "rajan@guthi.local",
                "anita@guthi.local",
                "deepak@guthi.local")) {
            userRepository.findByEmailIgnoreCase(email).ifPresent(members::add);
        }
        return members;
    }

    private void addIfMissing(List<User> bucket, String fullName, String email, String mobile) {
        if (!userRepository.existsByEmailIgnoreCase(email)) {
            bucket.add(createUser(fullName, email, mobile, Role.USER));
        }
    }

    private User createUser(String fullName, String email, String mobile, Role role) {
        return User.builder()
                .fullName(fullName)
                .email(email)
                .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                .mobileNumber(mobile)
                .role(role)
                .createdAt(Instant.now().minus(30, ChronoUnit.DAYS))
                .build();
    }

    private List<Donation> seedDonations(List<User> members) {
        List<Donation> donations = new ArrayList<>();
        Instant now = Instant.now();
        donations.add(donation(members.get(0), "5000", "Dashain contribution", now.minus(25, ChronoUnit.DAYS)));
        donations.add(donation(members.get(1), "2500", "Monthly Guthi fund", now.minus(20, ChronoUnit.DAYS)));
        donations.add(donation(members.get(2), "10000", "Temple maintenance", now.minus(18, ChronoUnit.DAYS)));
        donations.add(donation(members.get(3), "1500", "For Tihar deepawali", now.minus(14, ChronoUnit.DAYS)));
        donations.add(donation(members.get(0), "3000", "Indra Jatra offering", now.minus(10, ChronoUnit.DAYS)));
        donations.add(donation(members.get(4), "7500", "Support ritual expenses", now.minus(8, ChronoUnit.DAYS)));
        donations.add(donation(members.get(5), "2000", "Monthly", now.minus(6, ChronoUnit.DAYS)));
        donations.add(donation(members.get(1), "4000", "Anniversary of ancestors", now.minus(4, ChronoUnit.DAYS)));
        donations.add(donation(members.get(2), "1000", "Small gratitude", now.minus(2, ChronoUnit.DAYS)));
        donations.add(donation(members.get(3), "6000", "Repair of guthi ghar", now.minus(1, ChronoUnit.DAYS)));
        return donationRepository.saveAll(donations);
    }

    private Donation donation(User donor, String amount, String note, Instant when) {
        return Donation.builder()
                .donorId(donor.getId())
                .amount(new BigDecimal(amount))
                .note(note)
                .donatedAt(when)
                .build();
    }

    private void seedDonationClaims(List<User> members, User admin) {
        Instant now = Instant.now();
        List<DonationClaim> claims = new ArrayList<>();
        claims.add(
                DonationClaim.builder()
                        .donorId(members.get(0).getId())
                        .amount(new BigDecimal("2500"))
                        .note("Paid via PhonePe UPI")
                        .upiTransactionId("UPI-TXN-100001")
                        .status(DonationClaimStatus.APPROVED)
                        .submittedAt(now.minus(5, ChronoUnit.DAYS))
                        .resolvedAt(now.minus(4, ChronoUnit.DAYS))
                        .resolvedById(admin.getId())
                        .build());
        claims.add(
                DonationClaim.builder()
                        .donorId(members.get(1).getId())
                        .amount(new BigDecimal("1200"))
                        .note("Google Pay transfer")
                        .upiTransactionId("UPI-TXN-100002")
                        .status(DonationClaimStatus.PENDING)
                        .submittedAt(now.minus(1, ChronoUnit.DAYS))
                        .build());
        claims.add(
                DonationClaim.builder()
                        .donorId(members.get(2).getId())
                        .amount(new BigDecimal("500"))
                        .note("Duplicate — please verify")
                        .upiTransactionId("UPI-TXN-100003")
                        .status(DonationClaimStatus.REJECTED)
                        .submittedAt(now.minus(3, ChronoUnit.DAYS))
                        .resolvedAt(now.minus(2, ChronoUnit.DAYS))
                        .resolvedById(admin.getId())
                        .build());
        donationClaimRepository.saveAll(claims);
    }

    private List<CommunityRequest> seedCommunityRequests(List<User> members, User admin) {
        Instant now = Instant.now();
        List<CommunityRequest> requests = new ArrayList<>();

        requests.add(
                CommunityRequest.builder()
                        .requesterId(members.get(0).getId())
                        .type(RequestType.LAND_RENT)
                        .description("Requesting Guthi land at Patan for family rice cultivation (1 year lease).")
                        .landName("Patan North Plot #12")
                        .rentPeriodFrom(LocalDate.now().plusDays(10))
                        .rentPeriodTo(LocalDate.now().plusDays(375))
                        .status(RequestStatus.PENDING)
                        .createdAt(now.minus(6, ChronoUnit.DAYS))
                        .build());

        requests.add(
                CommunityRequest.builder()
                        .requesterId(members.get(1).getId())
                        .type(RequestType.RITUAL_HELP_PEOPLE)
                        .description("Need 8 volunteers for Pitri Tarpan ceremony at Pashupatinath.")
                        .peopleRequested(8)
                        .status(RequestStatus.APPROVED)
                        .adminNote("Approved — please coordinate with event lead.")
                        .createdAt(now.minus(12, ChronoUnit.DAYS))
                        .resolvedAt(now.minus(10, ChronoUnit.DAYS))
                        .build());

        requests.add(
                CommunityRequest.builder()
                        .requesterId(members.get(2).getId())
                        .type(RequestType.RITUAL_HELP_FUND)
                        .description("Fund support for Shraddha ritual (priest fees + offerings).")
                        .fundAmountRequested(new BigDecimal("3500"))
                        .status(RequestStatus.APPROVED)
                        .adminNote("Approved and disbursed.")
                        .createdAt(now.minus(15, ChronoUnit.DAYS))
                        .resolvedAt(now.minus(14, ChronoUnit.DAYS))
                        .build());

        requests.add(
                CommunityRequest.builder()
                        .requesterId(members.get(3).getId())
                        .type(RequestType.INFRASTRUCTURE_FUND)
                        .description("Repair of the western wall of the Guthi Ghar — cracks after monsoon.")
                        .fundAmountRequested(new BigDecimal("12000"))
                        .status(RequestStatus.APPROVED)
                        .adminNote("Approved. Contractor to share receipts.")
                        .createdAt(now.minus(20, ChronoUnit.DAYS))
                        .resolvedAt(now.minus(18, ChronoUnit.DAYS))
                        .build());

        requests.add(
                CommunityRequest.builder()
                        .requesterId(members.get(4).getId())
                        .type(RequestType.LAND_RENT)
                        .description("Short-term rent request for vegetable plot.")
                        .landName("Bhaktapur East Field")
                        .rentPeriodFrom(LocalDate.now().minusDays(60))
                        .rentPeriodTo(LocalDate.now().plusDays(180))
                        .status(RequestStatus.REJECTED)
                        .adminNote("Land is already allocated for ritual purposes this year.")
                        .createdAt(now.minus(22, ChronoUnit.DAYS))
                        .resolvedAt(now.minus(20, ChronoUnit.DAYS))
                        .build());

        return requestRepository.saveAll(requests);
    }

    private void seedDonationAllocations(
            List<CommunityRequest> requests, User admin, List<Donation> donations) {
        List<DonationAllocation> allocations = new ArrayList<>();
        allocations.add(
                DonationAllocation.builder()
                        .purpose("Pitri Tarpan volunteer logistics")
                        .amountUsed(new BigDecimal("1500"))
                        .allocatedAt(LocalDateTime.now().minusDays(10))
                        .approvedById(admin.getId())
                        .build());
        allocations.add(
                DonationAllocation.builder()
                        .purpose("Shraddha ritual — priest and offerings")
                        .amountUsed(new BigDecimal("3500"))
                        .allocatedAt(LocalDateTime.now().minusDays(14))
                        .approvedById(admin.getId())
                        .build());
        allocations.add(
                DonationAllocation.builder()
                        .purpose("Guthi Ghar western wall repair")
                        .amountUsed(new BigDecimal("12000"))
                        .allocatedAt(LocalDateTime.now().minusDays(18))
                        .approvedById(admin.getId())
                        .build());
        allocations.add(
                DonationAllocation.builder()
                        .purpose("Dashain Tika materials")
                        .amountUsed(new BigDecimal("2200"))
                        .allocatedAt(LocalDateTime.now().minusDays(3))
                        .approvedById(admin.getId())
                        .build());
        donationAllocationRepository.saveAll(allocations);
    }

    private List<Festival> seedFestivals() {
        List<Festival> festivals = new ArrayList<>();
        festivals.add(
                Festival.builder()
                        .name("Dashain 2025")
                        .description("Annual Dashain celebrations — tika, blessings and community feast.")
                        .festivalDate(LocalDate.now().minusDays(40))
                        .completed(true)
                        .build());
        festivals.add(
                Festival.builder()
                        .name("Tihar Deepawali")
                        .description("Five-day festival of lights, Laxmi puja and Bhai Tika.")
                        .festivalDate(LocalDate.now().minusDays(20))
                        .completed(true)
                        .build());
        festivals.add(
                Festival.builder()
                        .name("Indra Jatra")
                        .description("Street procession of the living goddess Kumari at Basantapur.")
                        .festivalDate(LocalDate.now().plusDays(25))
                        .completed(false)
                        .build());
        festivals.add(
                Festival.builder()
                        .name("Holi Phagu Purnima")
                        .description("Festival of colours at the Guthi courtyard.")
                        .festivalDate(LocalDate.now().plusDays(75))
                        .completed(false)
                        .build());
        return festivalRepository.saveAll(festivals);
    }

    private void seedFestivalReviews(List<Festival> festivals, List<User> members) {
        Festival dashain = festivals.get(0);
        Festival tihar = festivals.get(1);
        List<Review> reviews = new ArrayList<>();
        reviews.add(review(dashain, members.get(0), 5, "Beautifully organised, tika was peaceful."));
        reviews.add(review(dashain, members.get(1), 4, "Good arrangement, wish the feast started earlier."));
        reviews.add(review(dashain, members.get(2), 5, "Felt the presence of all elders."));
        reviews.add(review(tihar, members.get(0), 4, "Lights and diyas were lovely."));
        reviews.add(review(tihar, members.get(3), 5, "Bhai tika ceremony was really heartwarming."));
        reviewRepository.saveAll(reviews);
    }

    private Review review(Festival festival, User reviewer, int rating, String comment) {
        return Review.builder()
                .festivalId(festival.getId())
                .reviewerId(reviewer.getId())
                .rating(rating)
                .comment(comment)
                .createdAt(Instant.now().minus(2, ChronoUnit.DAYS))
                .build();
    }

    private List<VolunteerEvent> seedVolunteerEvents() {
        List<VolunteerEvent> events = new ArrayList<>();
        events.add(
                VolunteerEvent.builder()
                        .title("Temple courtyard cleaning")
                        .description("Monthly community clean-up of the Guthi temple courtyard.")
                        .volunteersRequired(12)
                        .eventDate(LocalDateTime.now().plusDays(5))
                        .active(true)
                        .build());
        events.add(
                VolunteerEvent.builder()
                        .title("Elder care visit")
                        .description("Visit elderly members of the community and distribute meals.")
                        .volunteersRequired(6)
                        .eventDate(LocalDateTime.now().plusDays(12))
                        .active(true)
                        .build());
        events.add(
                VolunteerEvent.builder()
                        .title("Dashain 2025 setup (completed)")
                        .description("Stage setup, tika station and chair arrangement.")
                        .volunteersRequired(10)
                        .eventDate(LocalDateTime.now().minusDays(42))
                        .active(false)
                        .build());
        return volunteerEventRepository.saveAll(events);
    }

    private void seedVolunteerSignups(List<VolunteerEvent> events, List<User> members) {
        List<VolunteerSignup> signups = new ArrayList<>();
        signups.add(signup(events.get(0), members.get(0)));
        signups.add(signup(events.get(0), members.get(1)));
        signups.add(signup(events.get(0), members.get(2)));
        signups.add(signup(events.get(1), members.get(0)));
        signups.add(signup(events.get(1), members.get(3)));
        signups.add(signup(events.get(2), members.get(1)));
        signups.add(signup(events.get(2), members.get(4)));
        signups.add(signup(events.get(2), members.get(5)));
        volunteerSignupRepository.saveAll(signups);
    }

    private VolunteerSignup signup(VolunteerEvent event, User user) {
        return VolunteerSignup.builder()
                .eventId(event.getId())
                .userId(user.getId())
                .participantName(user.getFullName())
                .mobileNumber(user.getMobileNumber())
                .signedUpAt(Instant.now().minus(3, ChronoUnit.DAYS))
                .build();
    }

    private List<WorkPost> seedWorkPosts(List<User> members) {
        Instant now = Instant.now();
        List<WorkPost> posts = new ArrayList<>();
        posts.add(
                WorkPost.builder()
                        .posterId(members.get(0).getId())
                        .title("Need carpenter for wooden window restoration")
                        .description("Traditional Newari window frames — 3 pieces. Paid job.")
                        .mobileNumber(members.get(0).getMobileNumber())
                        .postedAt(now.minus(4, ChronoUnit.DAYS))
                        .build());
        posts.add(
                WorkPost.builder()
                        .posterId(members.get(2).getId())
                        .title("Driver for festival supplies pickup")
                        .description("Need someone with a small truck for 2 days during Indra Jatra.")
                        .mobileNumber(members.get(2).getMobileNumber())
                        .postedAt(now.minus(2, ChronoUnit.DAYS))
                        .build());
        posts.add(
                WorkPost.builder()
                        .posterId(members.get(3).getId())
                        .title("Photographer for family Bratabandha")
                        .description("Half-day coverage, traditional-style photos preferred.")
                        .mobileNumber(members.get(3).getMobileNumber())
                        .postedAt(now.minus(1, ChronoUnit.DAYS))
                        .build());
        return workPostRepository.saveAll(posts);
    }

    private void seedWorkAcceptances(List<WorkPost> posts, List<User> members) {
        List<WorkAcceptance> acceptances = new ArrayList<>();
        acceptances.add(
                WorkAcceptance.builder()
                        .workPostId(posts.get(0).getId())
                        .acceptedById(members.get(5).getId())
                        .acceptedAt(Instant.now().minus(3, ChronoUnit.DAYS))
                        .build());
        acceptances.add(
                WorkAcceptance.builder()
                        .workPostId(posts.get(1).getId())
                        .acceptedById(members.get(4).getId())
                        .acceptedAt(Instant.now().minus(1, ChronoUnit.DAYS))
                        .build());
        workAcceptanceRepository.saveAll(acceptances);
    }
}
