# Round 6: Docs (Step 4)

This prompt reached the chat as pasted text.

````text
Tests reviewed and re-run independently: 43 passing, accepted all five deviations. Add these to the limitations list: concurrency FIFO is verified indirectly (arrival order is not observable externally); the controller test stub depends on the service not being final; enroll() can promote others as an unreported side effect; EnrollmentResult.of/dropped/constructor are public so results can be forged; waitlist is unbounded; state is in memory only and lost on restart; one offering only; eligibility is checked by id only (a duplicate id with different courses is treated as a duplicate); capacity cannot change after construction; skip-and-retain is reachable only through the package-private test seam.

## Step 4 (DOCS) only. No code changes.
Create these files; keep each proportionate (no padding), plain wording, no secrets or personal data:

1. README.md (repo root): what it is (2-3 lines), requirements (Java 17, Maven), build/test commands (`mvn test`, `mvn -q compile`), the package layout (model, dto, service, controller) in one table, the statuses in one table, and a short usage snippet constructing the service and calling enroll/drop. State plainly that there is no REST layer and no main program.
2. docs/DESIGN.md with sections: Scope and boundary; Assumptions (prerequisite rule, ineligible-at-front policy = skip and retain, eligibility checked at enroll and at promotion, duplicates by id, unbounded waitlist, drop of a waitlisted learner, capacity < 1 rejected); Data model; API (method signatures + status table including what each returns); Invariants and how each is enforced; Promotion rule (including the stuck-seat/no-queue-jumping reasoning in enroll); Concurrency approach (single ReentrantLock, reads locked, cross-read atomicity not offered); Test strategy (map each acceptance criterion 1-6 to test class/method names that cover it, taken from the real code); Tradeoffs and rejected alternatives (live-view Learner rejected for a data race; Predicate seam chosen; snapshot() rejected; REST/Spring dropped as out of scope).
3. docs/LIMITATIONS.md: the list above, each with a one-line risk and a one-line follow-up.
4. Do NOT write AI_USAGE.md; I will provide the prompt record separately.

Rules: every claim must be true of the current code and tests. Before finishing, grep the code/tests to confirm each method and test name you cite exists, and say so. Run `mvn test` once more and show the summary. List deviations. Stop and wait.
````

[Prompt log row 7](../AI_USAGE.md#2-prompt-log) · Decision: Accepted.
