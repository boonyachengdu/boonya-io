package com.boonya.lab.io.ota.repository;

import com.boonya.lab.io.ota.entity.Firmware;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FirmwareRepository extends JpaRepository<Firmware, Long> {

    List<Firmware> findByDeviceModelAndStatusOrderByCreateTimeDesc(String deviceModel, String status);

    Optional<Firmware> findByDeviceModelAndVersion(String deviceModel, String version);

    List<Firmware> findAllByOrderByCreateTimeDesc();
}
