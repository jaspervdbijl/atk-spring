package com.acutus.atk.spring.database;

import com.acutus.atk.db.sql.SQLHelper;
import com.acutus.atk.spring.util.properties.FileResource;
import com.acutus.atk.util.IOUtil;
import com.acutus.atk.util.Strings;
import com.acutus.atk.util.collection.Tuple1;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.acutus.atk.db.sql.SQLHelper.run;
import static com.acutus.atk.util.AtkUtil.convertStackTraceToString;
import static com.acutus.atk.util.AtkUtil.handle;
import static com.acutus.atk.util.StringUtils.isEmpty;
import static org.bouncycastle.util.io.Streams.readAll;

@Component
@Slf4j
public abstract class AbstractUpgradeService {

    @Value("${database.upgrade.enabled:false}")
    private boolean upgradeEnabled;

    @Value("${database.liquibase.enabled:false}")
    private boolean liquibaseEnabled;

    @Autowired
    DataSource dataSource;

    @Autowired
    ResourceLoader resourceLoader;

    @Value("classpath:scripts/*.sql")
    Resource[] resources;

    protected abstract String[] getPackages();

    protected String[] getExcludes() {
        return new String[]{};
    }

    @PostConstruct
    @SneakyThrows
    public void maintainPackages() {
        if (upgradeEnabled) {
            runScripts();
            runLiquibase("liquibase/masterlog/db.changelog.premaster.xml");
            FEService.maintainPackages(dataSource, Arrays.asList(getPackages()), new Strings(getExcludes()));
            runLiquibase("liquibase/masterlog/db.changelog.postmaster.xml");
        }
    }

    @SneakyThrows
    protected void runScripts() {
        SQLHelper.execute(dataSource, """
                create table if not exists m_scripts 
                (
                    filename      varchar(200) not null,
                    exec_time     datetime     not null,
                    exec_duration int          not null,
                    status        varchar(1)   not null,
                    error_log     text         null,
                    constraint m_scripts_pk
                        primary key (filename)
                );""");
        // scan the files and execute one by one
        List<String> executed = SQLHelper.query(dataSource, String.class, "select filename from m_scripts").stream()
                .map(a -> a.getFirst()).collect(Collectors.toList());
        for (Resource resource : Arrays.stream(resources).sorted(Comparator.comparing(Resource::getFilename))
                .collect(Collectors.toList())) {
            if (!executed.contains(resource.getFilename())) {
                long runtime = System.currentTimeMillis();
                Strings errors = new Strings();
                String lines = new String(IOUtil.readAvailable(new FileInputStream(resource.getFile())));
                Arrays.stream(lines.split("#GO")).filter(l -> !isEmpty(l)).forEach(l -> handle(() -> SQLHelper.execute(dataSource,l),
                        (ex) -> errors.add(convertStackTraceToString(ex,1024))));
                runtime = System.currentTimeMillis() - runtime;
                SQLHelper.executeUpdate(dataSource,"insert into m_scripts(filename,exec_time,exec_duration,status,error_log) values (?,?,?,?,?)",
                        resource.getFilename(),new Timestamp(System.currentTimeMillis()),runtime,errors.isEmpty() ? "P":"E",errors.toString("\n\n"));
            }
        }

    }

    protected void runLiquibase(String file) {
        if (liquibaseEnabled) {
            log.info("Start runLiquibase master");
            run(dataSource, connection -> {
                Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
                Liquibase liquibase = new Liquibase(file, new ClassLoaderResourceAccessor(), database);
                liquibase.update((Contexts) null);
            });
            log.info("Finished runLiquibase");
        }
    }

}
