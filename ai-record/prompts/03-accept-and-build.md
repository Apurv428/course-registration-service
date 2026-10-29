# Round 3: Accept decisions and build (Step 2)

````text
Thanks, this was useful. Decisions:

## Accepted
1. Predicate seam: ACCEPTED. Option A is withdrawn. Learner copies defensively and stays immutable. Add a package-private constructor taking Predicate<Learner>; the public constructor delegates with the default prerequisite rule.
2. Direct-enroll fix: ACCEPTED. When a seat is free, enroll runs a promotion pass first and seats the new learner only if a seat is still free after it. Otherwise the learner goes to the waitlist tail. Add a short comment explaining the queue-jumping and stuck-seat scenario you described.
3. Status list: ADD REMOVED_FROM_WAITLIST. Drop of a waitlisted learner returns it. Drop of an enrolled learner returns DROPPED, with promotedLearnerId set or null. Update the EnrollmentResult Javadoc to say this.
4. Capacity: keep rejecting capacity < 1 with IllegalArgumentException. Document it in Javadoc.
5. Concurrency: all reads stay locked. Do NOT add a snapshot() method. Tests check cross-read invariants only after awaitTermination.
6. Learner: copy a non-null set with Collections.unmodifiableSet(new HashSet<>(set)); store null as-is so the service returns INVALID_REQUEST. Null elements inside the set should also be INVALID_REQUEST. Document that Learner does not throw.

## Honesty requirement
Skip-and-retain is reachable only via the package-private predicate seam, not the public API with the default rule. Say this in the Javadoc and keep it in mind for the design doc.

## Go ahead: Step 2 (BUILD) only
Create pom.xml (Java 17, JUnit 5, surefire) and model classes only.
- Run `mvn -q compile` and show the output.
- Do not write tests yet.
- Afterwards, list any deviation from these decisions.
- Stop and wait.
````

[Prompt log row 4](../AI_USAGE.md#2-prompt-log) · Decision: Accepted (author's account).
