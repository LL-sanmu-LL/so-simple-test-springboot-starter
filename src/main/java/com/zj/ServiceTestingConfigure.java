package com.zj;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

@Slf4j
public class ServiceTestingConfigure implements ImportBeanDefinitionRegistrar {
    public static String[] filters = new String[]{};

    @Override
    public void registerBeanDefinitions(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry beanDefinitionRegistry) {
        log.info("ServiceTestingConfigure init....");
        AnnotationAttributes attributes = AnnotationAttributes.fromMap(annotationMetadata
                .getAnnotationAttributes(EnableServiceTesting.class.getName()));
        filters = attributes.getStringArray("value");
    }

    public void setFilters(String[] filters) {
        ServiceTestingConfigure.filters = filters;
    }
}