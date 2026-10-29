# Course Waitlist

A small in-memory service for one course offering. Learners enroll while seats remain, join a
FIFO waitlist when the course is full, and the earliest eligible waitlisted learner is promoted
when a seat opens.

There is no REST layer. The controller is a plain Java class, not a web endpoint. A small console
demo, `waitlist.Main`, runs a fixed scenario; it takes no input.

## Requirements

- Java 17
- Maven 3.x

## Build and test

```
mvn -q compile
mvn test
```

Expected output: `mvn test` ends with `Tests run: 64, Failures: 0` and `BUILD SUCCESS`.

## Run the demo

```
mvn -q compile
java -cp target/classes waitlist.Main
```

It prints each step of a short scenario (capacity 2, required course `CS101`) with the status and
the current enrolled and waitlist lists.

## Layout

| Package | Contents |
|---|---|
| `waitlist` | `Main`: console demo of a fixed scenario |
| `waitlist.model` | `Learner`: id plus an immutable copy of the completed-course ids |
| `waitlist.dto` | `EnrollmentResult` (status plus optional promoted learner id) and `EnrollmentStatus` |
| `waitlist.service` | `CourseEnrollmentService`: all rules, state and locking |
| `waitlist.controller` | `EnrollmentController`: builds a `Learner` from raw inputs and delegates; no rules, no state |

Dependencies point one way: controller, then service, then model and dto.

## Statuses

| Status | Returned when |
|---|---|
| `ENROLLED` | enroll: the learner got a seat |
| `WAITLISTED` | enroll: the course is full, the learner joined the waitlist tail |
| `ALREADY_ENROLLED` | enroll: the id already holds a seat |
| `ALREADY_WAITLISTED` | enroll: the id is already on the waitlist |
| `NOT_ELIGIBLE` | enroll: the learner lacks the required course |
| `DROPPED` | drop: an enrolled learner left; `promotedLearnerId` is the learner who took the seat, or null |
| `REMOVED_FROM_WAITLIST` | drop: a waitlisted learner was removed; nobody is promoted |
| `NOT_ENROLLED` | drop: the id is neither enrolled nor waitlisted (includes repeated drops) |
| `INVALID_REQUEST` | null learner, null or blank id, null course set or null element in it; null or blank id on drop |

## Usage

```java
CourseEnrollmentService service = new CourseEnrollmentService(1, "CS101");

service.enroll(new Learner("ann", Set.of("CS101"))).status();   // ENROLLED
service.enroll(new Learner("bob", Set.of("CS101"))).status();   // WAITLISTED
service.enroll(new Learner("cy", Set.of("MATH200"))).status();  // NOT_ELIGIBLE

EnrollmentResult result = service.drop("ann");
result.status();                      // DROPPED
result.promoted().orElse(null);       // "bob"
```

A null or blank required course means every learner is eligible. To pass a literal `null`
there, write `(String) null`, because the constructor is overloaded.

More detail is in [docs/DESIGN.md](docs/DESIGN.md) and [docs/LIMITATIONS.md](docs/LIMITATIONS.md).
How AI was used on this project is recorded in [ai-record/AI_USAGE.md](ai-record/AI_USAGE.md).
