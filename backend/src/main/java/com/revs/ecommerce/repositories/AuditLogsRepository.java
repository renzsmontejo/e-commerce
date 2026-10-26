package com.revs.ecommerce.repositories;

import com.revs.ecommerce.models.AuditLogs;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogsRepository extends JpaRepository<AuditLogs, Long> {
}
