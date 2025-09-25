package com.privatecal.security;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * Extended UserDetails implementation that provides easier access to user ID
 * This is typically used with @AuthenticationPrincipal annotation
 */
public class UserPrincipal extends UserDetailsImpl {

    public UserPrincipal(Long id, String username, String email, String password, String fullName,
                         Collection<? extends GrantedAuthority> authorities) {
        super(id, username, email, password, fullName, authorities);
    }

    public Long getUserId() {
        return getId();
    }
}