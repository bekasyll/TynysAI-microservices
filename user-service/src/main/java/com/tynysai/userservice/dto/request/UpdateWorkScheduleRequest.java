package com.tynysai.userservice.dto.request;

import com.tynysai.userservice.model.TimeRange;
import lombok.Data;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

@Data
public class UpdateWorkScheduleRequest {
    private Map<DayOfWeek, List<TimeRange>> workSchedule;
}