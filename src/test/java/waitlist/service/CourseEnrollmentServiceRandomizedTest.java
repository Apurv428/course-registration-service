package waitlist.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static waitlist.dto.EnrollmentStatus.ALREADY_ENROLLED;
import static waitlist.dto.EnrollmentStatus.ALREADY_WAITLISTED;
import static waitlist.dto.EnrollmentStatus.ENROLLED;
import static waitlist.dto.EnrollmentStatus.NOT_ELIGIBLE;
import static waitlist.dto.EnrollmentStatus.NOT_ENROLLED;
import static waitlist.dto.EnrollmentStatus.REMOVED_FROM_WAITLIST;
import static waitlist.dto.EnrollmentStatus.WAITLISTED;
import static waitlist.service.ServiceTestSupport.assertInvariants;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import waitlist.dto.EnrollmentResult;
import waitlist.model.Learner;

/** Single-threaded comparison of the service against a simple reference model. */
class CourseEnrollmentServiceRandomizedTest {

    private static final String REQUIRED = "CS101";
    private static final int ID_POOL = 15;
    private static final int OPERATIONS = 500;

    /**
     * Reference model written from the documented rules, not from the service code: a seat list,
     * a FIFO queue of eligible waiters, and promotion of the first waiter when a seat opens.
     */
    private static final class Model {
        private final int capacity;
        final List<String> seated = new ArrayList<>();
        final List<String> queue = new ArrayList<>();

        Model(int capacity) {
            this.capacity = capacity;
        }

        EnrollmentResult enroll(String id, boolean eligible) {
            if (seated.contains(id)) {
                return EnrollmentResult.of(ALREADY_ENROLLED);
            }
            if (queue.contains(id)) {
                return EnrollmentResult.of(ALREADY_WAITLISTED);
            }
            if (!eligible) {
                return EnrollmentResult.of(NOT_ELIGIBLE);
            }
            if (seated.size() < capacity) {
                seated.add(id);
                return EnrollmentResult.of(ENROLLED);
            }
            queue.add(id);
            return EnrollmentResult.of(WAITLISTED);
        }

        EnrollmentResult drop(String id) {
            if (seated.remove(id)) {
                String next = queue.isEmpty() ? null : queue.remove(0);
                if (next != null) {
                    seated.add(next);
                }
                return EnrollmentResult.dropped(next);
            }
            if (queue.remove(id)) {
                return EnrollmentResult.of(REMOVED_FROM_WAITLIST);
            }
            return EnrollmentResult.of(NOT_ENROLLED);
        }
    }

    static Stream<Long> seeds() {
        return LongStream.rangeClosed(1, 20).boxed();
    }

    @ParameterizedTest(name = "seed {0}")
    @MethodSource("seeds")
    void serviceMatchesReferenceModelAfterEveryRandomOperation(long seed) {
        Random random = new Random(seed);
        int capacity = 1 + random.nextInt(4);
        CourseEnrollmentService service = new CourseEnrollmentService(capacity, REQUIRED);
        Model model = new Model(capacity);

        for (int step = 1; step <= OPERATIONS; step++) {
            String where = "seed=" + seed + " capacity=" + capacity + " step=" + step;
            EnrollmentResult actual;
            EnrollmentResult expected;
            if (random.nextInt(100) < 55) {
                String id = "L" + random.nextInt(ID_POOL);
                boolean eligible = random.nextInt(100) < 75;
                where += " enroll " + id + " eligible=" + eligible;
                actual = service.enroll(new Learner(id, Set.of(eligible ? REQUIRED : "MATH200")));
                expected = model.enroll(id, eligible);
            } else {
                String id = "L" + random.nextInt(ID_POOL + 5); // the last five ids are never enrolled
                where += " drop " + id;
                actual = service.drop(id);
                expected = model.drop(id);
            }

            assertEquals(expected, actual, where + ": result");
            assertEquals(model.seated, service.enrolledIds(), where + ": enrolled");
            assertEquals(model.queue, service.waitlistIds(), where + ": waitlist");
            assertInvariants(service, capacity);
        }
    }
}
