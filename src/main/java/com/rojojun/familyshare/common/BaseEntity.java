package com.rojojun.familyshare.common;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@MappedSuperclass
public abstract class BaseEntity {

    @GeneratedValue(strategy = GenerationType.UUID)
    @Id
    private UUID id;

    @Column(name = "생성일")
    private LocalDateTime createdAt;

    public UUID getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
