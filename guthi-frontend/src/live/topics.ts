/** Must match `LiveUpdatePublisher` in the Spring app. */
export const LIVE_TOPICS = {
  donationsPool: "/topic/donations/pool",
  donationsPersonalQueue: "/user/queue/donations-personal",
  adminDonationClaims: "/topic/admin/donation-claims",
  adminRequests: "/topic/admin/requests",
  requestsMyQueue: "/user/queue/requests-my",
  volunteerCalls: "/user/queue/volunteer-calls",
  volunteerEvents: "/topic/volunteer-events",
  adminVolunteerSignups: "/topic/admin/volunteer-signups",
  workPosts: "/topic/work-posts",
  festivals: "/topic/festivals",
  festivalReviews: "/topic/festival-reviews",
} as const;
