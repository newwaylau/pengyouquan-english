package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    List<Equipment> findBySlot(String slot);

    List<Equipment> findByRarity(String rarity);

    @Query("SELECT e FROM Equipment e WHERE e.effectJson LIKE %:set%")
    List<Equipment> findByEffectJsonContaining(@Param("set") String set);

    List<Equipment> findAllByOrderBySlotAsc();
}
