package com.veefin.ap2.repository;

import com.veefin.ap2.entity.AP2AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AP2AuditRepository extends JpaRepository<AP2AuditLog, Long> {
}
