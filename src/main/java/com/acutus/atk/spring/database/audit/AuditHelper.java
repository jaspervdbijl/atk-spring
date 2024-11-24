package com.acutus.atk.spring.database.audit;

import com.acutus.atk.db.AbstractAtkEntity;
import com.acutus.atk.db.AtkEnField;
import com.acutus.atk.db.AtkEnFields;
import com.acutus.atk.db.processor.AtkEntity;

import java.sql.Connection;
import java.util.Base64;

public class AuditHelper {

    private static String serialise(Object value) {
        if (value != null) {
            if (value instanceof Byte[] || value instanceof byte[]) {
                return Base64.getEncoder().encodeToString(((byte[]) value));
            } else {
                return value.toString();
            }
        } else {
            return null;
        }
    }

    public static void audit(Connection connection, AbstractAtkEntity entity, boolean insert) {
        AtkEntity atkEntity = entity.getClass().getAnnotation(AtkEntity.class);
        if (atkEntity != null && atkEntity.auditChanges()) {
            AtkEnFields changed = insert ? entity.getEnFields().getSet() : entity.getEnFields().getChanged();
            AuditTableEntity auditTableEntity = new AuditTableEntity()
                    .setTableN(entity.getTableName())
                    .persist().insert(connection);
            for (AtkEnField field : changed) {
                new AuditTableFieldEntity().setAuditTableId(auditTableEntity.getId())
                        .setFieldName(field.getColName())
                        .setOldValue(serialise(field.getOldValue()))
                        .setNewValue(serialise(field.get()))
                        .persist().insert(connection);
            }
        }
    }
}
