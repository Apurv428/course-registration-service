package waitlist.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class LearnerTest {

    @Test
    void mutatingOriginalSetAfterConstructionDoesNotChangeLearner() {
        Set<String> courses = new HashSet<>(Set.of("CS101"));
        Learner learner = new Learner("ann", courses);

        courses.add("CS102");
        courses.remove("CS101");

        assertEquals(Set.of("CS101"), learner.completedCourseIds());
    }

    @Test
    void exposedSetIsUnmodifiable() {
        Learner learner = new Learner("ann", new HashSet<>(Set.of("CS101")));

        assertThrows(UnsupportedOperationException.class, () -> learner.completedCourseIds().add("CS102"));
        assertThrows(UnsupportedOperationException.class, () -> learner.completedCourseIds().clear());
    }

    @Test
    void nullSetDoesNotThrowAndIsStoredAsNull() {
        Learner learner = assertDoesNotThrow(() -> new Learner("ann", null));

        assertNull(learner.completedCourseIds());
    }

    @Test
    void nullIdDoesNotThrow() {
        Learner learner = assertDoesNotThrow(() -> new Learner(null, Set.of("CS101")));

        assertNull(learner.id());
    }

    @Test
    void setContainingNullDoesNotThrow() {
        Set<String> withNull = new HashSet<>(Arrays.asList("CS101", null));

        Learner learner = assertDoesNotThrow(() -> new Learner("ann", withNull));

        assertEquals(withNull, learner.completedCourseIds());
    }
}
