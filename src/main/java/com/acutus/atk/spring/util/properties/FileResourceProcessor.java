package com.acutus.atk.spring.util.properties;

import java.io.InputStream;

import com.acutus.atk.io.IOUtil;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import static com.acutus.atk.util.AtkUtil.handle;

@Component
public class FileResourceProcessor implements BeanPostProcessor {
    public Object postProcessAfterInitialization(Object bean, String beanName)
            throws BeansException {
        return bean;
    }

    public Object postProcessBeforeInitialization(final Object bean, String beanName)
            throws BeansException {
        ReflectionUtils.doWithFields(bean.getClass(), field -> {
            if (field.getAnnotation(FileResource.class) != null) {
                FileResource fs = field.getAnnotation(FileResource.class);
                InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(fs.value());
                field.setAccessible(true);
                if (field.getType().equals(byte[].class)) {
                    handle(() -> field.set(bean, IOUtil.readAvailable(is)));
                } else if (field.getType().equals(String.class)) {
                    handle(() -> field.set(bean, new String(IOUtil.readAvailable(is))));
                } else {
                    throw new RuntimeException("Unsupported FileResoure type " + field.getType());
                }
                field.setAccessible(false);
            }
        });
        return bean;
    }
}
