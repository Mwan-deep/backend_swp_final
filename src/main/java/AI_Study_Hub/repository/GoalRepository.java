// File: GoalRepository.java
package AI_Study_Hub.repository;
import AI_Study_Hub.entity.Goal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface GoalRepository extends JpaRepository<Goal, Long> {
    Optional<Goal> findByGoalName(String goalName);
}