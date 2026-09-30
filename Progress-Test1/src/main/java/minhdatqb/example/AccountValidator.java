package minhdatqb.example;

import java.time.LocalDate;
import java.time.Period;
import java.util.regex.Pattern;

public class AccountValidator {

    private static final String EMAIL_REGEX =
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile(EMAIL_REGEX);

    // Username:
    // - Bắt đầu bằng chữ cái
    // - Sau đó có thể là chữ, số hoặc _
    // - Độ dài từ 5 đến 20 ký tự
    private static final String USERNAME_REGEX =
            "^[a-zA-Z][a-zA-Z0-9_]{4,19}$";

    private static final Pattern USERNAME_PATTERN =
            Pattern.compile(USERNAME_REGEX);

    // Phone Việt Nam:
    // 03, 05, 07, 08, 09 + 8 chữ số
    private static final String PHONE_REGEX =
            "^(03|05|07|08|09)\\d{8}$";

    private static final Pattern PHONE_PATTERN =
            Pattern.compile(PHONE_REGEX);

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

        // Email phải nhỏ hơn 100 ký tự
        if (email.length() >= 100) {
            return false;
        }

        return EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidPassword(String password, String username) {
        if (password == null) {
            return false;
        }

        int length = password.length();

        // Password từ 8 đến 32 ký tự
        if (length < 8 || length > 32) {
            return false;
        }

        // Không được chứa khoảng trắng
        if (password.matches(".*\\s.*")) {
            return false;
        }

        // Phải có ít nhất một chữ hoa
        if (!password.matches(".*[A-Z].*")) {
            return false;
        }

        // Không được chứa username
        boolean notContainUsername =
                username == null
                        || !password.toLowerCase()
                        .contains(username.toLowerCase());

        return notContainUsername;
    }

    public static int calculateAge(
            LocalDate dateOfBirth,
            LocalDate currentDate) {

        if (dateOfBirth == null || currentDate == null) {
            return 0;
        }

        return Period.between(dateOfBirth, currentDate).getYears();
    }

    public static boolean isValidPhone(String phone) {
        // Phone null được phép
        if (phone == null) {
            return true;
        }

        return PHONE_PATTERN.matcher(phone).matches();
    }
}