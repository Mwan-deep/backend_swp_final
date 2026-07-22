package AI_Study_Hub.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

@Entity
@Table(name = "account_goal")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AccountGoal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne
    @JoinColumn(name = "account_id")
    Account account;

    @ManyToOne
    @JoinColumn(name = "goal_id")
    Goal goal;

    @Column(name = "awarded_month")
    String awardedMonth;

    @Column(name = "category")
    String category;

    @Column(name = "created_at")
    LocalDateTime createdAt;
}