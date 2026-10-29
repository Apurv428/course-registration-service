package waitlist.controller;

import java.util.Set;
import waitlist.dto.EnrollmentResult;
import waitlist.model.Learner;
import waitlist.service.CourseEnrollmentService;

/** Thin entry point: turns raw inputs into a {@link Learner} and delegates. Adds no rules, holds no state. */
public class EnrollmentController {

    private final CourseEnrollmentService service;

    public EnrollmentController(CourseEnrollmentService service) {
        this.service = service;
    }

    public EnrollmentResult enroll(String learnerId, Set<String> completedCourseIds) {
        return service.enroll(new Learner(learnerId, completedCourseIds));
    }

    public EnrollmentResult drop(String learnerId) {
        return service.drop(learnerId);
    }
}
