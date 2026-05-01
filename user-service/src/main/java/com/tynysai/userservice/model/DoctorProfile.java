package com.tynysai.userservice.model;

import com.tynysai.userservice.model.enums.Gender;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.*;
import java.util.*;

@Entity
@Table(name = "doctor_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true, columnDefinition = "uuid")
    private UUID userId;

    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Column(nullable = false)
    private String specialization;

    @Column(unique = true)
    private String licenseNumber;

    private String hospitalName;

    private String department;

    private Integer yearsOfExperience;

    @Column(length = 500)
    private String bio;

    @Column(length = 500)
    private String education;

    @Column(nullable = false)
    @Builder.Default
    private boolean approved = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "work_schedule", columnDefinition = "jsonb")
    private Map<DayOfWeek, List<TimeRange>> workSchedule;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public Integer getAge() {
        if (dateOfBirth == null) return null;
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    public static Map<DayOfWeek, List<TimeRange>> defaultWorkSchedule() {
        Map<DayOfWeek, List<TimeRange>> schedule = new EnumMap<>(DayOfWeek.class);
        List<DayOfWeek> days = List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);

        for (DayOfWeek day : days) {
            List<TimeRange> intervals = new ArrayList<>();
            intervals.add(new TimeRange(LocalTime.of(9, 0), LocalTime.of(13, 0)));
            intervals.add(new TimeRange(LocalTime.of(14, 0), LocalTime.of(18, 0)));
            schedule.put(day, intervals);
        }

        return schedule;
    }
}
