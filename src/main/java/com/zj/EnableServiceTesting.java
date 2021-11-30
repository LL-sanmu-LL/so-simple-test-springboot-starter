package com.zj;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 注解启用服务测试工具
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({ServiceTestingConfigure.class, ServiceTestingContext.class, ServiceTestingController.class})
@Documented
public @interface EnableServiceTesting {
    String name() default "";

    /**
     * 只获取符合过滤条件的服务名
     */
    String[] value() default "";
}
