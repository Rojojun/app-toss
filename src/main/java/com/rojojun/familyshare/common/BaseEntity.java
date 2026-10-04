package com.rojojun.familyshare.common;

import jakarta.persistence.*;
import org.springframework.data.domain.AfterDomainEventPublication;
import org.springframework.data.domain.DomainEvents;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@MappedSuperclass
public abstract class BaseEntity {

    @GeneratedValue(strategy = GenerationType.UUID)
    @Id
    private UUID id;

    @Column(name = "생성일")
    private LocalDateTime createdAt;

    @Transient
    private final List<Object> domainEvents = new ArrayList<>();

    public UUID getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    protected void registerEvent(Object event) {
        domainEvents.add(event);
    }

    // protected: Hibernate 프록시가 대상 객체로 위임하려면 package-private이면 안 된다.
    @DomainEvents
    protected Collection<Object> domainEvents() {
        return List.copyOf(domainEvents);
    }

    @AfterDomainEventPublication
    protected void clearDomainEvents() {
        domainEvents.clear();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now(Clock.systemUTC());
        }
    }
}
