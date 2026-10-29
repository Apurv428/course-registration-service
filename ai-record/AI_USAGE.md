# AI Usage Record

## 1. Overview

| Tool / chat | Used for |
|---|---|
| A separate assistant chat | Design discussion and drafting the prompts below. |
| This chat (Claude Code in the editor) | Implementation, tests, docs, verification runs. |

The prompts in this record were drafted with AI help and reviewed and edited by the author before
sending. The design discussion and the drafting of these prompts happened in a separate AI chat whose
transcript is not included. The full text of every prompt sent in this chat is in
[ai-record/prompts/](prompts/).

**Source of facts.** The responses below are taken from this chat. Statements marked *(author's
account)* come from the author's instructions for this record and cannot be checked from this chat's
history. Anything else not in the history is marked "not recorded".

**Round numbering.** The eight rounds follow the author's list. The author's rounds 1 and 2 are one
exchange in this chat (the opening prompt and the design reply).

## 2. Prompt log

Full prompt text, one file per prompt sent in this chat:

1. [01-task-and-design.md](prompts/01-task-and-design.md): table rows 1–2
2. [02-design-review-questions.md](prompts/02-design-review-questions.md): row 3
3. [03-accept-and-build.md](prompts/03-accept-and-build.md): row 4
4. [04-layered-refactor.md](prompts/04-layered-refactor.md): row 5
5. [05-tests.md](prompts/05-tests.md): row 6
6. [06-docs.md](prompts/06-docs.md): row 7
7. [07-extras.md](prompts/07-extras.md): row 8
8. [08-ai-usage-record.md](prompts/08-ai-usage-record.md): row 9

| # | Prompt (summary) | AI response (excerpt / summary) | Decision | Informed | Verification |
|---|---|---|---|---|---|
| 1 | Opening task: Java 17 / Maven / JUnit 5 course waitlist, in-memory, no frameworks. Design decisions fixed up front: eligibility by completed-course set, skip-and-retain, single `ReentrantLock`, result object with a status enum. Work in phases and wait for approval. | See round 2. | Accepted as the baseline | Scope, statuses, invariants | n/a |
| 2 | "Step 1 only": restate the design, flag contradictions, wait. | Classes, signatures, check order, invariants. Flagged: "Skip-and-retain therefore never triggers through the public API." Proposed testing it with option A, a live-view `Learner` ("(A, recommended)"), and asked about `DROPPED` vs a waitlist status and capacity 0. | Edited (see section 3) | `Learner`, service design | Author review before approving |
| 3 | Seven-part clarification round instead of approval *(the prompt in this chat contains six questions)*: option A's risk, a differing call sequence for the direct-enroll rule, single `DROPPED`, capacity 0, unlocked reads, statuses. | Withdrew A: "The lock can't protect memory it doesn't own." Recommended the predicate seam. Showed a sequence where the proposed enroll rule leaves a seat stuck, and proposed promoting before seating. Explained torn reads and why separate locked reads are not atomic together. Flagged that `Set.copyOf` throws on null. | Accepted: seam, promotion loop, quiescent checks. Added by author: `REMOVED_FROM_WAITLIST`, null elements invalid. Rejected: snapshot(), allowing capacity 0 | Service design, `Learner`, status list | Author's written decisions in the next prompt |
| 4 | "Step 2 BUILD only": `pom.xml` and model classes; run `mvn -q compile`; list deviations. | Built the four classes and the pom; compile exit 0. Deviations listed, including "I also built the service" and "`new CourseEnrollmentService(1, null)` won't compile." | Accepted (author's account) | `CourseEnrollmentService`, `Learner`, `EnrollmentResult`, `pom.xml` | `mvn -q compile` exit 0 |
| 5 | Refactor into `model` / `dto` / `service` / `controller`, no behavior change, no tests yet. | Moved files, fixed imports, added `EnrollmentController`. Made `EnrollmentResult.of`/`dropped` public and explained the cost. Reported one Javadoc edit to keep dependencies one-way. | Accepted. Repository layer rejected and Spring/REST dropped *(author's account; only "no web framework" is in this chat's prompt)* | Package layout, controller | `mvn -q compile` exit 0; `find src -type f` |
| 6 | "Step 3 TEST only": test classes, required coverage, 5 mutation checks, 20x concurrency loop. | Wrote 43 tests plus `ServiceTestSupport`. "All 43 tests pass, every mutation was caught." Listed 5 deviations. | Accepted all five deviations. Author re-ran the tests independently. FIFO-under-concurrency and service-not-final logged as limitations | The four test classes | `mvn test`; mutation table in section 5; loop 0/20 |
| 7 | "Step 4 DOCS only": README, DESIGN, LIMITATIONS, plus the author's limitations list; grep every cited name. | Wrote the three docs. Found only `snapshot` and `setCapacity` missing, both deliberate. | Accepted | `README.md`, `docs/` | Identifier grep; `mvn test` |
| 8 | Extras: randomized reference-model test, 1000-learner test, demo `Main`, doc updates, mutation check on the randomized test alone, 20x loop. | Added the tests and `Main` (29 lines). Total 64 tests. Randomized test caught 2 of 3 mutations. Updated README, DESIGN traceability, LIMITATIONS. | Accepted | `CourseEnrollmentServiceRandomizedTest`, `Main`, docs | Section 5 |
| 9 | This record. | This file. | n/a | `ai-record/AI_USAGE.md` | Name grep in section 5 |

## 3. Where the author changed the AI's output

1. **Live-view `Learner` (option A) replaced by a predicate seam.** The AI recommended A in the
   design. The author asked what happens if production code mutates the set and suggested a
   package-private `Predicate<Learner>` constructor as an alternative. The AI compared them and
   switched ("so I'm withdrawing A"). The seam idea came from the author's question, not from the AI.
2. **Direct-enroll rule.** The AI's first rule was "seat free and no eligible waitlisted learner".
   Asked for a concrete differing sequence, the AI showed it leaves a seat stuck. The author
   accepted the fix (promotion pass before seating) and required a comment explaining it.
3. **Status list.** The AI suggested a separate waitlist-removal status as optional. The author
   added `REMOVED_FROM_WAITLIST` and required the `EnrollmentResult` Javadoc to say so.
4. **Capacity and reads.** The author kept the `capacity < 1` rejection (the AI had said either
   was defensible), rejected `snapshot()`, kept all reads locked, and fixed that invariants are
   checked only after `awaitTermination`.
5. **`Learner` nulls.** The AI proposed a null-tolerant copy. The author added that null elements
   inside the set must give `INVALID_REQUEST` and that `Learner` must be documented as never throwing.
6. **Honesty requirement.** The author required the Javadoc to state that skip-and-retain is
   reachable only through the package-private seam.
7. **Tests.** The author specified coverage and mutation checks, and asked the AI to check for
   missing tests. The AI added seven (whitespace prerequisite, id-based duplicates, one seat per
   drop, revoked eligibility keeps seat, all waiters ineligible, invalid learner with no
   prerequisite, `promoted()` accessor).
8. **Docs.** The author supplied the limitations list; the AI wrote the entries and added a risk
   and follow-up for each.
9. **Extras.** The author designed the randomized model test, the 1000-learner test and `Main`.

## 4. What the AI got wrong or missed

- **Proposed a flawed testing option.** Option A (live view) introduced a data race. The author's
  question exposed it; the AI did not flag it at design time.
- **First direct-enroll rule was incomplete.** It could leave a seat free while someone waits.
  Found only when the author asked for a concrete differing sequence.
- **Overload ambiguity not flagged at design.** `new CourseEnrollmentService(1, null)` does not
  compile with both a `String` and a `Predicate` constructor. Reported after the build, not before.
- **Interpreted "model classes only" loosely.** The service was built in step 2 and the AI said so.
- **Randomized test misses one mutation.** Removing the promotion loop in `enroll` is not caught,
  because under the public constructor the loop never has anyone to promote. Only the seam tests
  catch it. The test was not changed to force a catch.
- **Shared-author blind spot.** The reference model in the randomized test was written by the same
  AI that wrote the service, from the documented rules. A shared misreading of the rules would
  pass in both.
- **Never run on JDK 17.** `pom.xml` sets `release 17`, but the machine used JDK 21 (`java -version`)
  and Maven ran on JDK 25 (`mvn -v`). Java 17 compatibility rests on the compiler flag only.
- **Minor.** While editing `LIMITATIONS.md` the AI first inserted item 11 before item 10 and then
  reordered it.

## 5. Verification evidence

**Final run** (`mvn test`, run when this record was written): `Tests run: 64, Failures: 0, Errors: 0, Skipped: 0` and `BUILD SUCCESS`.

| Class | Tests |
|---|---|
| `EnrollmentControllerTest` | 4 |
| `LearnerTest` | 5 |
| `CourseEnrollmentServiceTest` | 33 |
| `CourseEnrollmentServiceConcurrencyTest` | 2 |
| `CourseEnrollmentServiceRandomizedTest` | 20 (one per seed) |
| **Total** | **64** |

**Mutation checks (each applied alone, then reverted and re-run green).**
The mutation script was a throwaway in the session scratchpad and is not in the repo; the
mutations are simple enough to reproduce by hand.

| Mutation | Full suite (43 tests at the time) | Randomized test alone |
|---|---|---|
| Remove duplicate check in `enroll` | Caught: 5 failures | Caught: 20 of 20 seeds fail |
| `promoteNext` takes the last waiter | Caught: 7 failures | Caught: 20 of 20 seeds fail |
| Remove lock from `enroll` and `drop` | Caught; re-run 10 times, caught 10 of 10 | Not run |
| Remove promotion loop in `enroll` | Caught: 2 failures (seam tests) | **Missed** (0 failures), see section 4 |
| Remove promotion-time eligibility check | Caught: 4 failures | Not run |

**Repeated runs.** Concurrency tests, 20 runs: 0 failures. Randomized test plus
`thousandLearnersArePromotedOneByOneInExactJoinOrder`, 20 runs: 0 failures.

**Reproduce.**
```
mvn test
mvn -q compile && java -cp target/classes waitlist.Main
mvn test -Dtest=CourseEnrollmentServiceConcurrencyTest
mvn test -Dtest=CourseEnrollmentServiceRandomizedTest
```

**Name check.** After writing this file I grepped the source tree for every file and test name cited
here; the result is reported in the hand-over message, not stored in this file.

## 6. What the author did independently

- Wrote the task constraints, the phase process and the design decisions in the opening prompt
  (drafted with AI help in a separate chat, see section 1).
- Pushed back on the design with targeted questions instead of approving, which led to the seam, the
  promotion loop and the dropped live view.
- Added `REMOVED_FROM_WAITLIST`, the null-element rule and the Javadoc honesty requirement.
- Required the mutation checks and repeated-run loops, and re-ran the 43 tests independently
  (the author's message in this chat says so).
- Supplied the limitations list and the extras (randomized and 1000-learner tests, demo `Main`).
- Anything else, such as a manual review of each file or a check of every cited identifier in
  `DESIGN.md`: *(author's account)*, not recorded in this chat.
