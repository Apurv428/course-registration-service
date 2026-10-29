package waitlist.dto;

import java.util.Optional;

/**
 * Result of an enrollment operation.
 *
 * <p>Dropping an enrolled learner returns {@link EnrollmentStatus#DROPPED}; its
 * {@code promotedLearnerId} is the waitlisted learner who took the freed seat, or null if
 * nobody was eligible. Dropping a waitlisted learner returns
 * {@link EnrollmentStatus#REMOVED_FROM_WAITLIST} and never promotes anyone. For every other
 * status {@code promotedLearnerId} is null.
 */
public record EnrollmentResult(EnrollmentStatus status, String promotedLearnerId) {

    public static EnrollmentResult of(EnrollmentStatus status) {
        return new EnrollmentResult(status, null);
    }

    public static EnrollmentResult dropped(String promotedLearnerId) {
        return new EnrollmentResult(EnrollmentStatus.DROPPED, promotedLearnerId);
    }

    public Optional<String> promoted() {
        return Optional.ofNullable(promotedLearnerId);
    }
}
