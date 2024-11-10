package com.acutus.atk.spring.util.properties;

import com.acutus.atk.spring.database.entity.SysConfigEntity;
import com.acutus.atk.util.Assert;
import lombok.SneakyThrows;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class DbValueProcessor implements BeanPostProcessor {

    @Autowired
    DataSource dataSource;

    private Map<Object, Field> beans = new HashMap<>();


    private Optional<String> getDefault(String value) {
        return Optional.ofNullable(value.contains(":") ? value.substring(value.indexOf(":")+1) : null);
    }

    private String getConfigFromDB(String name, Optional<String> defValue) {
        Optional<SysConfigEntity> sysConfig = new SysConfigEntity().setName(name).query().get(dataSource);
        Assert.isTrue(sysConfig.isPresent() || defValue.isPresent(),"Config value not found: " + name);
        if (sysConfig.isEmpty()) {
            sysConfig = Optional.of(new SysConfigEntity().setName(name).setValue(defValue.get()).persist().insert(dataSource));
        }
        return sysConfig.get().getValue();
    }

    @SneakyThrows
    private Object getValue(Object bean,Field field) {
        DbValue dbValue = field.getAnnotation(DbValue.class);
        String value = dbValue.value().replace("${","").replace("}","");
        String name = value.contains(":") ? value.substring(0,value.indexOf(":")) : value;

        String sysValue = getConfigFromDB(name,getDefault(value));

        return String.class.equals(field.getType()) ? sysValue
                :  field.getType().getMethod("valueOf", String.class).invoke(bean, sysValue);
    }

    public Object postProcessAfterInitialization(Object bean, String beanName)
            throws BeansException {
        return bean;
    }

    public Object postProcessBeforeInitialization(final Object bean, String beanName)
            throws BeansException {
        ReflectionUtils.doWithFields(bean.getClass(), field -> {
            if (field.getAnnotation(DbValue.class) != null) {
                field.setAccessible(true);
                refreshBean(bean, field);
                Scope scope = bean.getClass().getAnnotation(Scope.class);
                if (scope == null || !scope.value().equals("prototype")) {
                    beans.put(bean, field);
                }
            }
        });
        return bean;
    }

    @SneakyThrows
    private void refreshBean(Object bean,Field field) {
        field.set(bean,getValue(bean,field));
    }

    @Scheduled(fixedDelay = 5000)
    public void refreshCachedValues() {
        beans.keySet().stream().forEach(k -> refreshBean(k,beans.get(k)));
    }
}
