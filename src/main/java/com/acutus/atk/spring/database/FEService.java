package com.acutus.atk.spring.database;

import com.acutus.atk.db.AbstractAtkEntity;
import com.acutus.atk.db.fe.FEHelper;
import com.acutus.atk.db.processor.AtkEntity;
import com.acutus.atk.spring.util.Reflections;
import com.acutus.atk.util.Strings;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.acutus.atk.db.Persist.setPersistCallback;
import static com.acutus.atk.db.sql.SQLHelper.run;
import static com.acutus.atk.spring.database.audit.AuditHelper.audit;
import static com.acutus.atk.util.AtkUtil.handle;

public class FEService {

    static {
        setPersistCallback((connection, entity, isInsert) -> audit(connection, entity, isInsert));
    }

    public static void maintainPackages(Connection connection, List<String> packages, Strings excludes) {
        FEHelper.maintainDataDefinition(connection, packages
                .stream()
                .map(p -> new Reflections(p).getSubTypesOf(AbstractAtkEntity.class))
                .flatMap(l -> l.stream())
                .filter(a -> excludes.startsWith(a.getPackageName()).isEmpty() && handle(() -> a.getConstructor().newInstance().getEntityType() == AtkEntity.Type.TABLE))
                .collect(Collectors.toList()));
    }

    public static void maintainPackages(DataSource dataSource, String... packages) {
        run(dataSource, c -> maintainPackages(c, Arrays.asList(packages),new Strings()));
    }

    public static void maintainPackages(DataSource dataSource, List<String> packages, Strings excludes) {
        run(dataSource, c -> maintainPackages(c, packages,excludes));
    }

    public static void maintainClasses(DataSource dataSource, Class... classes) {
        run(dataSource, c -> FEHelper.maintainDataDefinition(c, classes));
    }

}
