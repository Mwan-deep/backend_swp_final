package AI_Study_Hub.repository;

import AI_Study_Hub.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, String>, JpaSpecificationExecutor<Device> {
     Optional<Device> findByDeviceId(String deviceId);
     List<Device> findByAccountId(Long accountId);
}