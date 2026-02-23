package com.al3000.cloudbase.service;

import com.al3000.cloudbase.dto.LoginInfo;
import com.al3000.cloudbase.dto.UserName;
import com.al3000.cloudbase.exception.UserAlreadyExistException;
import com.al3000.cloudbase.exception.UserNotFoundException;
import com.al3000.cloudbase.model.UserDetailCustom;
import com.al3000.cloudbase.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LoginServiceTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuthenticationManager authenticationManager;

    private LoginService loginService;

    final String username = "alice";
    final String password = "password";
    final String codedPassword = encoder.encode(password);
    final String wrongPassword = encoder.encode(password + "!");
    final LoginInfo loginInfo = new LoginInfo(username, password);
    final UserDetailCustom userDetailCustom = new UserDetailCustom(username, codedPassword);
    final UserDetailCustom userDetailCustomWrongPassword = new UserDetailCustom(username, wrongPassword);
    @BeforeEach
    void setUp() {
        loginService = new LoginService(
                userRepository,
                encoder,
                authenticationManager
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createUser_whenUserDoesNotExist_thenCreateNewUser() throws Exception {
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        // Arrange
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // Act
        loginService.createUser(loginInfo);

        // Assert
        verify(userRepository, times(1))
                .save(argThat(user ->
                user.getUsername().equals(username) && encoder.matches(password, user.getPassword())
        ));
        verify(userRepository, times(1)).findByUsername(username);
        verifyNoMoreInteractions(userRepository);

    }
    @Test
    void createUser_whenUserDoesExist_thenThrowException() {
        // Arrange
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(userDetailCustom));

        // Act & Assert
        assertThatThrownBy( () -> loginService.createUser(loginInfo) )
                .isInstanceOf(UserAlreadyExistException.class);

        // Assert
        verify(userRepository, only()).findByUsername(username);

    }
    @Test
    void login_whenUserNotFound_throwsAndDoesNotAuthenticate() {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> loginService.login(loginInfo, request))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository, only()).findByUsername(username);
        verifyNoInteractions(authenticationManager);

        // Session should not be created
        assertThat(request.getSession(false)).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void login_whenPasswordIsNotCorrect_throwsAndDoesNotAuthenticate() {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(userDetailCustomWrongPassword));

        // Act + Assert
        assertThatThrownBy(() -> loginService.login(loginInfo, request))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository, only()).findByUsername(username);

        verifyNoInteractions(authenticationManager);

        // Session should not be created
        assertThat(request.getSession(false)).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void login_whenUserAndPasswordAreCorrect_Authenticate() throws UserNotFoundException {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();

        lenient().when(userRepository.findByUsername(username)).thenReturn(Optional.of(userDetailCustom));

        Authentication authResult =
                UsernamePasswordAuthenticationToken.authenticated(
                        username, null, List.of()
                );
        lenient().when(authenticationManager.authenticate(argThat(user ->
                user.getName().equals(username))))
                .thenReturn(authResult);

        // Act
        var result = loginService.login(loginInfo, request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(new UserName(username));


        verify(userRepository, only()).findByUsername(username);

        verify(authenticationManager, only()).authenticate(any());

        // Session should be created
        assertThat(request.getSession(false)).isNotNull();
        assertThat(Objects.requireNonNull(request.getSession(false))
                .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY))
                .isEqualTo(SecurityContextHolder.getContext());

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isSameAs(authResult);
    }
}
