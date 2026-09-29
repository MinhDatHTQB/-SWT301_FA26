package minhdatqb.example;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AccountService {
    // Key: username dạng lowercase, Value: Account object
    private final Map<String, Account> accountsByUsername;
    // Key: email dạng lowercase, Value: username (hoặc email) để check trùng
    private final Map<String, String> emailToUsernameMap;

    public AccountService() {
        this.accountsByUsername = new HashMap<>();
        this.emailToUsernameMap = new HashMap<>();
    }

    public ResultCode register(String username, String email, String password,
                               String confirmPassword, LocalDate dateOfBirth, String phone) {
        LocalDate today = LocalDate.now();

        // REG-01: Kiểm tra thông tin bắt buộc / input rỗng / ngày sinh ở tương lai
        if (isBlank(username) || isBlank(email) || isBlank(password) || isBlank(confirmPassword)
                || dateOfBirth == null || dateOfBirth.isAfter(today)) {
            return ResultCode.INVALID_INPUT;
        }

        // REG-02: Kiểm tra định dạng Username
        if (!AccountValidator.isValidUsername(username)) {
            return ResultCode.INVALID_USERNAME;
        }

        // REG-04: Kiểm tra định dạng Email
        if (!AccountValidator.isValidEmail(email)) {
            return ResultCode.INVALID_EMAIL;
        }

        // REG-06: Kiểm tra độ phức tạp Password & không chứa username
        if (!AccountValidator.isValidPassword(password, username)) {
            return ResultCode.WEAK_PASSWORD;
        }

        // REG-07: Kiểm tra Confirm Password khớp với Password
        if (!password.equals(confirmPassword)) {
            return ResultCode.PASSWORD_MISMATCH;
        }

        // REG-08: Kiểm tra độ tuổi (Phải từ 18 tuổi trở lên tính đến today)
        int age = AccountValidator.calculateAge(dateOfBirth, today);
        if (age < 18) {
            return ResultCode.UNDERAGE;
        }

        // REG-09: Kiểm tra Phone (null hoặc "" được chấp nhận, nhưng chuỗi khoảng trắng hoặc sai định dạng là INVALID_PHONE)
        if (phone != null) {
            if (phone.isBlank()) {
                return ResultCode.INVALID_PHONE; // Khoảng trắng (" ") không được chấp nhận
            }
            if (!AccountValidator.isValidPhone(phone)) {
                return ResultCode.INVALID_PHONE;
            }
        }

        // REG-03: Kiểm tra trùng lặp Username (so sánh theo key lowercase)
        String lowerUsername = key(username);
        if (accountsByUsername.containsKey(lowerUsername)) {
            return ResultCode.DUPLICATE_USERNAME;
        }

        // REG-05: Kiểm tra trùng lặp Email (so sánh theo key lowercase)
        String lowerEmail = key(email);
        if (emailToUsernameMap.containsKey(lowerEmail)) {
            return ResultCode.DUPLICATE_EMAIL;
        }

        // REG-10: Đăng ký thành công -> Sinh salt, hash mật khẩu, lưu tài khoản vào Map
        String salt = PasswordHasher.generateSalt();
        String passwordHash = PasswordHasher.hash(salt, password);

        // Chuẩn hóa phone: nếu là null hoặc rỗng thì lưu null
        String validPhone = (phone == null || phone.isEmpty()) ? null : phone;

        Account newAccount = new Account(
                username,
                lowerEmail,
                dateOfBirth,
                validPhone,
                salt,
                passwordHash,
                AccountStatus.ACTIVE
        );

        accountsByUsername.put(lowerUsername, newAccount);
        emailToUsernameMap.put(lowerEmail, lowerUsername);

        return ResultCode.SUCCESS;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String key(String s) {
        return s.toLowerCase(Locale.ROOT);
    }

    // Các phương thức hỗ trợ truy vấn (nếu cần thiết cho các service/test sau này)
    public Account findAccountByUsername(String username) {
        if (username == null) return null;
        return accountsByUsername.get(key(username));
    }
}