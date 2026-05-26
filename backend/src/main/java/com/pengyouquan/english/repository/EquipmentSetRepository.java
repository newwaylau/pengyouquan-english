package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.EquipmentSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EquipmentSetRepository extends JpaRepository<EquipmentSet, Long> {
    Optional<EquipmentSet> findBySetKey(String setKey);
}
