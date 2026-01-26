package com.turgo.barangayapp.Service;

import com.turgo.barangayapp.Model.User;
import com.turgo.barangayapp.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserServices {

    @Autowired
    UserRepository userRepository;
    
    @Autowired
    PasswordEncoder passwordEncoder;

    public Optional<User> findByEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        return userRepository.findByEmail(email.trim().toLowerCase());
    }
    
    public boolean existsByEmail(String email) {
        if (email == null) {
            return false;
        }
        return userRepository.existsByEmail(email.trim().toLowerCase());
    }
    
    public User save(User user) {
        return userRepository.save(user);
    }
    
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
    
    public java.util.List<User> findAll() {
        return userRepository.findAll();
    }
    
    public User createUser(String email, String password, String fullName, String address, String phoneNumber, boolean isAdmin, String securityQuestion, String securityAnswer) {
        User user = new User();
        user.setEmail(email == null ? null : email.trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(password));
        user.setFullName(fullName);
        user.setAddress(address);
        user.setPhoneNumber(phoneNumber);
        user.setAdmin(isAdmin);
        user.setSecurityQuestion(securityQuestion);
        // Hash the security answer for security
        user.setSecurityAnswer(passwordEncoder.encode(securityAnswer.trim().toLowerCase()));
        return userRepository.save(user);
    }
    
    public User createUser(String email, String password, String fullName, String address, String phoneNumber) {
        return createUser(email, password, fullName, address, phoneNumber, false, null, null);
    }
    
    public boolean validateSecurityAnswer(String rawAnswer, String encodedAnswer) {
        return passwordEncoder.matches(rawAnswer.trim().toLowerCase(), encodedAnswer);
    }
    
    public void updatePassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
    
    public boolean validatePassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}