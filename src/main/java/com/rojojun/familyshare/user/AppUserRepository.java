package com.rojojun.familyshare.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUserModel, UUID> {
    boolean existsByTossUserKey(Long tossUserKey);
    Optional<AppUserModel> findByTossUserKey(Long tossUserKey);
}
