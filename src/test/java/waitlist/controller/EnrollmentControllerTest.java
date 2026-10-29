package waitlist.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static waitlist.dto.EnrollmentStatus.DROPPED;
import static waitlist.dto.EnrollmentStatus.ENROLLED;
import static waitlist.dto.EnrollmentStatus.INVALID_REQUEST;
import static waitlist.dto.EnrollmentStatus.WAITLISTED;

import java.util.Set;
import org.junit.jupiter.api.Test;
import waitlist.dto.EnrollmentResult;
import waitlist.model.Learner;
import waitlist.service.CourseEnrollmentService;

class EnrollmentControllerTest {

    /** Hand-written stub (no mocking library) that records its arguments and returns canned results. */
    private static final class RecordingService extends CourseEnrollmentService {
        final EnrollmentResult enrollResult = EnrollmentResult.of(WAITLISTED);
        final EnrollmentResult dropResult = EnrollmentResult.dropped("next");
        Learner enrolledLearner;
        String droppedId;

        RecordingService() {
            super(1, (String) null);
        }

        @Override
        public EnrollmentResult enroll(Learner learner) {
            enrolledLearner = learner;
            return enrollResult;
        }

        @Override
        public EnrollmentResult drop(String learnerId) {
            droppedId = learnerId;
            return dropResult;
        }
    }

    @Test
    void enrollBuildsLearnerFromRawInputsAndReturnsServiceResultUnchanged() {
        RecordingService service = new RecordingService();

        EnrollmentResult result = new EnrollmentController(service).enroll("ann", Set.of("CS101", "CS102"));

        assertSame(service.enrollResult, result);
        assertEquals("ann", service.enrolledLearner.id());
        assertEquals(Set.of("CS101", "CS102"), service.enrolledLearner.completedCourseIds());
    }

    @Test
    void dropDelegatesIdAndReturnsServiceResultUnchanged() {
        RecordingService service = new RecordingService();

        EnrollmentResult result = new EnrollmentController(service).drop("ann");

        assertSame(service.dropResult, result);
        assertEquals("ann", service.droppedId);
    }

    @Test
    void nullIdOrNullCourseSetYieldsInvalidRequestFromTheRealService() {
        CourseEnrollmentService service = new CourseEnrollmentService(1, (String) null);
        EnrollmentController controller = new EnrollmentController(service);

        assertEquals(INVALID_REQUEST, controller.enroll(null, Set.of()).status());
        assertEquals(INVALID_REQUEST, controller.enroll("ann", null).status());

        assertEquals(0, service.enrolledCount());
        assertTrue(service.waitlistIds().isEmpty());
    }

    @Test
    void enrollAndDropWorkEndToEndThroughTheController() {
        CourseEnrollmentService service = new CourseEnrollmentService(1, "CS101");
        EnrollmentController controller = new EnrollmentController(service);

        assertEquals(ENROLLED, controller.enroll("ann", Set.of("CS101")).status());
        assertEquals(WAITLISTED, controller.enroll("bob", Set.of("CS101")).status());

        EnrollmentResult dropped = controller.drop("ann");

        assertEquals(DROPPED, dropped.status());
        assertEquals("bob", dropped.promotedLearnerId());
    }
}
