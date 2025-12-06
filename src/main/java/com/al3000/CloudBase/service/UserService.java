package com.al3000.CloudBase.service;

import com.al3000.CloudBase.dto.LoginInfo;
import com.al3000.CloudBase.exception.UserAlreadyExistException;
import com.al3000.CloudBase.exception.UserNotFoundException;
import com.al3000.CloudBase.model.UserInfo;
import com.al3000.CloudBase.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    UserService(UserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }
    public void createUser(LoginInfo info) {
        var login = repository.findByUsername(info.username);
        if (login.isPresent()){
            throw new UserAlreadyExistException("User already exist");
        }
        String encoded = passwordEncoder.encode(info.password);
        repository.save(new UserInfo(info.username, encoded));
    }
    public LoginInfo login(LoginInfo info) {
        var login = repository.findByUsername(info.username);
        if (login.isEmpty()){
            throw new UserNotFoundException("User not found");
        }
        if(passwordEncoder.matches(info.password, login.get().getPassword())){
            return info;
        }
        throw new UserNotFoundException("User not found");
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return repository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
