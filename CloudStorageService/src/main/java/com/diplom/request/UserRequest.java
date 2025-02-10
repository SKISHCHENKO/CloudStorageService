package com.diplom.request;

import lombok.Data;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;


@Data
public class UserRequest {

    @Size(max = 255)
    @NotNull
    private String username;

    @Size(max = 255)
    @NotNull
    private String password;

    @Size(max = 50)
    @NotNull
    private String email;

    @NotNull
    @Pattern(regexp = "ROLE_USER|ROLE_ADMIN", message = "Роль должна быть ROLE_USER или ROLE_ADMIN")
    private String role;
}