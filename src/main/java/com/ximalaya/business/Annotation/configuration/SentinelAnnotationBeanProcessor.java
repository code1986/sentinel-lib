package com.ximalaya.business.Annotation.configuration;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.ximalaya.business.Annotation.annotation.DegradeRuleDefine;
import com.ximalaya.business.Annotation.annotation.FlowRuleDefine;
import com.ximalaya.business.Annotation.annotation.RateLimitFlowRuleDefine;
import com.ximalaya.business.Annotation.annotation.WarmUpFlowRuleDefine;
import com.ximalaya.business.Annotation.utils.ConstStrings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.env.Environment;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class SentinelAnnotationBeanProcessor implements ApplicationContextAware, BeanPostProcessor, ApplicationListener<ContextRefreshedEvent> {

    private static Logger logger = LoggerFactory.getLogger(SentinelAnnotationBeanProcessor.class);

    List<FlowRule> flowRules = new ArrayList<FlowRule>();
    List<DegradeRule> degradeRules = new ArrayList<DegradeRule>();

    private Environment environment;
    private EvaluationContext context;
    private SpelExpressionParser parser = new SpelExpressionParser();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        environment = applicationContext.getEnvironment();
        context = new StandardEvaluationContext(environment);
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

        String className = AopUtils.getTargetClass(bean).getSimpleName();

        for (Method method : AopUtils.getTargetClass(bean).getDeclaredMethods()) {
            if (method.isAnnotationPresent(SentinelResource.class)) {

                SentinelResource annotationSentinel = method.getAnnotation(SentinelResource.class);

                String flowControlHandler = annotationSentinel.blockHandler();
                if (flowControlHandler != null && !flowControlHandler.isEmpty()) {
                    boolean foundFlowRule = false;
                    FlowRuleDefine flowDef = method.getAnnotation(FlowRuleDefine.class);
                    if (flowDef != null) {
                        loadFlowRule(flowDef, annotationSentinel, className, method);
                        foundFlowRule = true;
                    }

                    WarmUpFlowRuleDefine warmDef = method.getAnnotation(WarmUpFlowRuleDefine.class);
                    if (warmDef != null) {
                        loadWarmupFlowRule(warmDef, annotationSentinel, className, method);
                        foundFlowRule = true;
                    }

                    RateLimitFlowRuleDefine rateDef = method.getAnnotation(RateLimitFlowRuleDefine.class);
                    if (rateDef != null) {
                        loadRateLimitFlowRule(rateDef, annotationSentinel, className, method);
                        foundFlowRule = true;
                    }

                    if (!foundFlowRule ){
                        logger.warn("SENTINEL   FLOW  ==> not found @FlowRuleDefine at [{}.{}], remove `blockHandler` or add @FlowRuleDefine", className, method.getName());
                    }
                }


                String degradeControlHandler = annotationSentinel.fallback();
                if (degradeControlHandler != null && !degradeControlHandler.isEmpty()) {
                    DegradeRuleDefine degradeDef = method.getAnnotation(DegradeRuleDefine.class);
                    if (degradeDef != null) {
                        loadDegradeRule(degradeDef, annotationSentinel, className, method);
                    } else {
                        logger.warn("SENTINEL DEGRADE ==> not found @DegradeRuleDefine at [{}.{}], remove `fallback` or add @DegradeRuleDefine", className, method.getName());
                    }
                }
            }
        }
        return bean;
    }

    private void loadRateLimitFlowRule(RateLimitFlowRuleDefine flowDef, SentinelResource annotationSentinel, String className, Method method) {
        FlowRule rule = new FlowRule();
        rule.setResource(annotationSentinel.value());
        rule.setCount(getDouble(flowDef.count()));
        rule.setGrade(flowDef.grade());
        rule.setLimitApp(flowDef.app());
        rule.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_RATE_LIMITER);
        rule.setMaxQueueingTimeMs(getInt(flowDef.rateLimit()));

        flowRules.add(rule);

        logger.info("SENTINEL RateLimit FLOW  ==> resource \"{}\" on METHOD: ({}.{}), rule: <grade:{}, count:{}, maxQueueTimeMS:{}, behavior:{}>",
                annotationSentinel.value(), className, method.getName(),
                ConstStrings.flowGradeString(rule.getGrade()), rule.getCount(), rule.getMaxQueueingTimeMs(),
                ConstStrings.behaviorGradeString(rule.getControlBehavior()));
    }

    private void loadWarmupFlowRule(WarmUpFlowRuleDefine flowDef, SentinelResource annotationSentinel, String className, Method method) {
        FlowRule rule = new FlowRule();
        rule.setResource(annotationSentinel.value());
        rule.setCount(getDouble(flowDef.count()));
        rule.setGrade(flowDef.grade());
        rule.setWarmUpPeriodSec(getInt(flowDef.warmUpPeriodSec()));
        rule.setLimitApp(flowDef.app());
        if (!flowDef.rateLimit().isEmpty()) {
            rule.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_WARM_UP_RATE_LIMITER);
            rule.setMaxQueueingTimeMs(getInt(flowDef.rateLimit()));

            logger.info("SENTINEL WarmUp FLOW  ==> resource \"{}\" on METHOD: ({}.{}), rule: <grade:{}, count:{}, WarmUpPeriodSec:{}, maxQueueTimeMS:{} behavior:{}>",
                    annotationSentinel.value(), className, method.getName(),
                    ConstStrings.flowGradeString(flowDef.grade()), rule.getCount(), rule.getWarmUpPeriodSec(), rule.getMaxQueueingTimeMs(),
                    ConstStrings.behaviorGradeString(rule.getControlBehavior()));

        } else {
            rule.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_WARM_UP);

            logger.info("SENTINEL WarmUp FLOW  ==> resource \"{}\" on METHOD: ({}.{}), rule: <grade:{}, count:{}, WarmUpPeriodSec:{}, behavior:{}>",
                    annotationSentinel.value(), className, method.getName(),
                    ConstStrings.flowGradeString(flowDef.grade()), rule.getCount(), rule.getWarmUpPeriodSec(),
                    ConstStrings.behaviorGradeString(rule.getControlBehavior()));
        }

        flowRules.add(rule);

    }

    private void loadFlowRule(FlowRuleDefine flowDef, SentinelResource annotationSentinel, String className, Method method) {
        FlowRule rule = new FlowRule();
        rule.setResource(annotationSentinel.value());
        rule.setCount(getDouble(flowDef.count()));
        rule.setGrade(flowDef.grade());
        rule.setControlBehavior(flowDef.behavior());
        rule.setLimitApp(flowDef.app());
        flowRules.add(rule);
        logger.info("SENTINEL   FLOW  ==> resource \"{}\" on METHOD: ({}.{}), rule: <grade:{}, count:{}, behavior:{}>",
                annotationSentinel.value(), className, method.getName(),
                ConstStrings.flowGradeString(flowDef.grade()), rule.getCount(),
                ConstStrings.behaviorGradeString(flowDef.behavior()));
    }

    private void loadDegradeRule(DegradeRuleDefine degradeDef, SentinelResource annotationSentinel, String className, Method method) {
        DegradeRule rule = new DegradeRule();
        rule.setResource(annotationSentinel.value());
        rule.setCount(getDouble(degradeDef.count()));
        rule.setGrade(degradeDef.grade());
        rule.setTimeWindow(getInt(degradeDef.timeWindow()));
        rule.setLimitApp(degradeDef.app());
        degradeRules.add(rule);
        logger.info("SENTINEL DEGRADE ==> resource \"{}\" on METHOD: ({}.{}), rule: <grade:{}, count:{}, timeWindow:{}(s)>",
                annotationSentinel.value(), className, method.getName(),
                ConstStrings.degradeGradeString(degradeDef.grade()),
                rule.getCount(), rule.getTimeWindow());
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        logger.info("SENTINEL         ==> load {} flow rules", flowRules.size());
        FlowRuleManager.loadRules(flowRules);
        logger.info("SENTINEL         ==> load {} degrade rules", degradeRules.size());
        DegradeRuleManager.loadRules(degradeRules);
    }

    public String parserSPEL(String expr) {
        String key;
        if (expr.charAt(1) == '{') {
            // ${xxx} format
            key = expr.substring(2, expr.length()-1);
        } else {
            // $xxx format
            key = expr.substring(1);
        }

        return parser.parseExpression(key).getValue(context, String.class);
    }

    public String readProperty(String expr) {
        String key;
        if (expr.charAt(1) == '{') {
            // ${xxx} format
            key = expr.substring(2, expr.length()-1);
        } else {
            // $xxx format
            key = expr.substring(1);
        }

        return environment.getProperty(key);
    }

    String getProperty(String expr) {
        expr = expr.trim();
        if (expr.isEmpty()) {
            return expr;
        }

        switch (expr.charAt(0)) {
            case '#': return parserSPEL(expr);
            case '$': return readProperty(expr);
            default: return expr;
        }
    }

    double getDouble(String expr) {
        return Double.parseDouble(getProperty(expr));
    }

    int getInt(String expr) {
        return Integer.parseInt(getProperty(expr));
    }

}
