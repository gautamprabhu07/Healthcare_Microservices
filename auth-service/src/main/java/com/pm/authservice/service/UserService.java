package com.pm.authservice.service;

import com.pm.authservice.dto.ChangePasswordRequest;
import com.pm.authservice.dto.CreateUserRequest;
import com.pm.authservice.dto.PageResponse;
import com.pm.authservice.dto.UpdateUserRequest;
import com.pm.authservice.dto.UserResponse;
import com.pm.authservice.exception.ApiValidationException;
import com.pm.authservice.exception.BusinessRuleException;
import com.pm.authservice.exception.EmailAlreadyExistsException;
import com.pm.authservice.exception.UserNotFoundException;
import com.pm.authservice.mapper.UserMapper;
import com.pm.authservice.model.User;
import com.pm.authservice.repository.UserRepository;
import com.pm.authservice.security.CurrentUser;
import com.pm.authservice.security.Role;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

  private static final int MAX_PAGE_SIZE = 100;

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public UserService(UserRepository userRepository,
      PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  public Optional<User> findByEmail(String email) {
    return userRepository.findByEmail(normalizeEmail(email));
  }

  public Optional<User> findById(UUID id) {
    return userRepository.findById(id);
  }

  public UserResponse getById(UUID id) {
    return UserMapper.toResponse(getEntity(id));
  }

  public PageResponse<UserResponse> list(Role role, int page, int size) {
    PageRequest pageable = PageRequest.of(Math.max(page, 0),
        Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
        Sort.by("lastName", "firstName"));

    Page<User> users = role == null
        ? userRepository.findAll(pageable)
        : userRepository.findByRole(role.name(), pageable);

    return PageResponse.of(users.map(UserMapper::toResponse));
  }

  public List<UserResponse> listDoctors() {
    return userRepository
        .findByRoleAndEnabledTrueOrderByLastNameAscFirstNameAsc(
            Role.DOCTOR.name())
        .stream().map(UserMapper::toResponse).toList();
  }

  @Transactional
  public UserResponse create(CreateUserRequest request) {
    String email = normalizeEmail(request.email());
    if (userRepository.existsByEmail(email)) {
      throw new EmailAlreadyExistsException(
          "A user with this email already exists: " + email);
    }

    User user = new User();
    user.setEmail(email);
    user.setPassword(passwordEncoder.encode(request.password()));
    applyProfile(user, request.firstName(), request.lastName(), request.role(),
        request.specialization());
    return UserMapper.toResponse(userRepository.save(user));
  }

  @Transactional
  public UserResponse update(UUID id, UpdateUserRequest request,
      CurrentUser actor) {
    User user = getEntity(id);

    if (actor.id().equals(id) && !user.getRole().equals(request.role().name())) {
      throw new BusinessRuleException("You cannot change your own role");
    }

    applyProfile(user, request.firstName(), request.lastName(), request.role(),
        request.specialization());
    return UserMapper.toResponse(userRepository.save(user));
  }

  @Transactional
  public UserResponse setEnabled(UUID id, boolean enabled, CurrentUser actor) {
    User user = getEntity(id);

    if (actor.id().equals(id) && !enabled) {
      throw new BusinessRuleException("You cannot disable your own account");
    }

    user.setEnabled(enabled);
    return UserMapper.toResponse(userRepository.save(user));
  }

  @Transactional
  public void changePassword(UUID id, ChangePasswordRequest request) {
    User user = getEntity(id);

    if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
      throw new ApiValidationException("currentPassword",
          "Current password is incorrect");
    }
    if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
      throw new ApiValidationException("newPassword",
          "New password must be different from the current password");
    }

    user.setPassword(passwordEncoder.encode(request.newPassword()));
    userRepository.save(user);
  }

  private User getEntity(UUID id) {
    return userRepository.findById(id).orElseThrow(
        () -> new UserNotFoundException("User not found with ID: " + id));
  }

  private void applyProfile(User user, String firstName, String lastName,
      Role role, String specialization) {
    boolean hasSpecialization = specialization != null
        && !specialization.isBlank();
    if (role == Role.DOCTOR && !hasSpecialization) {
      throw new ApiValidationException("specialization",
          "Specialization is required for doctors");
    }

    user.setFirstName(firstName.trim());
    user.setLastName(lastName.trim());
    user.setRole(role.name());
    user.setSpecialization(
        role == Role.DOCTOR ? specialization.trim() : null);
  }

  private static String normalizeEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }
}
