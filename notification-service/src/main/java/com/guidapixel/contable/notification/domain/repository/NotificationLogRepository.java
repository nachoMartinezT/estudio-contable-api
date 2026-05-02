package com.guidapixel.contable.notification.domain.repository;

import com.guidapixel.contable.notification.domain.model.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
}
