# Limitations

Each item gives the risk and a possible follow-up.

1. **Concurrency FIFO is verified indirectly.**
   Risk: arrival order across threads cannot be observed from outside, so the test infers order
   from the recorded waitlist and draining it.
   Follow-up: record a per-operation sequence number inside the service for tests, or accept the
   indirect check.

2. **The controller test stub depends on the service not being final.**
   Risk: making `CourseEnrollmentService` final breaks `EnrollmentControllerTest`.
   Follow-up: extract a small interface for the service, or test the controller only against the
   real service.

3. **`enroll()` can promote other learners as an unreported side effect.**
   Risk: a caller sees only the newcomer's status and may not know a waitlisted learner got a seat.
   Follow-up: return the list of promoted ids in the result.

4. **`EnrollmentResult.of`, `dropped` and the constructor are public, so results can be forged.**
   Risk: code outside the service can build a result that does not reflect real state.
   Follow-up: if it matters, make `EnrollmentResult` a class with a package-private constructor
   and keep the factories in the service package.

5. **The waitlist is unbounded.**
   Risk: memory grows with the number of waiting learners, and each promotion scan is linear in it.
   Follow-up: add a maximum waitlist size and a status for rejection.

6. **State is in memory only and lost on restart.**
   Risk: all enrollments and the waitlist disappear when the process stops.
   Follow-up: put a repository behind the service.

7. **One offering only.**
   Risk: each offering needs its own service instance, and nothing coordinates them.
   Follow-up: key state by offering id, or add a registry of services.

8. **Eligibility and duplicates are checked by id only.**
   Risk: a repeat request with the same id and different courses is treated as a duplicate, and
   the stored course set is never updated.
   Follow-up: define whether a repeat request should refresh the learner's courses.

9. **Capacity cannot change after construction.**
   Risk: a section cannot be expanded, and capacity 0 is rejected.
   Follow-up: add a `setCapacity` that promotes from the waitlist when it grows.

10. **Skip-and-retain is reachable only through the package-private test seam.**
    Risk: with the public constructor the code path never runs, so it is tested but unused in
    normal operation.
    Follow-up: if eligibility is ever allowed to change after joining, expose the predicate or
    remove the dead path.

11. **The randomized model comparison is single-threaded.**
    Risk: it checks sequential behavior only; thread interleavings are covered just by the two
    concurrency tests.
    Follow-up: add a concurrent history check (for example, linearizability against the model).
