package com.al3000.CloudBase.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Table(name = "usernames")
@Entity
@Getter
@Setter
@NoArgsConstructor
@RequiredArgsConstructor
public class UserDetailCustom implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id = null;

    @NonNull
    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @NonNull
    @Column(nullable = false)
    private String password;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    @NonNull
    public String getUsername() {
        return username;
    }
}
