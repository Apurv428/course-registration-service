# Round 4: Layered refactor

This prompt reached the chat as pasted text.

````text
Before Step 3 (do NOT write tests yet), refactor into a layered package structure. Convention for this project:

waitlist/
├── model/       Learner
├── dto/         EnrollmentResult, EnrollmentStatus
├── service/     CourseEnrollmentService (all business logic, lock, state, Predicate seam)
├── controller/  EnrollmentController (thin, plain Java, NO web framework, NO HTTP)
└── (optional) Main/CLI in waitlist/ root only if it stays under ~40 lines

Rules:
- Dependencies point one way: controller -> service -> model/dto. Service must not import controller.
- Business logic, locking and state stay in service. Controller only: accepts raw inputs (e.g. learnerId String, completed courses Set<String>), builds the Learner, calls the service, returns the EnrollmentResult. It adds no rules of its own and holds no state.
- Because packages now differ, make EnrollmentResult.of/dropped public (or equivalent) and tell me the cost. Keep the Predicate<Learner> constructor package-private in the service package.
- Mirror the layout under src/test/java/waitlist/<layer>/ later (service tests in waitlist.service to reach the seam; controller tests in waitlist.controller).
- Behavior must not change. Move code, fix imports, nothing else.

Verify: run `mvn -q compile` and show output. Show the final tree (`find src -type f | sort`). List any deviation. Stop and wait. Tests come next only when I say so.
````

[Prompt log row 5](../AI_USAGE.md#2-prompt-log) · Decision: Accepted; repository layer rejected and Spring/REST dropped (author's account).
