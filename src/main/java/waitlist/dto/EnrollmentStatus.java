package waitlist.dto;

/** Outcome of an {@code enroll} or {@code drop} request. */
public enum EnrollmentStatus {
    /** enroll: the learner now holds a seat. */
    ENROLLED,
    /** enroll: the course is full; the learner joined the waitlist tail. */
    WAITLISTED,
    /** enroll: the learner already holds a seat. */
    ALREADY_ENROLLED,
    /** enroll: the learner is already on the waitlist. */
    ALREADY_WAITLISTED,
    /** enroll: the learner does not meet the prerequisite. */
    NOT_ELIGIBLE,
    /** drop: an enrolled learner released their seat. */
    DROPPED,
    /** drop: a waitlisted learner was removed from the waitlist. */
    REMOVED_FROM_WAITLIST,
    /** drop: the learner is neither enrolled nor waitlisted (includes repeated drops). */
    NOT_ENROLLED,
    /** enroll or drop: null learner, null or blank id, or a null course set or element. */
    INVALID_REQUEST
}
