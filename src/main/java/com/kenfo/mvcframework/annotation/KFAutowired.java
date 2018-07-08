package com.kenfo.mvcframework.annotation;

import java.lang.annotation.*;

/**
 * @author kenfo
 * @version V1.0
 * @Description: TODO
 * @date 2018/2/11 下午1:36
 */
@Documented
//类和方法上使用
@Target({ElementType.FIELD})
//运行时
@Retention(RetentionPolicy.RUNTIME)
public @interface KFAutowired {
    String value() default  "";
}
