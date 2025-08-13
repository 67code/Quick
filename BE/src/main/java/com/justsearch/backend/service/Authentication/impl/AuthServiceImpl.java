package com.justsearch.backend.service.Authentication.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.justsearch.backend.dto.SignInDto;
import com.justsearch.backend.dto.SignupRequestDto;
import com.justsearch.backend.dto.TokenResponseDto;
import com.justsearch.backend.model.RefreshToken;
import com.justsearch.backend.model.Role;
import com.justsearch.backend.model.User;
import com.justsearch.backend.repository.RefreshTokenRepository;
import com.justsearch.backend.repository.UserRepository;
import com.justsearch.backend.security.JwtUtils;
import com.justsearch.backend.service.Authentication.AuthService;

@Service
public class AuthServiceImpl implements AuthService {
    @Autowired
    private UserRepository _userRepository;

    private RefreshTokenRepository _refreshTokenRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtUtils jwtUtils;
    private RoleServiceImpl _roleService;
    @Autowired
    private AuthenticationManager authenticationManager;

    public AuthServiceImpl(PasswordEncoder passwordEncoder, JwtUtils jwtUtils,
            RefreshTokenRepository refreshTokenRepository, RoleServiceImpl roleService,
            AuthenticationManager authenticationManager) {
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this._refreshTokenRepository = refreshTokenRepository;
        _roleService = roleService;
        this.authenticationManager = authenticationManager;

    }

    public void userSignUp(SignupRequestDto signUpRequest) {
        if (_userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new RuntimeException("Email already taken");
        }

        User user = new User();
        user.setEmail(signUpRequest.getEmail());
        user.setName(signUpRequest.getName());
        user.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
        user.setPhone(signUpRequest.getPhone());
        Set<Role> roleSet = new HashSet<>();
        Role role = _roleService.findByName("USER");
        roleSet.add(role);
        if (user.getEmail().split("@")[1].equals("admin.edu")) {
            Role adminRole = _roleService.findByName("ADMIN");
            roleSet.add(adminRole);
        }
        user.setRoles(roleSet);

        _userRepository.save(user);
    }

    public ResponseEntity<?> userSignIn(SignInDto request) {
        Optional<User> userOptional = _userRepository.findByEmail(request.getEmail());
        if (userOptional.isEmpty()) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "User not found");
            errorResponse.put("error", "INVALID_EMAIL");
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(errorResponse);
        }
        User user = userOptional.get();
        try {
            final String token = getAccessToken(user, request.getPassword());
            RefreshToken refreshToken = jwtUtils.generateRefreshToken(user.getId());
            _refreshTokenRepository.save(refreshToken);

            // Determine the role
            String role = user.getRoles().stream()
                    .anyMatch(r -> r.getName().equalsIgnoreCase("ADMIN")) ? "ADMIN" : "USER";

            TokenResponseDto tokenResponse = new TokenResponseDto(
                    user.getName(),
                    token,
                    refreshToken.getToken(),
                    user.getId(),
                    role // pass the role string here
            );

            return ResponseEntity.ok(tokenResponse);
        } catch (Exception ex) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Invalid credentials");
            errorResponse.put("error", "BAD_CREDENTIALS");
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(errorResponse);
        }
    }

    @Transactional
    public ResponseEntity<TokenResponseDto> refresh(String refreshToken) {
        if (jwtUtils.validateRefreshToken(refreshToken)) {
            long userId = jwtUtils.getUserIdFromRefreshToken(refreshToken);
            User user = _userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            _refreshTokenRepository.deleteByToken(refreshToken);
            UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                    user.getEmail(),
                    user.getPassword(),
                    user.getRoles().stream()
                            .map(role -> new SimpleGrantedAuthority(role.getName()))
                            .toList());
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            String newAccessToken = jwtUtils.generateToken(auth);
            RefreshToken newRefreshToken = jwtUtils.generateRefreshToken(user.getId());
            String role = user.getRoles().stream()
                    .anyMatch(r -> r.getName().equalsIgnoreCase("ADMIN")) ? "ADMIN" : "USER";

            TokenResponseDto tokenResponse = new TokenResponseDto(
                    user.getName(),
                    newAccessToken,
                    newRefreshToken.getToken(),
                    user.getId(),
                    role // pass the role string here
            );

            _refreshTokenRepository.save(newRefreshToken);
            return ResponseEntity.ok(tokenResponse);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }

    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new RuntimeException("Refresh token is required");
        }
        RefreshToken refreshTokenEntity = _refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));
        refreshTokenEntity.setRevoked(true);
        _refreshTokenRepository.save(refreshTokenEntity);
    }

    private String getAccessToken(User user, String rawPassword) {
        final Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getEmail(), rawPassword));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        final String token = jwtUtils.generateToken(authentication);
        return token;
    }

}