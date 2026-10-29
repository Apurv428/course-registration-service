package waitlist.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static waitlist.dto.EnrollmentStatus.ALREADY_ENROLLED;
import static waitlist.dto.EnrollmentStatus.ALREADY_WAITLISTED;
import static waitlist.dto.EnrollmentStatus.DROPPED;
import static waitlist.dto.EnrollmentStatus.ENROLLED;
import static waitlist.dto.EnrollmentStatus.INVALID_REQUEST;
import static waitlist.dto.EnrollmentStatus.NOT_ELIGIBLE;
import static waitlist.dto.EnrollmentStatus.NOT_ENROLLED;
import static waitlist.dto.EnrollmentStatus.REMOVED_FROM_WAITLIST;
import static waitlist.dto.EnrollmentStatus.WAITLISTED;
import static waitlist.service.ServiceTestSupport.PREREQ;
import static waitlist.service.ServiceTestSupport.State;
import static waitlist.service.ServiceTestSupport.assertInvariants;
import static waitlist.service.ServiceTestSupport.eligible;
import static waitlist.service.ServiceTestSupport.learner;
import static waitlist.service.ServiceTestSupport.state;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import waitlist.dto.EnrollmentResult;
import waitlist.model.Learner;

class CourseEnrollmentServiceTest {

    /** Learners whose eligibility the predicate seam reports as revoked. */
    private final Set<String> blocked = new HashSet<>();

    private CourseEnrollmentService serviceWithBlocklist(int capacity) {
        return new CourseEnrollmentService(capacity, l -> !blocked.contains(l.id()));
    }

    /** Capacity 1; "a" holds the seat and "b" is waitlisted. */
    private static CourseEnrollmentService fullServiceWithWaiter() {
        CourseEnrollmentService service = new CourseEnrollmentService(1, PREREQ);
        service.enroll(eligible("a"));
        service.enroll(eligible("b"));
        return service;
    }

    // 1. capacity available

    @Test
    void enrollsEligibleLearnersWhileCapacityRemains() {
        CourseEnrollmentService service = new CourseEnrollmentService(3, PREREQ);

        assertEquals(ENROLLED, service.enroll(eligible("a")).status());
        assertEquals(1, service.enrolledCount());
        assertEquals(ENROLLED, service.enroll(eligible("b")).status());
        assertEquals(ENROLLED, service.enroll(eligible("c")).status());

        assertEquals(3, service.enrolledCount());
        assertEquals(List.of("a", "b", "c"), service.enrolledIds());
        assertTrue(service.waitlistIds().isEmpty());
        assertInvariants(service, 3);
    }

    // 2. full capacity

    @Test
    void waitlistsEligibleLearnersInArrivalOrderWhenFull() {
        CourseEnrollmentService service = new CourseEnrollmentService(2, PREREQ);
        service.enroll(eligible("a"));
        service.enroll(eligible("b"));

        assertEquals(WAITLISTED, service.enroll(eligible("c")).status());
        assertEquals(WAITLISTED, service.enroll(eligible("d")).status());
        assertEquals(WAITLISTED, service.enroll(eligible("e")).status());

        assertEquals(List.of("a", "b"), service.enrolledIds());
        assertEquals(List.of("c", "d", "e"), service.waitlistIds());
        assertTrue(service.isWaitlisted("d"));
        assertFalse(service.isEnrolled("d"));
        assertInvariants(service, 2);
    }

    // 3. duplicates

    @Test
    void secondEnrollOfEnrolledLearnerIsAlreadyEnrolled() {
        CourseEnrollmentService service = new CourseEnrollmentService(2, PREREQ);
        service.enroll(eligible("a"));
        State before = state(service);

        assertEquals(ALREADY_ENROLLED, service.enroll(eligible("a")).status());

        assertEquals(before, state(service));
        assertEquals(1, service.enrolledCount());
        assertInvariants(service, 2);
    }

    @Test
    void secondEnrollOfWaitlistedLearnerIsAlreadyWaitlisted() {
        CourseEnrollmentService service = fullServiceWithWaiter();
        State before = state(service);

        assertEquals(ALREADY_WAITLISTED, service.enroll(eligible("b")).status());

        assertEquals(before, state(service));
        assertEquals(List.of("b"), service.waitlistIds());
        assertInvariants(service, 1);
    }

    @Test
    void duplicateIsDetectedByIdEvenWithDifferentCourses() {
        CourseEnrollmentService service = fullServiceWithWaiter();
        State before = state(service);

        // "a" and "b" re-request without the prerequisite: still duplicates, not NOT_ELIGIBLE.
        assertEquals(ALREADY_ENROLLED, service.enroll(learner("a")).status());
        assertEquals(ALREADY_WAITLISTED, service.enroll(learner("b")).status());

        assertEquals(before, state(service));
    }

    // 4. FIFO promotion

    @Test
    void dropPromotesEarliestWaitlistedLearner() {
        CourseEnrollmentService service = new CourseEnrollmentService(1, PREREQ);
        service.enroll(eligible("a"));
        service.enroll(eligible("b"));
        service.enroll(eligible("c"));

        EnrollmentResult result = service.drop("a");

        assertEquals(DROPPED, result.status());
        assertEquals("b", result.promotedLearnerId());
        assertEquals(List.of("b"), service.enrolledIds());
        assertEquals(List.of("c"), service.waitlistIds());
        assertInvariants(service, 1);
    }

    @Test
    void promotionOrderOverSeveralDropsEqualsJoinOrder() {
        CourseEnrollmentService service = new CourseEnrollmentService(1, PREREQ);
        service.enroll(eligible("a"));
        for (String id : List.of("b", "c", "d", "e")) {
            service.enroll(eligible(id));
        }

        assertEquals("b", service.drop("a").promotedLearnerId());
        assertEquals("c", service.drop("b").promotedLearnerId());
        assertEquals("d", service.drop("c").promotedLearnerId());
        assertEquals("e", service.drop("d").promotedLearnerId());
        assertEquals(List.of("e"), service.enrolledIds());
        assertTrue(service.waitlistIds().isEmpty());
        assertInvariants(service, 1);
    }

    @Test
    void dropFreesOnlyOneSeatSoOnlyOneLearnerIsPromoted() {
        CourseEnrollmentService service = new CourseEnrollmentService(2, PREREQ);
        for (String id : List.of("a", "b", "c", "d")) {
            service.enroll(eligible(id));
        }

        service.drop("a");

        assertEquals(List.of("b", "c"), service.enrolledIds());
        assertEquals(List.of("d"), service.waitlistIds());
        assertInvariants(service, 2);
    }

    // 5. prerequisite

    @Test
    void ineligibleLearnerIsRejectedWhileSeatsAreFree() {
        CourseEnrollmentService service = new CourseEnrollmentService(2, PREREQ);

        assertEquals(NOT_ELIGIBLE, service.enroll(learner("x", "MATH200")).status());
        assertEquals(NOT_ELIGIBLE, service.enroll(learner("y")).status());

        assertTrue(service.enrolledIds().isEmpty());
        assertTrue(service.waitlistIds().isEmpty());
    }

    @Test
    void ineligibleLearnerIsRejectedAndNotWaitlistedWhenFull() {
        CourseEnrollmentService service = fullServiceWithWaiter();
        State before = state(service);

        assertEquals(NOT_ELIGIBLE, service.enroll(learner("x", "MATH200")).status());

        assertEquals(before, state(service));
        assertFalse(service.isWaitlisted("x"));
        assertInvariants(service, 1);
    }

    @Test
    void nullBlankOrWhitespacePrerequisiteMakesEveryoneEligible() {
        for (String required : Arrays.asList((String) null, "", "   ")) {
            CourseEnrollmentService service = new CourseEnrollmentService(2, required);

            assertEquals(ENROLLED, service.enroll(learner("a")).status(), "required=" + required);
            assertEquals(ENROLLED, service.enroll(learner("b", "ANYTHING")).status());
            assertEquals(WAITLISTED, service.enroll(learner("c")).status());
        }
    }

    // 6. drop edge cases

    @Test
    void dropOfUnknownIdIsNotEnrolledAndChangesNothing() {
        CourseEnrollmentService service = fullServiceWithWaiter();
        State before = state(service);

        EnrollmentResult result = service.drop("ghost");

        assertEquals(NOT_ENROLLED, result.status());
        assertNull(result.promotedLearnerId());
        assertEquals(before, state(service));
    }

    @Test
    void repeatedDropIsNotEnrolledAndPromotesNobodyExtra() {
        CourseEnrollmentService service = new CourseEnrollmentService(1, PREREQ);
        service.enroll(eligible("a"));
        service.enroll(eligible("b"));
        service.enroll(eligible("c"));
        assertEquals("b", service.drop("a").promotedLearnerId());
        State afterFirstDrop = state(service);

        EnrollmentResult second = service.drop("a");

        assertEquals(NOT_ENROLLED, second.status());
        assertTrue(second.promoted().isEmpty());
        assertEquals(afterFirstDrop, state(service));
        assertInvariants(service, 1);
    }

    @Test
    void dropWithNullOrBlankIdIsInvalidAndChangesNothing() {
        CourseEnrollmentService service = fullServiceWithWaiter();
        State before = state(service);

        for (String id : Arrays.asList(null, "", "   ")) {
            EnrollmentResult result = service.drop(id);
            assertEquals(INVALID_REQUEST, result.status(), "id=" + id);
            assertNull(result.promotedLearnerId());
        }

        assertEquals(before, state(service));
    }

    @Test
    void dropOfWaitlistedLearnerRemovesThemWithoutPromotionAndKeepsOrderOfTheRest() {
        CourseEnrollmentService service = new CourseEnrollmentService(1, PREREQ);
        for (String id : List.of("a", "b", "c", "d")) {
            service.enroll(eligible(id));
        }

        EnrollmentResult result = service.drop("c");

        assertEquals(REMOVED_FROM_WAITLIST, result.status());
        assertTrue(result.promoted().isEmpty());
        assertEquals(List.of("a"), service.enrolledIds());
        assertEquals(List.of("b", "d"), service.waitlistIds());
        assertInvariants(service, 1);
    }

    @Test
    void dropOfEnrolledLearnerWithEmptyWaitlistLeavesSeatFree() {
        CourseEnrollmentService service = new CourseEnrollmentService(2, PREREQ);
        service.enroll(eligible("a"));

        EnrollmentResult result = service.drop("a");

        assertEquals(DROPPED, result.status());
        assertTrue(result.promoted().isEmpty());
        assertEquals(0, service.enrolledCount());
        assertEquals(ENROLLED, service.enroll(eligible("b")).status());
    }

    // 7. invalid enroll and constructor

    @Test
    void enrollOfNullLearnerIsInvalid() {
        assertInvalidEnrollChangesNothing(null);
    }

    @Test
    void enrollWithNullIdIsInvalid() {
        assertInvalidEnrollChangesNothing(new Learner(null, Set.of(PREREQ)));
    }

    @Test
    void enrollWithBlankIdIsInvalid() {
        assertInvalidEnrollChangesNothing(new Learner("", Set.of(PREREQ)));
        assertInvalidEnrollChangesNothing(new Learner("   ", Set.of(PREREQ)));
    }

    @Test
    void enrollWithNullCourseSetIsInvalid() {
        assertInvalidEnrollChangesNothing(new Learner("x", null));
    }

    @Test
    void enrollWithNullElementInCourseSetIsInvalid() {
        assertInvalidEnrollChangesNothing(new Learner("x", new HashSet<>(Arrays.asList(PREREQ, null))));
    }

    @Test
    void invalidEnrollIsRejectedEvenWhenNoPrerequisiteIsRequired() {
        CourseEnrollmentService service = new CourseEnrollmentService(2, (String) null);

        assertEquals(INVALID_REQUEST, service.enroll(new Learner("x", null)).status());
        assertEquals(0, service.enrolledCount());
    }

    @Test
    void constructorRejectsCapacityBelowOne() {
        assertThrows(IllegalArgumentException.class, () -> new CourseEnrollmentService(0, PREREQ));
        assertThrows(IllegalArgumentException.class, () -> new CourseEnrollmentService(-1, PREREQ));
        assertThrows(IllegalArgumentException.class, () -> new CourseEnrollmentService(0, l -> true));
    }

    private static void assertInvalidEnrollChangesNothing(Learner request) {
        CourseEnrollmentService service = fullServiceWithWaiter();
        State before = state(service);

        EnrollmentResult result = service.enroll(request);

        assertEquals(INVALID_REQUEST, result.status());
        assertNull(result.promotedLearnerId());
        assertEquals(before, state(service));
        assertInvariants(service, 1);
    }

    // 8. re-join

    @Test
    void droppedLearnerCanEnrollAgainWhenSeatIsFree() {
        CourseEnrollmentService service = new CourseEnrollmentService(1, PREREQ);
        service.enroll(eligible("a"));
        service.drop("a");

        assertEquals(ENROLLED, service.enroll(eligible("a")).status());
        assertEquals(List.of("a"), service.enrolledIds());
    }

    @Test
    void learnerRemovedFromWaitlistRejoinsAtTheTail() {
        CourseEnrollmentService service = new CourseEnrollmentService(1, PREREQ);
        for (String id : List.of("a", "b", "c")) {
            service.enroll(eligible(id));
        }
        service.drop("b");

        assertEquals(WAITLISTED, service.enroll(eligible("b")).status());

        assertEquals(List.of("c", "b"), service.waitlistIds());
        assertInvariants(service, 1);
    }

    @Test
    void droppedEnrolledLearnerGoesToWaitlistTailWhenFullAgain() {
        CourseEnrollmentService service = new CourseEnrollmentService(1, PREREQ);
        for (String id : List.of("a", "b", "c")) {
            service.enroll(eligible(id));
        }
        service.drop("a"); // b promoted, c still waiting

        assertEquals(WAITLISTED, service.enroll(eligible("a")).status());

        assertEquals(List.of("b"), service.enrolledIds());
        assertEquals(List.of("c", "a"), service.waitlistIds());
        assertInvariants(service, 1);
    }

    // 9. skip-and-retain (reachable only through the predicate seam)

    @Test
    void ineligibleLearnerAtFrontIsSkippedAndKeepsPositionUntilEligibleAgainThenIsPromoted() {
        CourseEnrollmentService service = serviceWithBlocklist(1);
        service.enroll(learner("a"));
        for (String id : List.of("b", "c", "d")) {
            service.enroll(learner(id));
        }
        blocked.add("b");

        EnrollmentResult first = service.drop("a");

        assertEquals(DROPPED, first.status());
        assertEquals("c", first.promotedLearnerId());
        assertEquals(List.of("c"), service.enrolledIds());
        assertEquals(List.of("b", "d"), service.waitlistIds()); // b keeps position 0
        assertInvariants(service, 1);

        blocked.remove("b");
        EnrollmentResult second = service.drop("c");

        assertEquals("b", second.promotedLearnerId()); // earlier than d
        assertEquals(List.of("b"), service.enrolledIds());
        assertEquals(List.of("d"), service.waitlistIds());
        assertInvariants(service, 1);
    }

    @Test
    void dropPromotesNobodyWhenEveryWaitlistedLearnerIsIneligible() {
        CourseEnrollmentService service = serviceWithBlocklist(1);
        service.enroll(learner("a"));
        service.enroll(learner("b"));
        service.enroll(learner("c"));
        blocked.addAll(List.of("b", "c"));

        EnrollmentResult result = service.drop("a");

        assertEquals(DROPPED, result.status());
        assertTrue(result.promoted().isEmpty());
        assertEquals(0, service.enrolledCount());
        assertEquals(List.of("b", "c"), service.waitlistIds());
    }

    @Test
    void enrolledLearnerKeepsSeatWhenEligibilityIsLaterRevoked() {
        CourseEnrollmentService service = serviceWithBlocklist(1);
        service.enroll(learner("a"));
        blocked.add("a");

        assertEquals(ALREADY_ENROLLED, service.enroll(learner("a")).status());
        assertEquals(List.of("a"), service.enrolledIds());
    }

    // 10 and 11. skipped learner becomes eligible again while a seat is free

    @Test
    void newcomerDoesNotJumpTheQueueAheadOfSkippedLearnerWhoIsEligibleAgain() {
        CourseEnrollmentService service = serviceWithBlocklist(1);
        service.enroll(learner("A"));
        service.enroll(learner("B")); // waitlisted
        blocked.add("B");
        assertTrue(service.drop("A").promoted().isEmpty()); // B skipped, seat now free but unclaimed
        assertEquals(0, service.enrolledCount());
        blocked.remove("B");

        EnrollmentResult result = service.enroll(learner("C"));

        assertEquals(WAITLISTED, result.status());
        assertEquals(List.of("B"), service.enrolledIds());
        assertEquals(List.of("C"), service.waitlistIds());
        assertInvariants(service, 1);
    }

    @Test
    void enrollCallCanPromoteAnotherWaitlistedLearnerAsSideEffectAndStillSeatTheNewcomer() {
        CourseEnrollmentService service = serviceWithBlocklist(2);
        service.enroll(learner("A"));
        service.enroll(learner("B"));
        service.enroll(learner("C")); // waitlisted
        blocked.add("C");
        service.drop("A");
        service.drop("B"); // two free seats, C skipped and retained
        assertEquals(List.of("C"), service.waitlistIds());
        blocked.remove("C");

        EnrollmentResult result = service.enroll(learner("E"));

        assertEquals(ENROLLED, result.status());
        assertEquals(List.of("C", "E"), service.enrolledIds()); // C was promoted first, by E's enroll
        assertTrue(service.waitlistIds().isEmpty());
        assertInvariants(service, 2);
    }

    // large waitlist

    @Test
    void thousandLearnersArePromotedOneByOneInExactJoinOrder() {
        int capacity = 3;
        int learners = 1000;
        CourseEnrollmentService service = new CourseEnrollmentService(capacity, PREREQ);
        List<String> joinOrder = new ArrayList<>();
        for (int i = 0; i < learners; i++) {
            joinOrder.add("L" + i);
            service.enroll(eligible("L" + i));
        }
        assertEquals(joinOrder.subList(0, capacity), service.enrolledIds());
        assertEquals(joinOrder.subList(capacity, learners), service.waitlistIds());
        assertInvariants(service, capacity);

        // Seats are held in join order, so dropping the earliest holder frees a seat for the next waiter.
        for (int i = 0; i < learners - capacity; i++) {
            EnrollmentResult result = service.drop(joinOrder.get(i));

            assertEquals(DROPPED, result.status());
            assertEquals(joinOrder.get(i + capacity), result.promotedLearnerId(), "promotion " + i);
        }

        assertTrue(service.waitlistIds().isEmpty());
        assertEquals(joinOrder.subList(learners - capacity, learners), service.enrolledIds());
        assertInvariants(service, capacity);
    }

    // extra

    @Test
    void promotedResultExposesIdThroughOptionalAccessor() {
        CourseEnrollmentService service = fullServiceWithWaiter();

        EnrollmentResult result = service.drop("a");

        assertEquals("b", result.promoted().orElseThrow());
        assertFalse(service.isEnrolled("a"));
        assertTrue(service.isEnrolled("b"));
    }
}
