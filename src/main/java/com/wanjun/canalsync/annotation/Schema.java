package com.wanjun.canalsync.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * Created by wangchengli on 2018/1/22
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Schema {
    String value() default "";
}
