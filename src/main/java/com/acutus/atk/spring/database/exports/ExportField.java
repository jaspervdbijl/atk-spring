package com.acutus.atk.spring.database.exports;

import org.hibernate.id.factory.IdentifierGeneratorFactory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

import static com.acutus.atk.db.annotations.ForeignKey.Deferrability.NotDeferrable;

/*
 * Annotate Entity Fields with @ForeignKey to specify whether the field is a entity foreign key to a referring table
 * @param Class refers to the Entity class (referred table)
 * @param Field. Optional. Refers to the field in the entity. Default to the Entity'd id field
 * @param onUpdateAction. Optional. If the parent table is updated specify if the appropriate action. NoAction (default). Set Null, Set default value or Cascade
 * @param onUpdateDelete. Optional. If the parent table is deleted specify if the appropriate action. NoAction (default). Set Null, Set default value or Cascade
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ExportField {
    String value();
    String [] lookupValues() default {};
    String [] lookupKeys() default {};

    Class fKClass() default Void.class;
    String fkIdField() default "";
}
