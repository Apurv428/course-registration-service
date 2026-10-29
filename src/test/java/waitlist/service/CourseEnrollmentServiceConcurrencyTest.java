package waitlist.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static waitlist.dto.EnrollmentStatus.ALREADY_ENROLLED;
import static waitlist.dto.EnrollmentStatus.ALREADY_WAITLISTED;
import static waitlist.dto.EnrollmentStatus.DROPPED;
import static waitlist.dto.EnrollmentStatus.ENROLLED;
import static waitlist.dto.EnrollmentStatus.NOT_ENROLLED;
import static waitlist.dto.EnrollmentStatus.REMOVED_FROM_WAITLIST;
import static waitlist.dto.EnrollmentStatus.WAITLISTED;
import static waitlist.service.ServiceTestSupport.PREREQ;
import static waitlist.service.ServiceTestSupport.assertInvariants;
import static waitlist.service.ServiceTestSupport.eligible;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import waitlist.dto.EnrollmentResult;
import waitlist.dto.EnrollmentStatus;

/** Invariants are checked only after every task has terminated, never while threads still run. */
class CourseEnrollmentServiceConcurrencyTest {

    private static final int ROUNDS = 20;

    @Test
    void fiftyConcurrentEnrollsFillExactlyCapacityAndWaitlistTheRestInFifoOrder() throws Exception {
        int capacity = 5;
        int learners = 50;
        for (int round = 0; round < ROUNDS; round++) {
            CourseEnrollmentService service = new CourseEnrollmentService(capacity, PREREQ);
            List<Callable<EnrollmentResult>> tasks = new ArrayList<>();
            for (int i = 0; i < learners; i++) {
                String id = "L" + i;
                tasks.add(() -> service.enroll(eligible(id)));
            }

            List<EnrollmentResult> results = runAtOnce(tasks);

            Set<String> grantedSeat = new HashSet<>();
            Set<String> grantedWaitlist = new HashSet<>();
            for (int i = 0; i < learners; i++) {
                EnrollmentStatus status = results.get(i).status();
                assertTrue(status == ENROLLED || status == WAITLISTED, "unexpected " + status);
                (status == ENROLLED ? grantedSeat : grantedWaitlist).add("L" + i);
            }
            assertEquals(capacity, grantedSeat.size());
            assertEquals(learners - capacity, grantedWaitlist.size());
            assertEquals(grantedSeat, new HashSet<>(service.enrolledIds()));
            assertEquals(grantedWaitlist, new HashSet<>(service.waitlistIds()));
            assertInvariants(service, capacity);

            // The recorded waitlist order is the FIFO order: draining the seats promotes it front first.
            List<String> queue = service.waitlistIds();
            List<String> promoted = new ArrayList<>();
            for (String seated : service.enrolledIds()) {
                promoted.add(service.drop(seated).promotedLearnerId());
            }
            assertEquals(queue.subList(0, capacity), promoted);
            assertInvariants(service, capacity);
        }
    }

    @Test
    void concurrentDuplicateEnrollsAndDropsOnSameIdsKeepStateConsistent() throws Exception {
        int capacity = 3;
        int learners = 20;
        for (int round = 0; round < ROUNDS; round++) {
            CourseEnrollmentService service = new CourseEnrollmentService(capacity, PREREQ);
            for (int i = 0; i < learners; i++) {
                service.enroll(eligible("L" + i)); // 3 seated, 17 waitlisted
            }
            List<Callable<EnrollmentResult>> tasks = new ArrayList<>();
            for (int i = 0; i < learners; i++) {
                String id = "L" + i;
                tasks.add(() -> service.enroll(eligible(id)));
                tasks.add(() -> service.enroll(eligible(id)));
                if (i % 2 == 0) {
                    tasks.add(() -> service.drop(id));
                    tasks.add(() -> service.drop(id));
                }
            }

            List<EnrollmentResult> results = runAtOnce(tasks);

            Set<EnrollmentStatus> allowed = Set.of(ENROLLED, WAITLISTED, ALREADY_ENROLLED, ALREADY_WAITLISTED,
                    DROPPED, REMOVED_FROM_WAITLIST, NOT_ENROLLED);
            for (EnrollmentResult result : results) {
                assertTrue(allowed.contains(result.status()), "unexpected " + result.status());
            }
            assertInvariants(service, capacity);
            // Odd ids are never dropped, so each must still be in exactly one list.
            for (int i = 1; i < learners; i += 2) {
                String id = "L" + i;
                assertTrue(service.isEnrolled(id) ^ service.isWaitlisted(id), id + " lost or double-counted");
            }
            // At least 10 learners remain, so the waitlist is non-empty and every seat must be taken.
            assertEquals(capacity, service.enrolledCount());
        }
    }

    /** Releases all tasks together from a start gate and waits (bounded) for them to finish. */
    private static List<EnrollmentResult> runAtOnce(List<Callable<EnrollmentResult>> tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<EnrollmentResult>> futures = new ArrayList<>();
            for (Callable<EnrollmentResult> task : tasks) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return task.call();
                }));
            }
            start.countDown();
            pool.shutdown();
            assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS), "tasks did not finish in time");
            List<EnrollmentResult> results = new ArrayList<>();
            for (Future<EnrollmentResult> future : futures) {
                results.add(future.get(1, TimeUnit.SECONDS)); // rethrows any task exception
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }
}
