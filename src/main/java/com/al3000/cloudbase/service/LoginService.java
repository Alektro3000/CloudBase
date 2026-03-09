package com.al3000.cloudbase.service;

import com.al3000.cloudbase.dto.LoginInfo;
import com.al3000.cloudbase.dto.UserName;
import com.al3000.cloudbase.exception.UserAlreadyExistException;
import com.al3000.cloudbase.exception.UserNotFoundException;
import com.al3000.cloudbase.model.UserDetailCustom;
import com.al3000.cloudbase.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class LoginService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public void createUser(LoginInfo info) throws UserAlreadyExistException {
        var login = repository.findByUsername(info.username());
        if (login.isPresent()){
            throw new UserAlreadyExistException("User already exist");
        }
        String encoded = passwordEncoder.encode(info.password());
        repository.save(new UserDetailCustom(info.username(), encoded));
    }

    public UserName login(LoginInfo info, HttpServletRequest request, HttpServletResponse response) throws UserNotFoundException {
        var logged = findLogin(info);
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        logged.username(),
                        logged.password()
                )
        );

        // Put Authentication into SecurityContext
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        // And save it
        HttpSessionSecurityContextRepository repository =
                new HttpSessionSecurityContextRepository();
        repository.saveContext(context, request, response);

        return new UserName(logged.username());
    }

    private LoginInfo findLogin(LoginInfo info) throws UserNotFoundException {
        var login = repository.findByUsername(info.username());
        if (login.isEmpty()){
            throw new UserNotFoundException("User not found");
        }
        if(passwordEncoder.matches(info.password(), login.get().getPassword())){
            return info;
        }
        throw new UserNotFoundException("User not found");
    }

}
