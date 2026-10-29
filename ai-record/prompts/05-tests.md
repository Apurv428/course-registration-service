# Round 5: Tests (Step 3)

````text
## Step 3 (TEST) only
Write JUnit 5 tests, no mocking libraries. Layout:
- src/test/java/waitlist/service/CourseEnrollmentServiceTest.java (same package, to reach the Predicate seam)
- src/test/java/waitlist/service/CourseEnrollmentServiceConcurrencyTest.java
- src/test/java/waitlist/controller/EnrollmentControllerTest.java (delegation only: enroll builds a Learner and returns the service result; null id/null course set -> INVALID_REQUEST; drop delegates)
- src/test/java/waitlist/model/LearnerTest.java (defensive copy: mutating the original set afterwards does not change the Learner; null set and null id do not throw; the exposed set is unmodifiable)
Use descriptive method names and small helpers (learner(id, courses...), assertInvariants(service)).

Required service coverage:
1. Capacity available: enroll succeeds up to capacity; enrolledCount correct.
2. Full capacity: next eligible learner is WAITLISTED; waitlistIds order correct.
3. Duplicates: enroll twice -> ALREADY_ENROLLED; twice while waitlisted -> ALREADY_WAITLISTED; no duplicate records in either list.
4. FIFO promotion: drop promotes the earliest waitlisted learner; over several drops, promotion order equals join order.
5. Prerequisite: ineligible -> NOT_ELIGIBLE both when seats are free and when full, and in neither list. Null and blank required course make everyone eligible (cast (String) null).
6. Drop edge cases: unknown id, repeated drop (NOT_ENROLLED), null/blank id (INVALID_REQUEST), waitlisted drop (REMOVED_FROM_WAITLIST, no promotion, order of the rest preserved), enrolled drop with empty waitlist (DROPPED, promoted empty). Assert state unchanged after every failed call.
7. Invalid enroll: null learner, null id, blank id, null course set, set containing null -> INVALID_REQUEST, state unchanged. Constructor: capacity 0 and negative -> IllegalArgumentException.
8. Re-join: a dropped learner can enroll again; goes to the waitlist tail when full.
9. Skip-and-retain via the Predicate seam: front learner made ineligible -> drop promotes the next; skipped learner keeps position 0; once eligible again, a later drop promotes them.
10. Stuck-seat scenario (A enrolled, B waitlisted, B ineligible, A drops, B eligible again, C enrolls): B is enrolled and C is WAITLISTED. C must not jump the queue.
11. Side-effect case: enroll() that promotes another waitlisted learner (document with a test name saying so).
12. Concurrency (ExecutorService, CountDownLatch start gate, bounded awaitTermination so it cannot hang): (a) capacity 5, 50 distinct learners -> exactly 5 enrolled, 45 waitlisted, FIFO-consistent, no overlap/duplicates; (b) concurrent mix of drops and duplicate enrolls on the same ids. Check cross-read invariants only after termination.

Check if there is any other test that needs to be added.

## Verification (required)
- Run `mvn test`; show the full summary.
- Mutation check, one at a time, reverting after each and confirming green again: (a) remove the duplicate check in enroll, (b) make promoteNext take the last waitlisted learner, (c) remove the lock from enroll/drop, (d) remove the "no queue-jumping" promotion loop in enroll, (e) remove the promotion-time eligibility check. Report which tests failed for each. Add a test for any uncaught mutation.
- Run the concurrency tests 20 times in a loop; report failures.
- List deviations. Do not start docs. Stop and wait.
````

[Prompt log row 6](../AI_USAGE.md#2-prompt-log) · Decision: Accepted all five deviations; author re-ran the tests independently.
