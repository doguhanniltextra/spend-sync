package com.enterprise.spendsync.shared.security;

import com.enterprise.spendsync.core.internal.domain.RoleType;
import com.enterprise.spendsync.core.internal.domain.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final UUID tenantId;
    private final UUID vendorId;
    private final String userType; // "USER" or "VENDOR"
    private final String email;
    private final String password;
    private final String fullName;
    private final boolean active;
    private final Set<RoleType> roles;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(UUID id,
                         UUID tenantId,
                         String email,
                         String password,
                         String fullName,
                         boolean active,
                         Set<RoleType> roles,
                         Collection<? extends GrantedAuthority> authorities) {
        this(id, tenantId, null, "USER", email, password, fullName, active, roles, authorities);
    }

    public UserPrincipal(UUID id,
                         UUID tenantId,
                         UUID vendorId,
                         String userType,
                         String email,
                         String password,
                         String fullName,
                         boolean active,
                         Set<RoleType> roles,
                         Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.tenantId = tenantId;
        this.vendorId = vendorId;
        this.userType = userType != null ? userType : "USER";
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.active = active;
        this.roles = roles != null ? Set.copyOf(roles) : Collections.emptySet();
        this.authorities = authorities != null ? Set.copyOf(authorities) : Collections.emptySet();
    }

    public static UserPrincipal create(User user, Collection<? extends GrantedAuthority> authorities) {
        UUID tenantId = user.getTenant() != null ? user.getTenant().getId() : null;
        String fullName = user.getFirstName() + " " + user.getLastName();

        return new UserPrincipal(
                user.getId(),
                tenantId,
                user.getEmail(),
                user.getPasswordHash(),
                fullName,
                user.isActive(),
                user.getRoles(),
                authorities
        );
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getVendorId() {
        return vendorId;
    }

    public String getUserType() {
        return userType;
    }

    public boolean isVendor() {
        return "VENDOR".equalsIgnoreCase(userType);
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public Set<RoleType> getRoles() {
        return roles;
    }

    public Set<String> getRoleNames() {
        return roles.stream().map(RoleType::name).collect(Collectors.toSet());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
