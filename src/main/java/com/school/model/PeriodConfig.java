package com.school.model;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class PeriodConfig {

    private int       period;
    private String    label;
    private LocalTime startTime;
    private LocalTime endTime;
    private boolean   interval;

    private static final DateTimeFormatter T_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public PeriodConfig() {}

    /** e.g. "08:00 - 08:45" */
    public String getTimeRange() {
        if (startTime == null || endTime == null) return "";
        return T_FMT.format(startTime) + " - " + T_FMT.format(endTime);
    }

    /** Label + time range for the row header. e.g. "Period 1\n08:00-08:45" */
    public String getRowHeader() {
        return label + "\n" + getTimeRange();
    }

    // Getters / Setters
    public int getPeriod()                      { return period; }
    public void setPeriod(int v)                { this.period = v; }

    public String getLabel()                    { return label; }
    public void setLabel(String v)              { this.label = v; }

    public LocalTime getStartTime()             { return startTime; }
    public void setStartTime(LocalTime v)       { this.startTime = v; }

    public LocalTime getEndTime()               { return endTime; }
    public void setEndTime(LocalTime v)         { this.endTime = v; }

    public boolean isInterval()                 { return interval; }
    public void setInterval(boolean v)          { this.interval = v; }
}
