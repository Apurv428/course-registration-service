# Round 2: Design review questions

````text
Before I approve, I have some questions. Please answer them briefly. Don't write any code yet.

## Questions

1. Decision 3 (testing skip-and-retain): Option A makes Learner a live view over the caller's mutable set. What happens if a production caller mutates that set after calling enroll()? Could that break an invariant, for example a learner enrolled as eligible and later holding a seat while ineligible? Is there a design that lets us test skip-and-retain without this hazard? For example, a package-private constructor that takes a Predicate<Learner> for eligibility. Compare the two on risk, test clarity and code size, then recommend one.

2. Direct enroll rule: you proposed "enrolled < capacity AND no eligible waitlisted learner". Can you give a concrete sequence of calls where this differs from a simple "enrolled < capacity" check? If you can't, argue for or against keeping the extra O(n) scan.

3. Decision 4 (single DROPPED status): a caller can't tell "dropped from enrolled" from "dropped from waitlist" without extra state. Is that a real usability problem? What would a separate status like REMOVED_FROM_WAITLIST cost in code and tests?

4. Decision 6 (constructor throws on capacity < 1): should capacity 0 be valid, meaning a course that only waitlists? Say why you chose to reject it.

5. Concurrency: besides the lock, which interleavings could break an invariant if one of the read methods (isEnrolled, enrolledIds, waitlistIds) were not locked? Give one concrete example. Also, is a lock around each read enough for a test that checks two reads together, such as enrolledCount() and waitlistIds()? If not, what would you suggest?

6. Is there anything in the approved list of statuses that you would add, remove or rename? Check it against INVALID_REQUEST cases such as a null learner, a blank id, and a null course set.

After your answers, I'll tell you which positions I accept and give you the go-ahead for Step 2. Wait for that.
````

[Prompt log row 3](../AI_USAGE.md#2-prompt-log) · Decision: Accepted (seam, promotion loop, quiescent checks); added by author: `REMOVED_FROM_WAITLIST`, null elements invalid; rejected: `snapshot()`, allowing capacity 0.
