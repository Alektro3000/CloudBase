package com.al3000.cloudbase.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FileFullInfoTest {

    @Test
    void assertPathException() {
        assertThatThrownBy(() -> new FileFullInfo("","asd","",0L, true));
    }
    @Test
    void assertNameException() {
        assertThatThrownBy(() -> new FileFullInfo("","","/",0L, true));
    }
    @Test
    void assertUserException() {
        assertThatThrownBy(() -> new FileFullInfo("/","","",0L, true));
    }
}
