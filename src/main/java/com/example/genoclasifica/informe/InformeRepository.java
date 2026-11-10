package com.example.genoclasifica.informe;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InformeRepository extends JpaRepository<Informe, Long> {

    Optional<Informe> findByCodigoMuestra(String codigoMuestra);
}
