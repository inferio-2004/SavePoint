package com.example.Savepoint.User;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserLoginDTO(@NotBlank @Size(max=72) String password,@NotBlank @Email String mail) {}