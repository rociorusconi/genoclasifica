package com.example.genoclasifica.auditoria;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

// Crea los triggers después de que Hibernate crea las tablas (por eso depende del EntityManagerFactory)
@Component
public class TablasInalterables {

    private static final String[] TABLAS = {"AUDITORIA", "INFORME"};

    private final JdbcTemplate jdbc;

    public TablasInalterables(JdbcTemplate jdbc, EntityManagerFactory tablasCreadas) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    void crearTriggers() {
        for (String tabla : TABLAS) {
            for (String operacion : new String[]{"UPDATE", "DELETE"}) {
                jdbc.execute("CREATE TRIGGER IF NOT EXISTS NO_" + operacion + "_" + tabla + " BEFORE " + operacion
                    + " ON " + tabla + " FOR EACH ROW CALL '" + RechazarModificacion.class.getName() + "'");
            }
        }
    }
}
