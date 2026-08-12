package com.learnpulse.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "infrastructure_checks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InfrastructureCheckEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "component_name", nullable = false)
    private String componentName;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "checked_at", nullable = false)
    private Instant checkedAt;
}
