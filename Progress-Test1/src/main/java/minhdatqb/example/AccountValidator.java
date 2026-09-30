package minhdatqb.example;

import java.time.LocalDate;
import java.time.Period;
import java.util.regex.Pattern;

public class AccountValidator {

    private static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

    private static final String USERNAME_REGEX = "^[a-zA-Z0-9_]{4,20}$";
    private static final Pattern USERNAME_PATTERN = Pattern.compile(USERNAME_REGEX);

    private static final String PHONE_REGEX = "^\\d{10,11}$";
    private static final Pattern PHONE_PATTERN = Pattern.compile(PHONE_REGEX);

    public static boolean isValidUsername(String username) {
        if (username == null) {
            return false;
        }
        return USERNAME_PATTERN.matcher(username).matches();
    }

    public static boolean isValidEmail(String email) {
        if (email == null) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidPassword(String password, String username) {
        if (password == null) {
            return false;
        }
        int length = password.length();
        boolean isValidLength = (length >= 7 && length <= 20);
        boolean notContainUsername = (username == null || !password.toLowerCase().contains(username.toLowerCase()));

        return isValidLength && notContainUsername;
    }

    public static int calculateAge(LocalDate dateOfBirth, LocalDate currentDate) {
        if (dateOfBirth == null || currentDate == null) {
            return 0;
        }
        return Period.between(dateOfBirth, currentDate).getYears();
    }

    public static boolean isValidPhone(String phone) {
        if (phone == null) {
            return true;
        }
        return PHONE_PATTERN.matcher(phone).matches();
    }
}