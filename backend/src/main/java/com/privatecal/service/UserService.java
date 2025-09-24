package com.privatecal.service;

import com.privatecal.dto.UserResponse;
import com.privatecal.dto.UserPreferencesRequest;
import com.privatecal.dto.UserPreferencesResponse;
import com.privatecal.entity.User;
import com.privatecal.repository.TaskRepository;
import com.privatecal.repository.UserRepository;
import com.privatecal.security.UserDetailsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for user management operations
 */
@Service
@Transactional
public class UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private TaskRepository taskRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    /**
     * Get current authenticated user
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl)) {
            throw new RuntimeException("No authenticated user found");
        }
        
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return getUserById(userDetails.getId());
    }
    
    /**
     * Get current authenticated user ID
     */
    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl)) {
            throw new RuntimeException("No authenticated user found");
        }
        
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return userDetails.getId();
    }
    
    /**
     * Get user by ID
     */
    @Transactional(readOnly = true)
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + userId));
    }
    
    /**
     * Get user by username
     */
    @Transactional(readOnly = true)
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    /**
     * Get user by email
     */
    @Transactional(readOnly = true)
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    /**
     * Get user profile as response DTO
     */
    @Transactional(readOnly = true)
    public UserResponse getUserProfile(Long userId) {
        User user = getUserById(userId);
        long taskCount = taskRepository.countByUser_Id(userId);
        return UserResponse.fromUser(user, taskCount);
    }
    
    /**
     * Get current user profile
     */
    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfile() {
        User currentUser = getCurrentUser();
        long taskCount = taskRepository.countByUser_Id(currentUser.getId());
        return UserResponse.fromUser(currentUser, taskCount);
    }
    
    /**
     * Update user profile
     */
    public UserResponse updateUserProfile(Long userId, UserResponse updateRequest) {
        logger.debug("Updating profile for user ID: {}", userId);
        
        User user = getUserById(userId);
        
        // Update fields if provided
        if (updateRequest.getFirstName() != null) {
            user.setFirstName(updateRequest.getFirstName().trim());
        }
        
        if (updateRequest.getLastName() != null) {
            user.setLastName(updateRequest.getLastName().trim());
        }
        
        if (updateRequest.getEmail() != null && !updateRequest.getEmail().trim().isEmpty()) {
            String newEmail = updateRequest.getEmail().trim();
            
            // Check if email is already taken by another user
            Optional<User> existingUser = userRepository.findByEmail(newEmail);
            if (existingUser.isPresent() && !existingUser.get().getId().equals(userId)) {
                throw new RuntimeException("Email is already taken by another user");
            }
            
            user.setEmail(newEmail);
        }
        
        if (updateRequest.getTimezone() != null) {
            user.setTimezone(updateRequest.getTimezone().trim());
        }
        
        User updatedUser = userRepository.save(user);
        long taskCount = taskRepository.countByUser_Id(userId);
        
        logger.info("Profile updated successfully for user: {}", updatedUser.getUsername());
        
        return UserResponse.fromUser(updatedUser, taskCount);
    }
    
    /**
     * Update current user profile
     */
    public UserResponse updateCurrentUserProfile(UserResponse updateRequest) {
        Long currentUserId = getCurrentUserId();
        return updateUserProfile(currentUserId, updateRequest);
    }
    
    /**
     * Change user password
     */
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        logger.debug("Changing password for user ID: {}", userId);
        
        User user = getUserById(userId);
        
        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            logger.warn("Password change failed - incorrect current password for user: {}", user.getUsername());
            throw new RuntimeException("Current password is incorrect");
        }
        
        // Validate new password
        if (newPassword == null || newPassword.length() < 6) {
            throw new RuntimeException("New password must be at least 6 characters long");
        }
        
        // Update password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        logger.info("Password changed successfully for user: {}", user.getUsername());
    }
    
    /**
     * Change current user password
     */
    public void changeCurrentUserPassword(String currentPassword, String newPassword) {
        Long currentUserId = getCurrentUserId();
        changePassword(currentUserId, currentPassword, newPassword);
    }
    
    /**
     * Check if username is available
     */
    @Transactional(readOnly = true)
    public boolean isUsernameAvailable(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        return !userRepository.existsByUsername(username.trim());
    }
    
    /**
     * Check if username is available for update (excluding current user)
     */
    @Transactional(readOnly = true)
    public boolean isUsernameAvailableForUpdate(Long userId, String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        
        Optional<User> existingUser = userRepository.findByUsername(username.trim());
        return existingUser.isEmpty() || existingUser.get().getId().equals(userId);
    }
    
    /**
     * Check if email is available
     */
    @Transactional(readOnly = true)
    public boolean isEmailAvailable(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return !userRepository.existsByEmail(email.trim());
    }
    
    /**
     * Check if email is available for update (excluding current user)
     */
    @Transactional(readOnly = true)
    public boolean isEmailAvailableForUpdate(Long userId, String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        Optional<User> existingUser = userRepository.findByEmail(email.trim());
        return existingUser.isEmpty() || existingUser.get().getId().equals(userId);
    }
    
    /**
     * Get user statistics
     */
    @Transactional(readOnly = true)
    public UserResponse getUserStatistics(Long userId) {
        User user = getUserById(userId);
        long taskCount = taskRepository.countByUser_Id(userId);
        
        UserResponse response = UserResponse.fromUser(user, taskCount);
        return response;
    }
    
    /**
     * Get current user statistics
     */
    @Transactional(readOnly = true)
    public UserResponse getCurrentUserStatistics() {
        Long currentUserId = getCurrentUserId();
        return getUserStatistics(currentUserId);
    }
    
    /**
     * Delete user account (soft delete - marks as inactive)
     */
    public void deleteUserAccount(Long userId) {
        logger.warn("Deleting account for user ID: {}", userId);
        
        User user = getUserById(userId);
        
        // In a production system, you might want to:
        // 1. Soft delete by setting an 'active' flag to false
        // 2. Anonymize user data instead of hard delete
        // 3. Keep data for audit purposes
        
        // For this implementation, we'll do a hard delete
        // Delete all user tasks (cascade will handle reminders)
        taskRepository.deleteByUser(user);
        
        // Delete user
        userRepository.delete(user);
        
        logger.info("Account deleted successfully for user: {}", user.getUsername());
    }
    
    /**
     * Delete current user account
     */
    public void deleteCurrentUserAccount() {
        Long currentUserId = getCurrentUserId();
        deleteUserAccount(currentUserId);
        
        // Clear security context
        SecurityContextHolder.clearContext();
    }
    
    /**
     * Search users (admin functionality - if needed)
     */
    @Transactional(readOnly = true)
    public List<UserResponse> searchUsers(String searchTerm) {
        Optional<User> users = userRepository.findByUsernameOrEmail(searchTerm);
        
        return users.stream()
                .map(user -> {
                    long taskCount = taskRepository.countByUser_Id(user.getId());
                    return UserResponse.fromUser(user, taskCount);
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Get all users (admin functionality - if needed)
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        List<User> users = userRepository.findAll();
        
        return users.stream()
                .map(user -> {
                    long taskCount = taskRepository.countByUser_Id(user.getId());
                    return UserResponse.fromUser(user, taskCount);
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Check if user exists
     */
    @Transactional(readOnly = true)
    public boolean userExists(Long userId) {
        return userRepository.existsById(userId);
    }
    
    /**
     * Validate user ownership (security helper)
     */
    public void validateUserOwnership(Long userId) {
        Long currentUserId = getCurrentUserId();
        if (!currentUserId.equals(userId)) {
            throw new RuntimeException("Access denied: User can only access their own data");
        }
    }

    /**
     * Get user preferences
     */
    @Transactional(readOnly = true)
    public UserPreferencesResponse getUserPreferences(Long userId) {
        logger.debug("Getting preferences for user ID: {}", userId);
        User user = getUserById(userId);
        return UserPreferencesResponse.fromUser(user);
    }

    /**
     * Get current user preferences
     */
    @Transactional(readOnly = true)
    public UserPreferencesResponse getCurrentUserPreferences() {
        User currentUser = getCurrentUser();
        return UserPreferencesResponse.fromUser(currentUser);
    }

    /**
     * Update user preferences
     */
    public UserPreferencesResponse updateUserPreferences(Long userId, UserPreferencesRequest preferencesRequest) {
        logger.debug("Updating preferences for user ID: {}", userId);

        User user = getUserById(userId);

        // Update fields if provided (only non-null values)
        if (preferencesRequest.getTheme() != null) {
            validateTheme(preferencesRequest.getTheme());
            user.setTheme(preferencesRequest.getTheme());
        }

        if (preferencesRequest.getTimezone() != null) {
            user.setTimezone(preferencesRequest.getTimezone().trim());
        }

        if (preferencesRequest.getTimeFormat() != null) {
            validateTimeFormat(preferencesRequest.getTimeFormat());
            user.setTimeFormat(preferencesRequest.getTimeFormat());
        }

        if (preferencesRequest.getCalendarView() != null) {
            validateCalendarView(preferencesRequest.getCalendarView());
            user.setCalendarView(preferencesRequest.getCalendarView());
        }

        if (preferencesRequest.getEmailNotifications() != null) {
            user.setEmailNotifications(preferencesRequest.getEmailNotifications());
        }

        if (preferencesRequest.getReminderNotifications() != null) {
            user.setReminderNotifications(preferencesRequest.getReminderNotifications());
        }

        if (preferencesRequest.getWeekStartDay() != null) {
            validateWeekStartDay(preferencesRequest.getWeekStartDay());
            user.setWeekStartDay(preferencesRequest.getWeekStartDay());
        }

        User updatedUser = userRepository.save(user);

        logger.info("Preferences updated successfully for user: {}", updatedUser.getUsername());

        return UserPreferencesResponse.fromUser(updatedUser);
    }

    /**
     * Update current user preferences
     */
    public UserPreferencesResponse updateCurrentUserPreferences(UserPreferencesRequest preferencesRequest) {
        Long currentUserId = getCurrentUserId();
        return updateUserPreferences(currentUserId, preferencesRequest);
    }

    /**
     * Validate theme value
     */
    private void validateTheme(String theme) {
        if (theme != null && !theme.matches("^(light|dark|system)$")) {
            throw new IllegalArgumentException("Theme must be 'light', 'dark', or 'system'");
        }
    }

    /**
     * Validate time format value
     */
    private void validateTimeFormat(String timeFormat) {
        if (timeFormat != null && !timeFormat.matches("^(12h|24h)$")) {
            throw new IllegalArgumentException("Time format must be '12h' or '24h'");
        }
    }

    /**
     * Validate calendar view value
     */
    private void validateCalendarView(String calendarView) {
        if (calendarView != null && !calendarView.matches("^(month|week|day|agenda)$")) {
            throw new IllegalArgumentException("Calendar view must be 'month', 'week', 'day', or 'agenda'");
        }
    }

    /**
     * Validate week start day value
     */
    private void validateWeekStartDay(Integer weekStartDay) {
        if (weekStartDay != null && weekStartDay != 0 && weekStartDay != 1) {
            throw new IllegalArgumentException("Week start day must be 0 (Sunday) or 1 (Monday)");
        }
    }
}