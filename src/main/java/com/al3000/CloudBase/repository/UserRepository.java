package com.al3000.CloudBase.repository;

import com.al3000.CloudBase.model.UserDetailCustom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserDetailCustom, Integer> {
    Optional<UserDetailCustom> findByUsername(String name);
}
