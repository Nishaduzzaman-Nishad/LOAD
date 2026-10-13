package com.example.load.models;

import java.util.regex.Pattern;

public class ValidationUtils {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^01[3-9][0-9]{8}$");

    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) return false;
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) return false;
        return PHONE_PATTERN.matcher(phone.trim()).matches();
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }

    public static boolean isValidWeight(double weight) {
        // Weight must be > 0 and <= 50 kg
        return weight > 0 && weight <= 50;
    }

    public static String getWeightError(double weight) {
        if (weight <= 0) return "Weight must be greater than 0";
        if (weight > 50) return "Weight cannot exceed 50 kg";
        return null;
    }

    public static String getPasswordError(String password) {
        if (password == null || password.isEmpty()) return "Password is required";
        if (password.length() < 6) return "Password must be at least 6 characters";
        return null;
    }

    public static String getEmailError(String email) {
        if (email == null || email.trim().isEmpty()) return "Email is required";
        if (!isValidEmail(email)) return "Please enter a valid email";
        return null;
    }

    public static String getPhoneError(String phone) {
        if (phone == null || phone.trim().isEmpty()) return "Phone is required";
        if (!isValidPhone(phone)) return "Enter a valid 11-digit phone (e.g., 017XXXXXXXX)";
        return null;
    }
}