package waitlist;

import java.util.Set;
import waitlist.controller.EnrollmentController;
import waitlist.dto.EnrollmentResult;
import waitlist.service.CourseEnrollmentService;

/** Console demo: capacity 2, prerequisite CS101. Run with {@code java -cp target/classes waitlist.Main}. */
public final class Main {

    public static void main(String[] args) {
        CourseEnrollmentService service = new CourseEnrollmentService(2, "CS101");
        EnrollmentController controller = new EnrollmentController(service);
        Set<String> ready = Set.of("CS101");

        print(service, "enroll ann", controller.enroll("ann", ready));
        print(service, "enroll bob", controller.enroll("bob", ready));
        print(service, "enroll cy (full)", controller.enroll("cy", ready));
        print(service, "enroll dan (no CS101)", controller.enroll("dan", Set.of("MATH200")));
        print(service, "enroll bob again", controller.enroll("bob", ready));
        print(service, "drop bob", controller.drop("bob"));
        print(service, "drop bob again", controller.drop("bob"));
    }

    private static void print(CourseEnrollmentService service, String step, EnrollmentResult result) {
        System.out.printf("%-22s %-18s promoted=%-5s enrolled=%s waitlist=%s%n", step, result.status(),
                result.promotedLearnerId(), service.enrolledIds(), service.waitlistIds());
    }
}
