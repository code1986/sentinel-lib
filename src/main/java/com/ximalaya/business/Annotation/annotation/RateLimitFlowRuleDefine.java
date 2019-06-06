package com.ximalaya.business.Annotation.annotation;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Inherited
@Documented
public @interface RateLimitFlowRuleDefine {
    String count() default "4000";
    String app() default "default";
    String rateLimit() default "500";
    int grade() default RuleConstant.FLOW_GRADE_QPS;
}
