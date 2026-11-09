package com.example.genoclasifica.muestra;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface MuestraRepository extends JpaRepository<Muestra, Long> {

    Optional<Muestra> findByCodigo(String codigo);

    boolean existsByIdCorridaAndChecksumAndEstadoNotIn(String idCorrida, String checksum, Collection<EstadoMuestra> estados);
}
