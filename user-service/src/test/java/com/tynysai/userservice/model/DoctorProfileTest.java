package com.tynysai.userservice.model;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DoctorProfileTest {

    @Test
    void defaultWorkSchedule_coversMondayThroughFriday() {
        Map<DayOfWeek, List<TimeRange>> schedule = DoctorProfile.defaultWorkSchedule();

        assertThat(schedule.keySet()).containsExactlyInAnyOrder(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);
    }

    @Test
    void defaultWorkSchedule_excludesWeekends() {
        Map<DayOfWeek, List<TimeRange>> schedule = DoctorProfile.defaultWorkSchedule();

        assertThat(schedule).doesNotContainKeys(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
    }

    @Test
    void defaultWorkSchedule_eachDayHasMorningAndAfternoonShift() {
        Map<DayOfWeek, List<TimeRange>> schedule = DoctorProfile.defaultWorkSchedule();

        for (DayOfWeek day : List.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)) {
            List<TimeRange> ranges = schedule.get(day);
            assertThat(ranges).hasSize(2);
            assertThat(ranges.get(0).getStart()).isEqualTo(LocalTime.of(9, 0));
            assertThat(ranges.get(0).getEnd()).isEqualTo(LocalTime.of(13, 0));
            assertThat(ranges.get(1).getStart()).isEqualTo(LocalTime.of(14, 0));
            assertThat(ranges.get(1).getEnd()).isEqualTo(LocalTime.of(18, 0));
        }
    }

    @Test
    void defaultWorkSchedule_returnsIndependentInstances() {
        // Mutating one returned schedule should not bleed into the next call.
        Map<DayOfWeek, List<TimeRange>> a = DoctorProfile.defaultWorkSchedule();
        a.get(DayOfWeek.MONDAY).clear();

        Map<DayOfWeek, List<TimeRange>> b = DoctorProfile.defaultWorkSchedule();
        assertThat(b.get(DayOfWeek.MONDAY)).hasSize(2);
    }
}