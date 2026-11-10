package com.example.genoclasifica.reclasificacion;

import com.example.genoclasifica.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findByCodigoMuestra(String codigoMuestra);

    boolean existsByCodigoMuestraAndGenIgnoreCaseAndPosicionAndCategoriaNueva(String codigoMuestra, String gen,
                                                                              Integer posicion, Categoria categoriaNueva);
}
