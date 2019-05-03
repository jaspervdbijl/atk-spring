package com.acutus.atk.spring.database;

import com.acutus.atk.db.AbstractAtkEntity;
import com.acutus.atk.db.fe.FEHelper;
import com.acutus.atk.spring.util.Reflections;
import com.acutus.atk.util.Strings;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.acutus.atk.db.sql.SQLHelper.run;
import static com.acutus.atk.db.sql.SQLHelper.runAndReturn;

public class FEService {

    public static void maintainPackages(Connection connection, String... packages) {
        FEHelper.maintainDataDefinition(connection, Arrays.asList(packages)
                .stream()
                .map(p -> new Reflections(p).getSubTypesOf(AbstractAtkEntity.class))
                .flatMap(l -> l.stream())
                .collect(Collectors.toList()));
    }

    public static void maintainPackages(DataSource dataSource, String... packages) {
        run(dataSource, c -> maintainPackages(c,packages));
    }

}
