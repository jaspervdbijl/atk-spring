package com.acutus.atk.spring.controller;

import com.acutus.atk.db.AbstractAtkEntity;
import com.acutus.atk.db.AtkEnFields;
import com.acutus.atk.db.Persist;
import com.acutus.atk.db.Query;
import com.acutus.atk.db.sql.Filter;
import com.acutus.atk.reflection.Reflect;
import com.acutus.atk.reflection.ReflectMethods;
import com.acutus.atk.util.Assert;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Optional;

public interface AbstractCrudController<T extends AbstractAtkEntity> {

    public DataSource getDataSource();

    /**
     * get the generic type - T
     * @return
     */
    public default Class<T> getType() {
        return (Class<T>) ((ParameterizedType) getClass()
                .getGenericSuperclass()).getActualTypeArguments()[0];
    }

    @SneakyThrows
    default Method getMethod(String name) {
        ReflectMethods methods = Reflect.getMethods(getType()).filter(name).filterParams();
        Assert.isTrue(methods.size() == 1,"Expected a %s method in %s ",name, getType().getName());
        return methods.get(0);
    }

    @SneakyThrows
    default Query getQuery(T entity) {
        return (Query) getMethod("query").invoke(entity);
    }

    @SneakyThrows
    default Persist getPersist(T entity) {
        return (Persist) getMethod("persist").invoke(entity);
    }

    @SneakyThrows
    default T getInstance() {
        return getType().getConstructor().newInstance();
    }

    @SneakyThrows
    @RequestMapping(method = RequestMethod.GET)
    public default List<T> getAll() {
        return getQuery(getInstance()).getAll(getDataSource(),new Filter());
    }

    @RequestMapping(method = RequestMethod.GET,path = "/{id}")
    public default @ResponseBody T getById(@PathVariable("id") String id) {
        T instance = getInstance();
        AtkEnFields ids = instance.getEnFields().getIds();
        Assert.isTrue(ids.size() == 1,"Expected exactly one id field for " + getType());

        ids.get(0).set(id);

        Optional<T> entity = (Optional<T>) getQuery(instance).getBySet(getDataSource());
        Assert.isTrue(entity.isPresent(),() -> new RuntimeException("Entity not found"));
        return entity.get();
    }

    @RequestMapping(method = RequestMethod.POST)
    public default void create(@RequestBody T entity) {
        getPersist(getInstance()).insert(getDataSource());
    }

    @RequestMapping(method = RequestMethod.PUT)
    public default void update(@RequestBody T entity) {
        getPersist(getInstance()).update(getDataSource());
    }

}
