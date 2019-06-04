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
import org.springframework.web.bind.annotation.ResponseBody;

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
    private ObjectMapper importMapper = new ObjectMapper(), exportMapper = new ObjectMapper();

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
        importMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        importMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        importMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

        exportMapper.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE);
        exportMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

        importMapper.findAndRegisterModules();
        exportMapper.findAndRegisterModules();

    }

    private String extractLastResourceFromPath(String path) {
        Assert.isTrue(path.contains("/"), () -> new HttpException(HttpStatus.BAD_REQUEST));
        return path.substring(path.lastIndexOf("/") + 1);
    }

    private Optional<Class<? extends AbstractAtkEntity>> mapEntityFromPath(String path) {
        String entityName = extractLastResourceFromPath(path);
        Optional<Class<? extends AbstractAtkEntity>> entityClass =
                entityClasses.stream().filter(e -> e.getSimpleName().equalsIgnoreCase(entityName + "Entity")).findFirst();
        return entityClass;
    }

    private Class<? extends AbstractAtkEntity> getEntityFromPath(String path) {
        Optional<Class<? extends AbstractAtkEntity>> entityClass = mapEntityFromPath(path);
        Assert.isTrue(entityClass.isPresent(), () -> new HttpException(HttpStatus.NOT_FOUND));
        return entityClass.get();

    }

    /**
     * override to intercept
     *
     * @param entity
     */
    public void beforeCreate(AbstractAtkEntity entity) {
    }

    @SneakyThrows
    @RequestMapping(method = RequestMethod.POST, path = "**")
    public void create(HttpServletRequest request, @RequestBody String entityBody) {

        AbstractAtkEntity entity = importMapper.readValue(entityBody, getEntityFromPath(request.getRequestURI()));
        beforeCreate(entity);
        entity.persist().insert(getDataSource());

    }

    /**
     * override to intercept
     *
     * @param entity
     */
    public void beforeUpate(AbstractAtkEntity entity) {}

    @SneakyThrows
    @RequestMapping(method = RequestMethod.PUT, path = "**")
    public void update(HttpServletRequest request, @RequestBody String entityBody) {
        // make sure it exists

        AbstractAtkEntity entity = getEntityFromPath(request.getRequestURI()).getConstructor().newInstance();
        entity.initFrom(importMapper.readValue(entityBody, entity.getBaseClass()));

        Assert.isTrue(entity.query().findById(getDataSource()).isPresent()
                , () -> new HttpException(HttpStatus.NOT_FOUND));

        beforeUpate(entity);

        entity.persist().update(getDataSource());

    }

    /**
     * override to intercept
     *
     * @param entity
     */
    public void beforeGet(AbstractAtkEntity entity) {
    }

    /**
     * override to intercept
     *
     * @param entityClass
     */
    public void beforeGetAll(Class<? extends AbstractAtkEntity> entityClass) {
    }

    @SneakyThrows
    public List<AbstractAtkEntity> getAll(Class<? extends AbstractAtkEntity> type) {
        AbstractAtkEntity entity = type.getConstructor().newInstance();
        return entity.query().getAll(getDataSource());
    }

    @SneakyThrows
    private AbstractAtkEntity getOne(String requestPath) {
        String id = extractLastResourceFromPath(requestPath);
        String path = requestPath.substring(0, requestPath.length() - id.length() - 1);
        AbstractAtkEntity entity = getEntityFromPath(path).getConstructor().newInstance();

        Assert.isTrue(entity.getEnFields().getIds().size() == 1, "Expected exactly one id field");

        entity.getEnFields().getIds().get(0).set(id);
        Optional<AbstractAtkEntity> found = entity.query().findById(getDataSource());

        Assert.isTrue(found.isPresent(), () -> new HttpException(HttpStatus.NOT_FOUND));

        return found.get();
    }

    @SneakyThrows
    @RequestMapping(method = RequestMethod.GET, path = "**")
    public @ResponseBody
    String get(HttpServletRequest request) {

        // could be get one or get all
        Optional<Class<? extends AbstractAtkEntity>> entityClass = mapEntityFromPath(request.getRequestURI());
        if (entityClass.isPresent()) {
            beforeGetAll(entityClass.get());
            return exportMapper.writeValueAsString(getAll(entityClass.get()));
        } else {
            AbstractAtkEntity entity = getOne(request.getRequestURI());
            beforeGet(entity);
            return exportMapper.writeValueAsString(entity);
        }
    }

    /**
     * override to intercept
     *
     * @param entity
     */
    public void beforeDelete(AbstractAtkEntity entity) {
    }

    @SneakyThrows
    @RequestMapping(method = RequestMethod.DELETE, path = "**")
    public void delete(HttpServletRequest request, @RequestBody String entityBody) {
        // make sure it exists

        AbstractAtkEntity entity = getOne(request.getRequestURI());
        beforeDelete(entity);
        entity.persist().delete(getDataSource());

    }
}
