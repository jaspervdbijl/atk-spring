package com.acutus.atk.spring.database.maintenance;

import com.acutus.atk.db.AbstractAtkEntity;
import com.acutus.atk.db.processor.AtkEntity;
import com.acutus.atk.db.sql.SQLHelper;
import com.acutus.atk.spring.util.Reflections;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

public abstract class MaintainEntityService {

    @Value("${db.maintain.enabled:true}")
    private boolean enabled;

    @SneakyThrows
    private void maintainData(Class c) {
        AtkEntity atk = (AtkEntity) c.getAnnotation(AtkEntity.class);
        String className = atk.className().isEmpty() ? c.getName() + atk.classNameExt() :
                atk.className();
        AbstractAtkEntity entity = (AbstractAtkEntity) Class.forName(className).getConstructor().newInstance();
        LocalDateTime time = LocalDateTime.now().minus(atk.trimD(),atk.trim());
        SQLHelper.executeUpdate(getDataSource(),String.format("delete from %s where created_date < ?",entity.getTableName()),time);
    }

    @Scheduled(cron = "${db.maintain.schedule:0 0 0 * * *}")
    public void maintainData() {
        Arrays.stream(getPaths()).forEach(p ->
                new Reflections(p)
                        .getTypesAnnotatedWith(AtkEntity.class)
                        .stream()
                        .filter(a -> !a.getAnnotation(AtkEntity.class).trim().equals(ChronoUnit.FOREVER) && a.getAnnotation(AtkEntity.class).trimD() > 0)
                        .forEach(this::maintainData));
    }

    public abstract String[] getPaths();

    public abstract DataSource getDataSource();
}
