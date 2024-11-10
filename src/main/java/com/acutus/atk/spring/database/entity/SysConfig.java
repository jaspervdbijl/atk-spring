package com.acutus.atk.spring.database.entity;

import com.acutus.atk.db.processor.AtkEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;

@AtkEntity(columnNamingStrategy = AtkEntity.ColumnNamingStrategy.CAMEL_CASE_UNDERSCORE, maintainEntity = true)
@Table(name = "sysconfig")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class SysConfig {

    @Id @Column(length = 100)
    private String name;

    @Column(length = 4096)
    String value;
}
