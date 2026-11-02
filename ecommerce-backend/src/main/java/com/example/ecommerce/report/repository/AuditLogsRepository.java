package com.example.ecommerce.report.repository;

import com.example.ecommerce.report.model.AuditLogs;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogsRepository extends JpaRepository<AuditLogs, Long> {
}
