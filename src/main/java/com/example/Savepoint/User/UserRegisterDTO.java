package com.example.Savepoint.User;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRegisterDTO(@NotBlank String username, @NotBlank @Size(max=72) String password, @NotBlank @Email String mail) {
    public UserRegisterDTO withPassword(String password) {
        return new UserRegisterDTO(this.username, password, this.mail);
    }
}