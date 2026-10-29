# Round 7: Extras (randomized test, large waitlist, demo Main)

````text
Docs are done and accepted. Now add the extras below, then update the docs so they match. Keep existing behavior and the 43 passing tests unchanged. No production logic changes except adding Main.

## 1. Randomized test: src/test/java/waitlist/service/CourseEnrollmentServiceRandomizedTest.java
- 20 fixed seeds, 500 random operations each, java.util.Random, single-threaded. Include the seed in every assertion message so failures are reproducible.
- Operations: enroll (random id from a pool of ~15, random eligible/ineligible via course set against a required course) and drop (random id, including unknown ids and repeats).
- Include a tiny reference model inside the test (plain list/set, FIFO queue, promote first waiter), written from the class Javadoc/docs, NOT copied from the service. After EVERY operation assert: returned status and promotedLearnerId match the model; enrolledIds and waitlistIds match the model; then assertInvariants(service).

## 2. Large waitlist test (in CourseEnrollmentServiceTest or a new class)
- Capacity 3, 1000 eligible learners enroll in order: 3 enrolled, 997 waitlisted in join order. Then drop enrolled learners repeatedly; each promoted id must equal the next id in join order, for all 997 promotions; end with an empty waitlist and invariants intact.

## 3. Demo Main: src/main/java/waitlist/Main.java (40 lines or fewer)
- Capacity 2, required course "CS101". Print each step of a short scenario with status and current enrolled/waitlist lists: enroll eligible learners until full, one WAITLISTED, one NOT_ELIGIBLE, a duplicate, a drop that promotes the waitlisted learner, a repeated drop. No input parsing, no frameworks.
- Document the run command (`mvn -q compile` then `java -cp target/classes waitlist.Main`).

## 4. Verification (required)
- `mvn test`: show the summary with the new total.
- Run Main and show its output.
- Mutation check on the randomized test ALONE (run only that class): (a) promoteNext takes the last waiter, (b) remove the duplicate check, (c) remove the promotion loop in enroll. Report which are caught; revert each and confirm green. If one is missed, say so honestly; do not bend the test to force a catch.
- Run the randomized and large-waitlist tests 20 times; report failures.

## 5. Doc updates (only after verification passes)
- README.md: replace the "no main program" statement with the Main run command; add a short "Expected output" line to the build/test section: `mvn test` ends with `Tests run: N, Failures: 0` using the real N.
- docs/DESIGN.md: add a "Traceability" table to the Test strategy section mapping acceptance criteria 1-7 to test class/method names and files (criterion 7 = docs/AI usage record, pointing to ai-record/AI_USAGE.md, to be written). Add the randomized test and the 1000-learner test to the test list.
- docs/LIMITATIONS.md: add that the randomized model comparison is single-threaded.
- Grep to confirm every cited test/method name exists. State that you did.

List deviations. Stop and wait if any issues.
````

[Prompt log row 8](../AI_USAGE.md#2-prompt-log) · Decision: Accepted.
