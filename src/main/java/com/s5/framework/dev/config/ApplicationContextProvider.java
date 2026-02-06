package com.s5.framework.dev.config;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Utility to access Spring beans from classes instantiated by external frameworks (like Flame).
 */
@Component
public class ApplicationContextProvider implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        context = applicationContext;
    }

    public static <T> T getBean(Class<T> clazz) {
        return context.getBean(clazz);
    }
}
