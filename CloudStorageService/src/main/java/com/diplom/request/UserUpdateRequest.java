package com.diplom.request;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;

@Data
public class UserUpdateRequest {
    @Max(255)
    @NotNull
    private String username;

    @Max(255)
    @NotNull
    private String password;

}