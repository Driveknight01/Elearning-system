package com.elearning.security;

import com.elearning.module.user.entity.User;
import com.elearning.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads active (non-deleted) users by email, username, or primary key for Spring Security.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        User user = userRepository.findByEmailAndDeletedAtIsNull(identifier)
                .or(() -> userRepository.findByUsernameAndDeletedAtIsNull(identifier))
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Không tìm thấy tài khoản với định danh: " + identifier));
        return UserPrincipal.from(user);
    }

    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Không tìm thấy người dùng với id: " + userId));
        return UserPrincipal.from(user);
    }
}
