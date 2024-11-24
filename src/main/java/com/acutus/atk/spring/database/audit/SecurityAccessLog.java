package com.acutus.atk.spring.database.audit;

import com.acutus.atk.db.processor.AtkEntity;
import lombok.Data;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@AtkEntity(columnNamingStrategy = AtkEntity.ColumnNamingStrategy.CAMEL_CASE_UNDERSCORE, audit = true, maintainEntity = true)
@Table(name = "a_security_access_log")
@Data
public class SecurityAccessLog {
    @Id
    @GeneratedValue()
    private Integer id;

    private String action,detail,status,host;

}
