package com.guthi.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.guthi.enums.Role;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Document(collection = "users")
@Getter  // ENCAPSULATION: Lombok generates controlled accessor methods instead of direct field access
@Setter  // ENCAPSULATION: Lombok generates controlled mutator methods for data consistency
@NoArgsConstructor
@AllArgsConstructor
@Builder
// INHERITANCE: User is-a UserDetails for Spring Security integration (UserDetails contract)
public class User implements UserDetails {

    @Id private String id;

    private String fullName;

    @Indexed(unique = true)
    private String email;

    // ENCAPSULATION: @JsonIgnore prevents sensitive password from leaking in API responses
    @JsonIgnore private String password;

    private String mobileNumber;

    private Role role;

    private Instant createdAt;

    // INHERITANCE: @Override UserDetails contract method to provide role-based authorities for Spring Security
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    // INHERITANCE: @Override UserDetails contract method returning email as username for authentication
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
        return true;
    }
}
