package waitlist.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import waitlist.model.Learner;

/** Helpers shared by the service tests. */
final class ServiceTestSupport {

    static final String PREREQ = "CS101";

    private ServiceTestSupport() {
    }

    record State(List<String> enrolled, List<String> waitlist) {
    }

    static Learner learner(String id, String... completedCourses) {
        return new Learner(id, Set.of(completedCourses));
    }

    /** A learner who meets {@link #PREREQ}. */
    static Learner eligible(String id) {
        return learner(id, PREREQ);
    }

    static State state(CourseEnrollmentService service) {
        return new State(service.enrolledIds(), service.waitlistIds());
    }

    /** Checks the invariants that must hold whenever no operation is in flight. */
    static void assertInvariants(CourseEnrollmentService service, int capacity) {
        List<String> enrolled = service.enrolledIds();
        List<String> waitlist = service.waitlistIds();
        assertTrue(enrolled.size() <= capacity, "enrolled " + enrolled + " exceeds capacity " + capacity);
        assertEquals(enrolled.size(), service.enrolledCount());
        assertEquals(enrolled.size(), new HashSet<>(enrolled).size(), "duplicate in enrolled " + enrolled);
        assertEquals(waitlist.size(), new HashSet<>(waitlist).size(), "duplicate in waitlist " + waitlist);
        assertTrue(Collections.disjoint(enrolled, waitlist),
                "learner both enrolled and waitlisted: " + enrolled + " / " + waitlist);
    }
}
