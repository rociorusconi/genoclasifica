package com.example.genoclasifica.auditoria;

import org.h2.api.Trigger;

import java.sql.Connection;
import java.sql.SQLException;

// Trigger de H2: cualquier UPDATE o DELETE sobre la tabla falla, aunque se haga por SQL directo (CP-CG-04)
public class RechazarModificacion implements Trigger {

    private String tabla;

    @Override
    public void init(Connection conn, String schema, String trigger, String tabla, boolean antes, int tipo) {
        this.tabla = tabla;
    }

    @Override
    public void fire(Connection conn, Object[] filaAnterior, Object[] filaNueva) throws SQLException {
        throw new SQLException("La tabla " + tabla + " es inalterable: no admite modificaciones ni borrados");
    }
}
