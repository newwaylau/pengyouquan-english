package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.UserEquipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserEquipmentRepository extends JpaRepository<UserEquipment, Long> {

    List<UserEquipment> findByUserId(Long userId);

    Optional<UserEquipment> findByUserIdAndEquipmentId(Long userId, Long equipmentId);

    int countByUserIdAndEquipmentId(Long userId, Long equipmentId);

    void deleteByUserIdAndEquipmentId(Long userId, Long equipmentId);
}
