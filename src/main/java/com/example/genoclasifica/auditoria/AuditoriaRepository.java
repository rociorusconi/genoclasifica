package com.example.genoclasifica.auditoria;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditoriaRepository extends JpaRepository<RegistroAuditoria, Long> {

    List<RegistroAuditoria> findByCodigoMuestraOrderByIdAsc(String codigoMuestra);
}
