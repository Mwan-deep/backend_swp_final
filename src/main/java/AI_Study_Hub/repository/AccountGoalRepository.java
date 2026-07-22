// File: AccountGoalRepository.java
package AI_Study_Hub.repository;
import AI_Study_Hub.entity.AccountGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AccountGoalRepository extends JpaRepository<AccountGoal, Long> {
    List<AccountGoal> findAllByOrderByCreatedAtDesc();
}