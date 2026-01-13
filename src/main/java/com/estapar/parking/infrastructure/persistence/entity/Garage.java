package com.estapar.parking.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "garage")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Garage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private Boolean isDefault;
    
    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Integer maxCapacity;

    @OneToMany(mappedBy = "garage", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Sector> sectors;

}
