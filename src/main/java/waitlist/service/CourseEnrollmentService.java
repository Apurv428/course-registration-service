package waitlist.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import java.util.function.Supplier;
import waitlist.dto.EnrollmentResult;
import waitlist.dto.EnrollmentStatus;
import waitlist.model.Learner;

/**
 * Enrollment with a FIFO waitlist for a single course offering, held in memory.
 *
 * <p>Requests never throw; they return an {@link EnrollmentResult}. Every operation, reads
 * included, runs under one lock, so each call sees a consistent state. Two separate reads are
 * not atomic together.
 *
 * <p>Eligibility is checked when a learner enrolls and again when they are promoted. With the
 * public constructor a learner's eligibility cannot change in between, so a waitlisted learner
 * is always eligible and the skip-and-retain path in promotion never runs. It is reachable only
 * through the package-private {@link Predicate} constructor, which exists so tests can exercise it.
 */
public class CourseEnrollmentService {

    private final int capacity;
    private final Predicate<Learner> eligibility;
    private final ReentrantLock lock = new ReentrantLock();
    private final Set<String> enrolled = new LinkedHashSet<>();
    private final Map<String, Learner> waitlist = new LinkedHashMap<>();

    /**
     * @param capacity         number of seats; must be at least 1
     * @param requiredCourseId prerequisite course id; null or blank means everyone is eligible
     * @throws IllegalArgumentException if {@code capacity < 1}
     */
    public CourseEnrollmentService(int capacity, String requiredCourseId) {
        this(capacity, learner -> requiredCourseId == null
                || requiredCourseId.isBlank()
                || learner.completedCourseIds().contains(requiredCourseId));
    }

    /** Test seam: replaces the prerequisite rule with an arbitrary eligibility predicate. */
    CourseEnrollmentService(int capacity, Predicate<Learner> eligibility) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be at least 1: " + capacity);
        }
        this.capacity = capacity;
        this.eligibility = Objects.requireNonNull(eligibility);
    }

    public EnrollmentResult enroll(Learner learner) {
        if (!isValid(learner)) {
            return EnrollmentResult.of(EnrollmentStatus.INVALID_REQUEST);
        }
        return locked(() -> {
            String id = learner.id();
            if (enrolled.contains(id)) {
                return EnrollmentResult.of(EnrollmentStatus.ALREADY_ENROLLED);
            }
            if (waitlist.containsKey(id)) {
                return EnrollmentResult.of(EnrollmentStatus.ALREADY_WAITLISTED);
            }
            if (!eligibility.test(learner)) {
                return EnrollmentResult.of(EnrollmentStatus.NOT_ELIGIBLE);
            }
            // A seat can be free while the waitlist is non-empty if a skipped learner became
            // eligible again after the last drop. Seating the newcomer directly would jump
            // that learner; waitlisting the newcomer without promoting would leave the seat
            // stuck. So fill free seats from the waitlist first, then take a seat only if
            // one is still free.
            while (promoteNext() != null) {
                // keep promoting until the seats are full or nobody eligible is left
            }
            if (enrolled.size() < capacity) {
                enrolled.add(id);
                return EnrollmentResult.of(EnrollmentStatus.ENROLLED);
            }
            waitlist.put(id, learner);
            return EnrollmentResult.of(EnrollmentStatus.WAITLISTED);
        });
    }

    public EnrollmentResult drop(String learnerId) {
        if (learnerId == null || learnerId.isBlank()) {
            return EnrollmentResult.of(EnrollmentStatus.INVALID_REQUEST);
        }
        return locked(() -> {
            if (enrolled.remove(learnerId)) {
                return EnrollmentResult.dropped(promoteNext());
            }
            if (waitlist.remove(learnerId) != null) {
                return EnrollmentResult.of(EnrollmentStatus.REMOVED_FROM_WAITLIST);
            }
            return EnrollmentResult.of(EnrollmentStatus.NOT_ENROLLED);
        });
    }

    public boolean isEnrolled(String learnerId) {
        return locked(() -> enrolled.contains(learnerId));
    }

    public boolean isWaitlisted(String learnerId) {
        return locked(() -> waitlist.containsKey(learnerId));
    }

    public int enrolledCount() {
        return locked(enrolled::size);
    }

    /** Enrolled learner ids in enrollment order. */
    public List<String> enrolledIds() {
        return locked(() -> new ArrayList<>(enrolled));
    }

    /** Waitlisted learner ids, front of the queue first. */
    public List<String> waitlistIds() {
        return locked(() -> new ArrayList<>(waitlist.keySet()));
    }

    /**
     * Moves the first eligible waitlisted learner into a free seat. Ineligible learners ahead of
     * them are skipped and keep their position. Caller must hold the lock.
     *
     * @return the promoted learner's id, or null if no seat is free or nobody is eligible
     */
    private String promoteNext() {
        if (enrolled.size() >= capacity) {
            return null;
        }
        Iterator<Learner> queue = waitlist.values().iterator();
        while (queue.hasNext()) {
            Learner candidate = queue.next();
            if (eligibility.test(candidate)) {
                queue.remove();
                enrolled.add(candidate.id());
                return candidate.id();
            }
        }
        return null;
    }

    private static boolean isValid(Learner learner) {
        return learner != null
                && learner.id() != null
                && !learner.id().isBlank()
                && learner.completedCourseIds() != null
                && !learner.completedCourseIds().contains(null);
    }

    private <T> T locked(Supplier<T> action) {
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }
}
