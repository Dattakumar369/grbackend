package com.example.security;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.entity.DeliveryPerson;
import com.example.entity.User;

public class CustomUserDetails implements UserDetails {

	private static final long serialVersionUID = 1L;
	 private String email;
	    private String password;
	    private List<GrantedAuthority> authorities;

	    public CustomUserDetails(User user) {
	        this.email = user.getEmail();
	        this.password = user.getPassword();
	        this.authorities = Arrays.asList(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
	    }

	    public CustomUserDetails(DeliveryPerson deliveryPerson) {
	        this.email = deliveryPerson.getEmail();
	        this.password = deliveryPerson.getPassword();
	        this.authorities = Arrays.asList(new SimpleGrantedAuthority("ROLE_" + deliveryPerson.getRole()));
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
		return true;
	}
}
