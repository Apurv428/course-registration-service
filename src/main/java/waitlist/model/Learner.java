package waitlist.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A learner identified by id, with the set of course ids they have completed.
 *
 * <p>The constructor never throws. A non-null set is copied defensively and exposed as
 * unmodifiable, so later changes by the caller have no effect. A null id or null set is
 * stored as-is; the enrollment service rejects it with {@code INVALID_REQUEST}, as it does for
 * a blank id or a set containing null.
 */
public record Learner(String id, Set<String> completedCourseIds) {

    public Learner {
        completedCourseIds = completedCourseIds == null
                ? null
                : Collections.unmodifiableSet(new HashSet<>(completedCourseIds));
    }
}
