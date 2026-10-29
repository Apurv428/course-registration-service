# Round 8: AI usage record

This prompt was sent while the previous round was still running.

````text
Deliverable: ai-record/AI_USAGE.md. No code changes. Write it from THIS chat's real history. Quote or summarize your own earlier responses accurately; do not invent excerpts. Keep it factual and candid. No secrets or personal data (no emails or names).

## Structure
1. Overview: AI tools used and for what (design discussion and prompt drafting in a separate assistant chat; implementation, tests and docs in this chat). State that the prompts below were drafted with AI help and reviewed/edited by me before sending.
2. Prompt log table, one row per round: # | Prompt (short quote or summary) | AI response (excerpt/summary) | Decision (Accepted / Edited / Rejected) | Files or decisions informed | Verification.
3. Detail section for the rounds where I changed the AI's output (below).
4. What AI got wrong or missed (candid).
5. Verification evidence summary: final test count, mutation table, repeated-run results, commands to reproduce.
6. What I did independently.

## Rounds to record (facts from my side; match them to your actual responses)
1. Task framing and design decisions (stack Java 17/Maven/JUnit 5, eligibility rule, skip-and-retain, lock, result statuses). Accepted as the baseline.
2. Step 1 design from you. You flagged that skip-and-retain is unreachable via the public API and proposed live-view Learner (option A) for testing. I EDITED: I did not accept a live view because of mutation risk; asked for a package-private Predicate seam instead.
3. Clarification round: I asked 7 questions instead of approving. You withdrew option A (data race on the caller's set), found a stuck-seat/queue-jumping flaw in my approved direct-enroll rule and proposed promotion-before-seating, explained torn reads. ACCEPTED: seam, promotion loop, no snapshot(), quiescent invariant checks. ADDED by me: REMOVED_FROM_WAITLIST status. REJECTED: capacity 0, snapshot().
4. Step 2 build. I reviewed all files; ACCEPTED deviations (service built early, (String) null ambiguity, one promotion per drop). Logged limitations (unreported promotion side effect in enroll, forgeable results).
5. Refactor to layered packages (model/dto/service/controller), requested by me. Behavior unchanged; verified with compile and file tree. I rejected adding a repository layer (lock and invariants belong with state). I considered and dropped a Spring/REST layer as out of scope.
6. Step 3 tests (43 tests). I required mutation checks (5 mutations), a 20x concurrency loop, and re-ran `mvn test` myself. All mutations caught. ACCEPTED 5 deviations; logged indirect FIFO-under-concurrency check and the service-not-final dependency as limitations.
7. Step 4 docs (README, DESIGN, LIMITATIONS). I checked that every backticked identifier in DESIGN.md exists in src.
8. Step 3b extras: randomized reference-model test, 1000-learner fairness test, demo Main, doc updates. Use the real results and numbers from this chat.

## Rules
- Use real test names, counts and numbers from the repo; run `mvn test` and put the actual summary line in section 5.
- For anything not in this chat's history, write "not recorded" rather than guessing.
- Grep to confirm each file and test name you cite exists, and say that you did.
- Add a line to the README pointing to ai-record/AI_USAGE.md.
````

[Prompt log row 9](../AI_USAGE.md#2-prompt-log) · Decision: n/a (deliverable request).
