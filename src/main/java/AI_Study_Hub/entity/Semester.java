package AI_Study_Hub.entity;

import com.fasterxml.jackson.annotation.JsonGetter;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "semesters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Semester {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "semester_id")
    private Long semesterId;

    @Column(name = "semester_name", nullable = false, length = 50)
    private String semesterName;

    // Ép đúng kiểu String vì database của bạn khai báo là VARCHAR(20)
    @Column(name = "academic_year", nullable = false, length = 20)
    private String academicYear;

    // Phương thức này sẽ tạo ra một trường "displayName" trong JSON trả về
    @JsonGetter("displayName")
    public String getDisplayName() {
        return this.semesterName + " " + this.academicYear;
    }
}