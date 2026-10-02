package com.pm.authservice.mapper;

import com.pm.authservice.dto.UserResponse;
import com.pm.authservice.model.User;

public class UserMapper {

  public static UserResponse toResponse(User user) {
    return new UserResponse(user.getId(), user.getEmail(), user.getFirstName(),
        user.getLastName(), user.getRole(), user.getSpecialization(),
        user.isEnabled(), user.getCreatedAt());
  }
}
