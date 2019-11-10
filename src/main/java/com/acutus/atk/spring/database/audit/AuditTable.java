package com.acutus.atk.spring.database.audit;

import com.acutus.atk.db.annotations.UID;
import com.acutus.atk.db.processor.AtkEntity;
import lombok.Data;

import javax.persistence.Id;
import javax.persistence.Table;

@AtkEntity(columnNamingStrategy = AtkEntity.ColumnNamingStrategy.CAMEL_CASE_UNDERSCORE,addAuditFields = true)
@Table()
@Data
public class AuditTable {
    @UID @Id
    private String id;

    private String tableName;

}
