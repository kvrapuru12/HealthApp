package com.healthapp.repository;

import com.healthapp.entity.NotificationDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationDeviceRepository extends JpaRepository<NotificationDevice, Long> {
    Optional<NotificationDevice> findByUserIdAndDeviceId(Long userId, String deviceId);
    Optional<NotificationDevice> findByExpoPushToken(String expoPushToken);
    Optional<NotificationDevice> findByIdAndUserId(Long id, Long userId);
    List<NotificationDevice> findByUserIdAndStatus(Long userId, NotificationDevice.Status status);
}
