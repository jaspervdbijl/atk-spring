package com.acutus.atk.spring.controller;

import com.acutus.atk.db.AbstractAtkEntity;
import com.acutus.atk.db.AtkEnField;
import com.acutus.atk.db.AtkEnFields;
import com.acutus.atk.db.Query;
import com.acutus.atk.db.sql.Filter;
import com.acutus.atk.util.Assert;
import com.acutus.atk.util.call.CallOne;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.acutus.atk.db.sql.Filter.Type.AND;

public interface AbstractCrudController<T extends AbstractAtkEntity> {

    public DataSource getDataSource();

    static ObjectMapper mapper = new ObjectMapper();
    static SimpleTypeConverter typeConverter = new SimpleTypeConverter();

    /**
     * get the generic type - T
     *
     * @return
     */
    public default Class<T> getType() {
        return (Class<T>) ((ParameterizedType) getClass().getGenericInterfaces()[0])
                .getActualTypeArguments()[0];
    }

    @SneakyThrows
    default T getInstance() {
        return getType().getConstructor().newInstance();
    }

    @SneakyThrows
    @RequestMapping(method = RequestMethod.GET)
    public default List getAll() {
        List values = new ArrayList();
        getInstance().query().getAll(getDataSource(), new Filter(), (CallOne<AbstractAtkEntity>) o -> values.add(o.toBase()), 100);
        return values;
    }

    @RequestMapping(method = RequestMethod.GET, path = "/{id}")
    public default @ResponseBody
    Object getById(@PathVariable("id") String id) {
        T instance = getInstance();
        AtkEnFields ids = instance.getEnFields().getIds();
        Assert.isTrue(ids.size() == 1, "Expected exactly one id field for " + getType());

        ids.get(0).set(typeConverter.convertIfNecessary(id, ids.get(0).getField().getType()));

        Optional<T> entity = (Optional<T>) instance.query().get(getDataSource());
        Assert.isTrue(entity.isPresent(), () -> new RuntimeException("Entity not found"));
        return entity.get().toBase();
    }

    @SneakyThrows
    @RequestMapping(method = RequestMethod.GET, path = "/queryBySet")
    public default @ResponseBody
    List<T> queryBySet(@RequestBody String entity, @RequestParam(required = false) String orderBy, @RequestParam(required = false) Boolean orderByAsc) {
        T instance = mapper.readValue(entity, getType());
        Query query = instance.query();
        if (orderBy != null) {
            orderByAsc = orderByAsc == null ? false : orderByAsc;
            query.setOrderBy(orderByAsc ? Query.OrderBy.ASC : Query.OrderBy.DESC, instance.getEnFields().getByColName(orderBy).get().getField());
        }
        List values = new ArrayList();
        query.getAll(getDataSource(), new Filter(AND, instance.getEnFields().getSet()), (e) -> {
            values.add(((AbstractAtkEntity)e).toBase());
        }, 100);
        return values;
    }

    @RequestMapping(method = RequestMethod.POST)
    public default void create(@RequestBody T entity) {
        getInstance().persist().insert(getDataSource());
    }

    @RequestMapping(method = RequestMethod.PUT)
    public default void update(@RequestBody T entity) {
        // TODO only certain fields may be updated
        entity.getFields().stream().forEach(f -> ((AtkEnField)f).setSet(true));
        entity.persist().update(getDataSource());
    }


}
