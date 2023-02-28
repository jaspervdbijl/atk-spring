package com.acutus.atk.spring.database.audit;

import com.acutus.atk.db.annotations.ForeignKey;
import com.acutus.atk.db.annotations.UID;
import com.acutus.atk.db.processor.AtkEntity;

import javax.persistence.Id;
import javax.persistence.Lob;

@AtkEntity(enableAuditByUser = true)
public class AuditTableField {
    @UID
    @Id
    private String id;

    @ForeignKey(table = AuditTable.class, field = "id", name = "audTableIdFk", onDeleteAction = ForeignKey.Action.Cascade)
    private String auditTableId;

    private String fieldName;
    @Lob
    private String oldValue;
    @Lob
    private String newValue;

}
