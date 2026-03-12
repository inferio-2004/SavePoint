package com.example.Savepoint.User;

import com.example.Savepoint.Auth.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;


@RestController
public class UserController {
    private final AuthService authService;
    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping(path="/auth/steam")
    public void getAuth(HttpServletResponse resp) throws Exception{
        String finalUrl = authService.getRedirectUrlSteam();
        resp.sendRedirect(finalUrl);
    }

    @GetMapping(path="/auth/steam/callback")
    public ResponseEntity<UserProfileDTO> callback(@RequestParam Map<String,String> params, HttpServletResponse response, HttpServletRequest request) throws Exception{
        return ResponseEntity.ok(authService.handleSteamCallback(params,request,response));
    }

    @PostMapping(path="/auth/manual/login")
    public  ResponseEntity<UserProfileDTO> manualLogin(@Valid @RequestBody UserLoginDTO user, HttpServletResponse response, HttpServletRequest request){
        return ResponseEntity.ok(authService.handleManualLogin(user,request,response));
    }

    @PostMapping(path="/auth/manual/signup")
    public ResponseEntity<UserProfileDTO> manualSignup(@Valid @RequestBody UserRegisterDTO user, HttpServletResponse response, HttpServletRequest request) {
        return ResponseEntity.status(201).body(authService.handleManualRegister(user, request, response));
    }
}
