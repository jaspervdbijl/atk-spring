package com.acutus.atk.spring.database.exports;

import com.acutus.atk.beans.BeanHelper;
import com.acutus.atk.db.AbstractAtkEntity;
import com.acutus.atk.db.AtkEnField;
import com.acutus.atk.reflection.Reflect;
import com.acutus.atk.util.Assert;
import lombok.SneakyThrows;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.dhatim.fastexcel.reader.Cell;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
import org.dhatim.fastexcel.reader.Sheet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.acutus.atk.util.AtkUtil.getHumanFriendlyNameInsertSpace;

public class ExportImportService {

    private static List<Field> getExportFields(Class eClass) {
        return Reflect.getFields(eClass).filterAnnotation(ExportField.class).toList();
    }

    private static void validateHeader(AbstractAtkEntity entity, Row row) {
        List<Field> fields = getExportFields(entity.getClass());
        for (Cell cell : row.getCells(0, 0)) {
            ExportField exportField = fields.remove(0).getAnnotation(ExportField.class);
            Assert.isTrue(exportField.value().equalsIgnoreCase(cell.asString()), "Cell mismatch. Got header [" + cell.asString() + "]. expected [" + exportField.value() + "]");
        }
    }

    private static void write(Worksheet ws, Object value, int row, int c) {
        if (String.class.equals(value.getClass())) {
            ws.value(row, c, value.toString());
        } else if (Integer.class.equals(value.getClass())) {
            ws.value(row, c, (int) value);
        } else if (LocalDate.class.equals(value.getClass())) {
            ws.style(row, c).format("yyyy-MM-dd").set();
            ws.value(row, c, (LocalDate) value);
        } else if (LocalDateTime.class.equals(value.getClass())) {
            ws.style(row, c).format("yyyy-MM-dd HH:mm:ss").set();
            ws.value(row, c, (LocalDateTime) value);
        } else if (Boolean.class.equals(value.getClass())) {
            ws.value(row, c, (Boolean) value);
        } else if (Character.class.equals(value.getClass())) {
            ws.value(row, c, value.toString());
        } else {
            throw new UnsupportedOperationException("Type not implemented " + value.getClass() + " for value " + value);
        }
    }

    private static Object mapObject(Object value, Class from, Class to) {
        if (from.equals(to)) {
            return value;
        }
        if (value instanceof BigDecimal) {
            if (Integer.class.equals(to)) {
                return ((BigDecimal) value).intValue();
            } else if (Long.class.equals(to)) {
                return ((BigDecimal) value).longValue();
            } else if (Float.class.equals(to)) {
                return ((BigDecimal) value).floatValue();
            } else if (Double.class.equals(to)) {
                return ((BigDecimal) value).doubleValue();
            } else if (String.class.equals(to)) {
                return value.toString();
            } else {
                throw new UnsupportedOperationException("Type not implemented " + to + " for value " + value);
            }
        } else if (value instanceof Boolean) {
            if (Character.class.equals(to)) {
                return ((Boolean) value) ? 'Y' : 'N';
            } else if (Boolean.class.equals(to)) {
                return value;
            } else {
                throw new RuntimeException("Type not implemented " + to + " for value " + value);
            }
        } else if (value instanceof Character) {
            if (Character.class.equals(to)) {
                return value;
            } else if (Boolean.class.equals(to)) {
                return (Character) value == 'Y';
            } else {
                throw new RuntimeException("Type not implemented " + to + " for value " + value);
            }
        } else {
            try {
                return BeanHelper.decode(to, value.toString());
            } catch (Exception e) {
                throw new RuntimeException("Invalid mapping.\nExpected Type " + to.getSimpleName() + " but got value [" + value+"]");
            }
        }
    }

    private static Object decode(Class type, Cell cell) {
        if (cell.getValue() == null) {
            return null;
        }
        if (LocalDate.class.equals(type)) {
            return cell.asDate().toLocalDate();
        } else if (LocalDateTime.class.equals(type)) {
            return cell.asDate();
        } else
            return mapObject(cell.getValue(), cell.getValue().getClass(), type);
    }

    @SneakyThrows
    private static void initKf(Connection con, AbstractAtkEntity entity, Field field, ExportField exportField, Cell cell) {
        Class eClass = Class.forName(exportField.fKClass().getName() + "Entity");
        AbstractAtkEntity fnEntity = (AbstractAtkEntity) eClass.getConstructor().newInstance();
        Optional<AtkEnField> fkField = fnEntity.getEnFields().getByFieldName(exportField.fkIdField());
        Assert.isTrue(fkField.isPresent(), "Field not found " + exportField.fkIdField());
        fkField.get().set(decode(fkField.get().getType(), cell));
        Optional<AbstractAtkEntity> loaded = fnEntity.query().get(con);
        Assert.isTrue(loaded.isPresent(), "Entity not found from cell " + cell.getValue().toString());
        field.set(entity, loaded.get().getEnFields().getIds().get(0).get());
    }

    @SneakyThrows
    private static void initLookupValue(AbstractAtkEntity entity, Field field, ExportField exportField, Cell cell) {
        if (exportField.lookupKeys().length > 0) {
            int index = IntStream.range(0, exportField.lookupValues().length)
                    .filter(i -> exportField.lookupKeys()[i].equalsIgnoreCase(cell.asString()))
                    .findFirst().orElse(-1);
            Assert.isTrue(index != -1, "Lookup value not found for " + cell.getValue().toString());
            field.set(entity, exportField.lookupKeys()[index]);
        } else {
            field.set(entity, decode(field.getType(), cell));
        }
    }

    @SneakyThrows
    private static void init(Connection con, AbstractAtkEntity entity, Row row) {
        List<Field> fields = getExportFields(entity.getClass());
        for (Cell cell : row) {
            try {
                Field field = fields.remove(0);
                ExportField exportField = field.getAnnotation(ExportField.class);
                if (cell != null && cell.getValue() != null) {
                    if (!Void.class.equals(exportField.fKClass())) {
                        initKf(con, entity, field, exportField, cell);
                    } else if (exportField.lookupValues().length > 0) {
                        initLookupValue(entity, field, exportField, cell);
                    } else {
                        field.set(entity, decode(field.getType(), cell));
                    }
                }
            } catch (Exception ex) {
                throw new RuntimeException("Cell " + (cell.getColumnIndex()+1) + ". "+ex.getMessage(), ex);

            }
        }
    }

    @SneakyThrows
    private static void remapIdFields(AbstractAtkEntity entity, Map<Class, Map<Object, Object>> idMap) {
        for (Field field : getExportFields(entity.getClass())) {
            ExportField exportField = field.getAnnotation(ExportField.class);
            if (!Void.class.equals(exportField.fKClass())) {
                Class fkClass = Class.forName(exportField.fKClass().getName() + "Entity");
                if (idMap.containsKey(fkClass)) {
                    Object value = field.get(entity);
                    if (value != null && idMap.get(fkClass).containsKey(value)) {
                        field.set(entity, idMap.get(fkClass).get(value));
                    }
                }
            }
        }
    }

    @SneakyThrows
    public static void importFromExcel(Connection con, String filePath, Class<? extends AbstractAtkEntity>... eTypes) {
        Map<Class, Map<Object, Object>> idMap = new HashMap<>();
        try (FileInputStream file = new FileInputStream(filePath); ReadableWorkbook wb = new ReadableWorkbook(file)) {
            for (int index = 0; index < eTypes.length; index++) {
                Optional<Sheet> sheet = wb.getSheet(index);
                Assert.isTrue(sheet.isPresent(), "Missing Sheet for " + eTypes[index].getSimpleName());
                boolean headerInit = false;
                try (Stream<Row> rows = sheet.get().openStream()) {
                    for (Row row : rows.toList()) {
                        AbstractAtkEntity entity = eTypes[index].getConstructor().newInstance();
                        if (!headerInit) {
                            validateHeader(entity, row);
                            headerInit = true;
                        } else {
                            if (row.stream().filter(c -> c != null).findAny().isPresent()) {
                                try {
                                    init(con, entity, row);
                                    remapIdFields(entity, idMap);

                                    Object mappedId = entity.getEnFields().getSingleId().get();
                                    entity.getEnFields().getSingleId().set(null);
                                    entity.persist().insert(con);
                                    if (mappedId != null) {
                                        idMap.computeIfAbsent(eTypes[index], k -> new HashMap<>()).put(mappedId, entity.getEnFields().getSingleId().get());
                                    }
                                } catch (Exception ex) {
                                    throw new RuntimeException("Sheet ["+sheet.get().getName()+"] Error importing row " + row.getRowNum() + ".\n" + ex.getMessage(), ex);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @SneakyThrows
    private static void exportSample(Connection c, Worksheet ws, Class<? extends AbstractAtkEntity> entityClass) {
        AbstractAtkEntity entity = entityClass.getConstructor().newInstance();
        // export header
        List<Field> fields = Reflect.getFields(entity.getClass()).filterAnnotation(ExportField.class).toList();
        ws.range(0, 0, 0, fields.size() - 1).style().bold().fillColor("99A3A4").set();
        for (int i = 0; i < fields.size(); i++) {
            ExportField exportField = fields.get(i).getAnnotation(ExportField.class);
            ws.value(0, i, exportField.value());
        }
        Optional<AbstractAtkEntity> loaded = entity.query().get(c, "select * from " + entity.getTableName() + " order by " + entity.getEnFields().getIds().get(0).getColName() + " desc");
        if (loaded.isPresent()) {
            for (int i = 0; i < fields.size(); i++) {
                ExportField exportField = fields.get(i).getAnnotation(ExportField.class);
                AtkEnField enField = loaded.get().getEnFields().getByFieldName(fields.get(i).getName()).get();
                if (enField.get() != null) {
                    if (!Void.class.equals(exportField.fKClass())) {
                        // load fk entity
                        Class fkClass = Class.forName(exportField.fKClass().getName() + "Entity");
                        AbstractAtkEntity fkEntity = (AbstractAtkEntity) fkClass.getConstructor().newInstance();
                        fkEntity.getEnFields().getSingleId().set(enField.get());
                        fkEntity = (AbstractAtkEntity) fkEntity.query().get(c).get();
                        write(ws, fkEntity.getEnFields().getByFieldName(exportField.fkIdField()).get().get(), 1, i);
                    } else {
                        write(ws, enField.get(), 1, i);
                    }
                }
            }
        }
    }

    @SneakyThrows
    public static void exportSample(String name, Connection con, File file, Class<? extends AbstractAtkEntity>... eTypes) {
        try (FileOutputStream os = new FileOutputStream(file)) {
            try (Workbook wb = new Workbook(os, name, "1.0")) {
                for (Class<? extends AbstractAtkEntity> entityClass : eTypes) {
                    Worksheet ws = wb.newWorksheet(getHumanFriendlyNameInsertSpace(entityClass.getSimpleName().replace("Entity", "")));
                    exportSample(con, ws, entityClass);
                }
            }
        }
    }
}
