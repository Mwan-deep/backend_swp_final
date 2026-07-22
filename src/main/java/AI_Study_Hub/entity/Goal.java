package AI_Study_Hub.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

@Entity
@Table(name = "goal")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Goal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "goal_id")
    Long goalId;

    @Column(name = "goal_name", columnDefinition = "NVARCHAR(255)")
    String goalName;

    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    String description;

    @Column(name = "goal_at")
    LocalDateTime goalAt;
}