package com.ximalaya.business.Annotation.utils;

public class ConstStrings {
    public static String flowGradeString(int grade) {
        switch (grade) {
            case 0: return "FLOW_GRADE_THREAD";
            case 1: return "FLOW_GRADE_QPS";
            default: return "unknown flow grade";
        }
    }

    public static String degradeGradeString(int grade) {
        switch (grade) {
            case 0: return "DEGRADE_GRADE_RT";
            case 1: return "DEGRADE_GRADE_EXCEPTION_RATIO";
            case 2: return "DEGRADE_GRADE_EXCEPTION_COUNT";
            default: return "unknown degrade grade";
        }
    }

    public static String behaviorGradeString(int behavior) {
        switch (behavior) {
            case 0: return "CONTROL_BEHAVIOR_DEFAULT";
            case 1: return "CONTROL_BEHAVIOR_WARM_UP";
            case 2: return "CONTROL_BEHAVIOR_RATE_LIMITER";
            case 3: return "CONTROL_BEHAVIOR_WARM_UP_RATE_LIMITER";
            default: return "unknown behavior grade";
        }
    }
}
