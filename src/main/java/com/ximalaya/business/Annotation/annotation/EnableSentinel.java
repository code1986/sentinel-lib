package com.ximalaya.business.Annotation.annotation;

import com.ximalaya.business.Annotation.configuration.SentinelConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Inherited
@Documented
@Import({SentinelConfiguration.class})
public @interface EnableSentinel {
}
