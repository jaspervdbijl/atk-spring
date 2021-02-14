package com.acutus.atk.spring.database;

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
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import static com.acutus.atk.db.sql.SQLHelper.run;

@Component
@Slf4j
public abstract class AbstractUpgradeService {

    @Value("${database.upgrade.enabled:true}")
    private boolean upgradeEnabled;

    @Value("${database.liquibase.enabled:true}")
    private boolean liquibaseEnabled;

    @Autowired
    DataSource dataSource;

    protected abstract String[] getPackages();

    @PostConstruct
    @SneakyThrows
    public void maintainPackages() {
        if (upgradeEnabled) {
            runLiquibase("liquibase/masterlog/db.changelog.premaster.xml");
            FEService.maintainPackages(dataSource, getPackages());
            runLiquibase("liquibase/masterlog/db.changelog.postmaster.xml");
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
