package com.al3000.cloudbase.bdd.steps;

import com.al3000.cloudbase.model.UserDetailCustom;
import com.al3000.cloudbase.repository.UserRepository;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;


public class AuthSteps extends BaseStepDefinitions {

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected UserRepository userRepository;

    @Before
    public void resetState() {
        userRepository.deleteAll();
    }
    protected void saveUser(String username, String password) {
        userRepository.findByUsername(username).ifPresent(userRepository::delete);
        userRepository.save(new UserDetailCustom(username, passwordEncoder.encode(password)));
    }


    @Given("the following sign-up payload:")
    public void theFollowingSignUpPayload(DataTable dataTable) {
        context.setRequestPayload(toPayload(dataTable));
    }

    @Given("the following sign-in payload:")
    public void theFollowingSignInPayload(DataTable dataTable) {
        context.setRequestPayload(toPayload(dataTable));
    }

    @Given("a user already exists with username {string}")
    public void aUserAlreadyExistsWithUsername(String username) {
        saveUser(username, "password1-2");
    }

    @Given("an existing user with username {string} and password {string}")
    public void anExistingUserWithUsernameAndPassword(String username, String password) {
        saveUser(username, password);
    }

    @Given("no user exists with username {string}")
    public void noUserExistsWithUsername(String username) {
        userRepository.findByUsername(username).ifPresent(userRepository::delete);
    }


}
