package com.boonya.lab.io.ota.repository;

import com.boonya.lab.io.ota.entity.OtaTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OtaTaskRepository extends JpaRepository<OtaTask, Long> {

    List<OtaTask> findByDeviceIdOrderByCreateTimeDesc(String deviceId);

    Optional<OtaTask> findFirstByDeviceIdAndStatusIn(String deviceId, List<String> statuses);

    List<OtaTask> findByFirmwareId(Long firmwareId);
}
