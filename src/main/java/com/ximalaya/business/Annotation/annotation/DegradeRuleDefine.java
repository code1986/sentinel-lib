package com.ximalaya.business.Annotation.annotation;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Inherited
@Documented
public @interface DegradeRuleDefine {
    String timeWindow() default "10"; // seconds
    String count() default "500";
    String app() default "default";
    int grade() default RuleConstant.DEGRADE_GRADE_RT;
}
