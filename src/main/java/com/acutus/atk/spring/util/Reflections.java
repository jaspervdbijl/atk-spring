package com.acutus.atk.spring.util;

import com.acutus.atk.util.call.CallNilRet;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.stream.Collectors;

import static com.acutus.atk.util.AtkUtil.handle;


public class Reflections {

    static ClassPathScanningCandidateComponentProvider DEF_PROVIDER = new ClassPathScanningCandidateComponentProvider(false);

    String path;

    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);

    public Reflections(String path) {
        this.path = path;
    }

    public <T> List<Class<? extends T>> getSubTypesOf(Class<T> type) {
        provider.resetFilters(false);
        provider.addIncludeFilter(new AssignableTypeFilter(type));
        return (List<Class<? extends T>>) provider.findCandidateComponents(path).stream()
                .map(b -> handle(() -> ((T) Class.forName(b.getBeanClassName())))).collect(Collectors.toList());
    }

    public <T> List<Class<T>> getTypesAnnotatedWith(final Class<? extends Annotation> type) {
        provider.resetFilters(false);
        return getTypesAnnotatedWith(path,provider,type);

    }

    public static <T> List<Class<T>> getTypesAnnotatedWith(String path, final Class<? extends Annotation> type) {
        return getTypesAnnotatedWith(path,DEF_PROVIDER,type);
    }

    private static <T> List<Class<T>> getTypesAnnotatedWith(String path
            , ClassPathScanningCandidateComponentProvider provider, final Class<? extends Annotation> type) {
        provider.resetFilters(false);
        provider.addIncludeFilter(new AnnotationTypeFilter(type));
        return (List<Class<T>>) provider.findCandidateComponents(path).stream()
                .map(b -> handle(() -> ((T) Class.forName(b.getBeanClassName())))).collect(Collectors.toList());
    }


}
