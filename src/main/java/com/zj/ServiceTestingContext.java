package com.zj;

import lombok.Getter;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;

//@ComponentScan({"com.weimob.saas.ec.troop.common.testing*"})
public class ServiceTestingContext implements ApplicationContextAware {
    @Getter
    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        WebApplicationContext currentWebApplicationContext = ContextLoader.getCurrentWebApplicationContext();
        context = currentWebApplicationContext == null ? applicationContext : currentWebApplicationContext;
    }

    /**
     * 通过类名获取bean
     */
    public static <T> T getBean(Class<T> className) {
        return context.getBean(className);
    }

    /**
     * 通过bean名称获取
     */
    public static Object getBean(String beanName) {
        return context.getBean(beanName);
    }

    /**
     * 获取所有bean
     */
    public static String[] getBeans() {
        return context.getBeanDefinitionNames();
    }
}
