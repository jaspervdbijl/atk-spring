package com.acutus.atk.spring.controller;

import com.acutus.atk.db.AbstractAtkEntity;
import com.acutus.atk.spring.error.HttpException;
import com.acutus.atk.spring.util.Reflections;
import com.acutus.atk.util.Assert;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * this crud controller will provide basic crud services for all entities listed in a particular package
 */
public abstract class DefaultCrudController {

    private List<Class<? extends AbstractAtkEntity>> entityClasses;
    private ObjectMapper mapper = new ObjectMapper();

    public DefaultCrudController(String... packages) {
        init(packages);
    }

    public abstract DataSource getDataSource();

    void init(String... packages) {
        entityClasses = Arrays.asList(packages)
                .stream()
                .map(p -> new Reflections(p).getSubTypesOf(AbstractAtkEntity.class))
                .flatMap(l -> l.stream())
                .collect(Collectors.toList());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

    }

    private String extractEntityFromPath(String path) {
        Assert.isTrue(path.contains("/"), () -> new HttpException(HttpStatus.BAD_REQUEST));
        return path.substring(path.lastIndexOf("/") + 1);
    }

    private Class<? extends AbstractAtkEntity> getEntityFromPath(String path) {
        String entityName = extractEntityFromPath(path);
        Optional<Class<? extends AbstractAtkEntity>> entityClass =
                entityClasses.stream().filter(e -> e.getSimpleName().equalsIgnoreCase(entityName + "Entity")).findFirst();
        Assert.isTrue(entityClass.isPresent(), () -> new HttpException(HttpStatus.NOT_FOUND));
        return entityClass.get();
    }

    @SneakyThrows
    @RequestMapping(method = RequestMethod.POST, path = "**")
    public void create(HttpServletRequest request, @RequestBody String entityBody) {

        mapper.readValue(entityBody, getEntityFromPath(request.getRequestURI()))
                .persist()
                .insert(getDataSource());

    }

    @SneakyThrows
    @RequestMapping(method = RequestMethod.PUT, path = "**")
    public void update(HttpServletRequest request, @RequestBody String entityBody) {
        // make sure it exists

        AbstractAtkEntity entity = getEntityFromPath(request.getRequestURI()).getConstructor().newInstance();
        entity.initFrom(mapper.readValue(entityBody, entity.getBaseClass()));

        Assert.isTrue(entity.query().findById(getDataSource()).isPresent(), () -> new HttpException(HttpStatus.NOT_FOUND));

        entity.persist().update(getDataSource());

    }

//
//    @SneakyThrows
//    @RequestMapping(method = RequestMethod.GET , path = "**")
//    public void update(HttpServletRequest request, @RequestBody String entityBody) {
//
//        AbstractAtkEntity entity = getEntityFromPath(request.getRequestURI()).getConstructor().newInstance();
//
//        entity.initFrom(mapper.readValue(entityBody,entity.getBaseClass()))
//                .persist()
//                .update(getDataSource());
//    }

}
