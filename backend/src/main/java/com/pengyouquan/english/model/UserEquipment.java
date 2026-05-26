package com.pengyouquan.english.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "user_equipment")
public class UserEquipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "equipment_id", nullable = false)
    private Long equipmentId;

    @Column(nullable = false)
    private Integer quantity = 1;
}
