package com.diplom.request;

import lombok.Data;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Max;


@Data
public class UserRequest {

    @Max(255)
    @NotNull
    private String username;

    @Max(255)
    @NotNull
    private String password;

    @Max(50)
    @NotNull
    private String email;

}