# Design

## Scope and boundary

One course offering, held in memory: enrollment, dropping, and FIFO waitlist promotion.
Out of scope: frontend, course catalog, payments, identity, multi-term registration,
deployment, configurable policy engine, database. There is no HTTP layer and no web framework.

The service takes a `Learner` (id plus completed-course ids). There is no learner registry.

## Assumptions

- **Prerequisite rule.** A learner is eligible if their completed courses contain the offering's
  required course id. A null or blank required id means everyone is eligible.
- **Ineligible learner at the front of the waitlist: skip and retain.** Promotion takes the first
  eligible learner. Skipped learners keep their position and are reconsidered on later promotions.
- **Eligibility is checked at enroll time and again at promotion time.**
- **Duplicates are detected by learner id only.** A second request with the same id and different
  courses is still a duplicate.
- **The waitlist is unbounded.**
- **Dropping a waitlisted learner removes them from the waitlist.** Nobody is promoted.
- **Capacity below 1 is rejected** with `IllegalArgumentException` in the constructor. Capacity
  cannot change afterwards, so a zero-seat course could never open.

## Data model

| Type | Notes |
|---|---|
| `Learner(String id, Set<String> completedCourseIds)` | Record. A non-null set is copied into an unmodifiable set. Null id or null set is stored as-is and never throws; the service rejects it. |
| `EnrollmentStatus` | Enum, see the table below. |
| `EnrollmentResult(EnrollmentStatus status, String promotedLearnerId)` | Record. `promotedLearnerId` is null unless a drop of an enrolled learner promoted someone. `promoted()` returns it as an `Optional`. |
| service state | `Set<String> enrolled` (a `LinkedHashSet`) and `Map<String, Learner> waitlist` (a `LinkedHashMap` keyed by id). The waitlist stores whole `Learner` objects because promotion re-checks eligibility. |

## API

```java
CourseEnrollmentService(int capacity, String requiredCourseId)   // IllegalArgumentException if capacity < 1
EnrollmentResult enroll(Learner learner)
EnrollmentResult drop(String learnerId)
boolean isEnrolled(String learnerId)
boolean isWaitlisted(String learnerId)
int enrolledCount()
List<String> enrolledIds()      // enrollment order
List<String> waitlistIds()      // front of queue first
```

`waitlist.Main` is a console demo that drives the controller through a fixed scenario; it is not part of the service API.

Requests never throw; they return a result. `EnrollmentController` offers `enroll(String id, Set<String> courses)`
and `drop(String id)` and delegates to the service.

`enroll` checks, in this order: invalid request, already enrolled, already waitlisted, not
eligible, then seat or waitlist.

| Status | Operation | Returned when | `promotedLearnerId` |
|---|---|---|---|
| `ENROLLED` | enroll | seat taken | null |
| `WAITLISTED` | enroll | no seat free after the promotion pass; added to the tail | null |
| `ALREADY_ENROLLED` | enroll | id already holds a seat | null |
| `ALREADY_WAITLISTED` | enroll | id already on the waitlist | null |
| `NOT_ELIGIBLE` | enroll | prerequisite not met; learner is not stored | null |
| `DROPPED` | drop | enrolled learner removed | promoted id, or null if nobody eligible |
| `REMOVED_FROM_WAITLIST` | drop | waitlisted learner removed | null |
| `NOT_ENROLLED` | drop | id in neither list | null |
| `INVALID_REQUEST` | both | null learner, null or blank id, null course set or null element; on drop, null or blank id | null |

## Invariants and how each is enforced

| Invariant | Enforcement |
|---|---|
| enrolled count <= capacity | The only two places that add to `enrolled` are guarded by `enrolled.size() < capacity`. |
| a learner is never both enrolled and waitlisted | `enroll` rejects ids already in either collection before inserting. Promotion removes from the waitlist and adds to `enrolled` under the same lock. |
| no duplicates | `enrolled` is a set and `waitlist` is a map keyed by id. |
| waitlist order is insertion order | `LinkedHashMap` keeps insertion order; promotion scans from the front; an id is never re-inserted while present. |

## Promotion rule

`promoteNext()` does nothing if the course is full. Otherwise it scans the waitlist from the
front, takes the first learner the eligibility rule accepts, moves them to `enrolled`, and returns
their id. Ineligible learners are skipped and left in place.

`drop` of an enrolled learner removes them and calls `promoteNext()` once, so at most one learner
is reported as promoted.

`enroll` runs a promotion loop before deciding where the newcomer goes. Reason: a seat can be free
while the waitlist is non-empty if a skipped learner became eligible again after the last drop.
Seating the newcomer directly would jump that learner. Waitlisting the newcomer without promoting
would leave the seat free with nobody entitled to it taking it. So `enroll` first fills free seats
from the waitlist, then seats the newcomer only if a seat is still free; otherwise they go to the
tail. A consequence is that `enroll` can promote other learners without reporting it in its result.

With the public constructor this situation cannot arise, because eligibility depends only on an
immutable course set and a fixed prerequisite, and only eligible learners can join the waitlist.
Skip-and-retain and the pre-seating promotion loop are reachable only through the package-private
`CourseEnrollmentService(int, Predicate<Learner>)` constructor, which tests use to change
eligibility between calls.

## Concurrency approach

One `ReentrantLock` guards all state. Every public method, reads included, runs under it via a
small `locked` helper, so each call sees a consistent state. Atomicity across two separate calls
is not offered: `enrolledCount()` followed by `waitlistIds()` may observe different states if
other threads are active. There is no `snapshot()` method.

## Test strategy

Plain JUnit 5, no mocking library. Invariants are checked by `ServiceTestSupport.assertInvariants`.

- **Example tests** in `CourseEnrollmentServiceTest` cover each rule, including the skip-and-retain
  and stuck-seat cases through the package-private predicate seam.
- **Randomized test** (`CourseEnrollmentServiceRandomizedTest`): 20 fixed seeds, 500 random
  enroll and drop operations each, single-threaded. A small reference model inside the test (a seat
  list and a FIFO queue) is compared with the service after every operation: status, promoted id,
  `enrolledIds` and `waitlistIds`, then `assertInvariants`. Every assertion message carries the
  seed, step and operation.
- **Large waitlist test** (`thousandLearnersArePromotedOneByOneInExactJoinOrder`): capacity 3,
  1000 learners; all 997 promotions must follow join order.
- **Concurrency tests** use an `ExecutorService`, a start-gate `CountDownLatch`, a bounded
  `awaitTermination`, and check cross-read invariants only after all tasks finish. Each repeats its
  scenario 20 times with a fresh service.

Mutation checks run during development (each applied once and reverted): removing the duplicate
check, promoting the last waitlisted learner instead of the first, removing the lock from
`enroll` and `drop`, removing the promotion loop in `enroll`, and removing the promotion-time
eligibility check. Each made at least one test fail. The randomized test alone catches the first
two but not the removal of the promotion loop: with the public constructor that loop never has
anything to promote, so only the seam tests (`newcomerDoesNotJumpTheQueue...` and
`enrollCallCanPromoteAnotherWaitlistedLearnerAsSideEffect...`) detect it.

### Traceability

Files are under `src/test/java/waitlist/`. Methods are in `service/CourseEnrollmentServiceTest.java`
unless stated otherwise.

| # | Acceptance criterion | Tests |
|---|---|---|
| 1 | Enroll succeeds with capacity and prerequisite | `enrollsEligibleLearnersWhileCapacityRemains`, `droppedLearnerCanEnrollAgainWhenSeatIsFree`, `nullBlankOrWhitespacePrerequisiteMakesEveryoneEligible` |
| 2 | Full: eligible learner waitlisted once, no duplicate records | `waitlistsEligibleLearnersInArrivalOrderWhenFull`, `secondEnrollOfEnrolledLearnerIsAlreadyEnrolled`, `secondEnrollOfWaitlistedLearnerIsAlreadyWaitlisted`, `duplicateIsDetectedByIdEvenWithDifferentCourses`; `service/CourseEnrollmentServiceConcurrencyTest.java`: `fiftyConcurrentEnrollsFillExactlyCapacityAndWaitlistTheRestInFifoOrder`, `concurrentDuplicateEnrollsAndDropsOnSameIdsKeepStateConsistent`; `service/CourseEnrollmentServiceRandomizedTest.java`: `serviceMatchesReferenceModelAfterEveryRandomOperation` |
| 3 | Drop promotes the earliest eligible waitlisted learner (FIFO) | `dropPromotesEarliestWaitlistedLearner`, `promotionOrderOverSeveralDropsEqualsJoinOrder`, `thousandLearnersArePromotedOneByOneInExactJoinOrder`, `dropFreesOnlyOneSeatSoOnlyOneLearnerIsPromoted`, `learnerRemovedFromWaitlistRejoinsAtTheTail`, `droppedEnrolledLearnerGoesToWaitlistTailWhenFullAgain`; `CourseEnrollmentServiceRandomizedTest`: `serviceMatchesReferenceModelAfterEveryRandomOperation` |
| 4 | Ineligible learner never enrolled or promoted | `ineligibleLearnerIsRejectedWhileSeatsAreFree`, `ineligibleLearnerIsRejectedAndNotWaitlistedWhenFull`, `ineligibleLearnerAtFrontIsSkippedAndKeepsPositionUntilEligibleAgainThenIsPromoted`, `dropPromotesNobodyWhenEveryWaitlistedLearnerIsIneligible`, `newcomerDoesNotJumpTheQueueAheadOfSkippedLearnerWhoIsEligibleAgain`, `enrollCallCanPromoteAnotherWaitlistedLearnerAsSideEffectAndStillSeatTheNewcomer` |
| 5 | Invalid requests and duplicate drops leave state intact | `dropOfUnknownIdIsNotEnrolledAndChangesNothing`, `repeatedDropIsNotEnrolledAndPromotesNobodyExtra`, `dropWithNullOrBlankIdIsInvalidAndChangesNothing`, `dropOfWaitlistedLearnerRemovesThemWithoutPromotionAndKeepsOrderOfTheRest`, `enrollOfNullLearnerIsInvalid`, `enrollWithNullIdIsInvalid`, `enrollWithBlankIdIsInvalid`, `enrollWithNullCourseSetIsInvalid`, `enrollWithNullElementInCourseSetIsInvalid`, `constructorRejectsCapacityBelowOne`; `controller/EnrollmentControllerTest.java`: `nullIdOrNullCourseSetYieldsInvalidRequestFromTheRealService` |
| 6 | Required test areas, including one focused concurrency test | The rows above cover capacity, duplicates, FIFO, prerequisite and drop cases. `service/CourseEnrollmentServiceConcurrencyTest.java` is the concurrency test. `model/LearnerTest.java` and `controller/EnrollmentControllerTest.java` cover the model and controller. |
| 7 | Documentation and AI usage record | `README.md`, `docs/DESIGN.md`, `docs/LIMITATIONS.md`. The AI usage record is `ai-record/AI_USAGE.md`. |

## Tradeoffs and rejected alternatives

- **Live-view `Learner` rejected.** Wrapping the caller's mutable set would let the caller change
  it while the service reads it under its lock, which is a data race the lock cannot prevent.
  `Learner` copies the set instead.
- **Predicate seam chosen.** A package-private constructor taking `Predicate<Learner>` lets tests
  exercise skip-and-retain without weakening `Learner`. Cost: `new CourseEnrollmentService(1, null)`
  is ambiguous, so callers write `(String) null`.
- **`snapshot()` rejected.** A single-lock snapshot would allow mid-flight cross-read checks, but
  tests check after all tasks finish, so the extra method was not needed.
- **Capacity 0 rejected.** With no way to change capacity, such a course would only waitlist.
- **REST and Spring dropped.** Out of scope; the controller is a plain class.
