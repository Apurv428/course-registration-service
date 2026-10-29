# Round 1: Task and design (Step 1)

````text
You are my engineering assistant. I'm completing a small project: a course waitlist feature in Java. Work step by step and wait for my approval between phases.

## Task
Implement enrollment, dropping, and FIFO waitlist promotion for ONE course offering. No starter project exists, so build a minimal service with an in-memory store.

## Stack
Java 17, Maven, JUnit 5. No frameworks (no Spring), no database, no frontend.

## Requirements
1. Enroll succeeds when capacity remains and the learner meets the prerequisite.
2. When full, an eligible learner joins the waitlist once. Duplicate requests create no duplicate records.
3. Dropping an enrolled learner opens a seat and promotes the earliest eligible waitlisted learner (FIFO).
4. An ineligible learner is never enrolled or promoted.
5. Invalid requests and duplicate drops return clear results and never corrupt enrollment, capacity, or waitlist state.
6. Tests cover: available capacity, full capacity, duplicate requests, FIFO promotion, prerequisite failure, invalid/repeated drops, plus one focused concurrency test.

## Design decisions
- Eligibility: a learner is eligible if their completed-course set contains the offering's required course ID. An empty/absent prerequisite means everyone is eligible.
- Ineligible learner at the front of the waitlist: SKIP AND RETAIN. Promote the next eligible learner. The skipped learner keeps their original position and is reconsidered on later drops.
- Eligibility is checked at enroll time and again at promotion time.
- Waitlist is unbounded.
- Concurrency: a single ReentrantLock around every mutating operation. The concurrency test uses an ExecutorService.
- Operations return a result object with a status enum instead of throwing. Statuses: ENROLLED, WAITLISTED, ALREADY_ENROLLED, ALREADY_WAITLISTED, NOT_ELIGIBLE, DROPPED, NOT_ENROLLED, INVALID_REQUEST. A drop result also carries the promoted learner id, if any.
- The service takes a Learner (id + completed-course set). No learner registry or identity layer.
- A direct enroll request while a waitlist exists is not allowed to jump the queue. A seat can be taken by a direct enroll only if the waitlist has no eligible learner. Flag any ambiguity here and propose a rule before coding.
- Drop of a waitlisted (not enrolled) learner removes them from the waitlist.

## Invariants to enforce and test
- enrolled count <= capacity
- a learner is never both enrolled and waitlisted
- no duplicates in enrolled or waitlist
- waitlist order is insertion order (FIFO)

## Out of scope
Frontend, course catalog, payments, identity, multi-term registration, deployment, configurable policy engine, new database.

## Process (follow in order)
1. DESIGN: Restate the design in a short summary: classes, method signatures, status enum, invariants, and any edge cases or contradictions you spot. Stop and wait for my approval.
2. BUILD: Implement the code. Stop and wait.
3. TEST: Write the JUnit 5 tests. Run them and show the output. Stop and wait.
4. DOCS: Only when I ask: DESIGN.md, README.md (setup + test command), and a limitations list.

## Rules
- Keep the code small and readable. Match standard Java conventions.
- Do not add features beyond this scope.
- Push back if any decision above looks wrong, and explain why.
- Never include secrets or personal data in code or docs.

Start with step 1 only.
````

[Prompt log rows 1–2](../AI_USAGE.md#2-prompt-log) · Decision: Accepted as the baseline (row 1); Edited (row 2, see section 3 of the record).
