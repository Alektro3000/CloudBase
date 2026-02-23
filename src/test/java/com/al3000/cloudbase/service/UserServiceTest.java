package com.al3000.cloudbase.service;

import com.al3000.cloudbase.model.UserDetailCustom;
import com.al3000.cloudbase.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, })
public class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;
    final String username = "alice";
    final String password = "password";

    @Test
    void loadUserByUsername_whenUserExists_returnsUserDetails() {
        // Arrange
        UserDetailCustom user = new UserDetailCustom(username, password);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        // Act
        UserDetails result = userService.loadUserByUsername(username);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(username);

        // Mock Assert
        verify(userRepository, only()).findByUsername(username);
    }
    @Test
    void loadUserByUsername_whenUserDoesNotExists_throwsUsernameNotFoundException() {
        // Arrange
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.loadUserByUsername(username))
                .isInstanceOf(UsernameNotFoundException.class);

        // Mock Assert
        verify(userRepository, only()).findByUsername(username);
    }
}
