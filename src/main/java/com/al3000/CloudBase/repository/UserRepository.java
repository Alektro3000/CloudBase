package com.al3000.CloudBase.repository;

import com.al3000.CloudBase.model.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserInfo, Integer> {
    Optional<UserInfo> findByUsername(String name);

    boolean existsByUsernameAndPassword(String username, String password);
}
