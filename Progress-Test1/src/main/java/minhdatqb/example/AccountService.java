package minhdatqb.example;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class AccountService {
    // Key: username dạng lowercase, Value: Account object
    private final Map<String, Account> accountsByUsername;
    // Key: email dạng lowercase, Value: username để check trùng email
    private final Map<String, String> emailToUsernameMap;

    private static final int MAX_FAILED_ATTEMPTS = 5;

    public AccountService() {
        this.accountsByUsername = new HashMap<>();
        this.emailToUsernameMap = new HashMap<>();
    }

    /**
     * Đăng ký tài khoản mới theo đúng thứ tự quy tắc BR-REG-01 đến 10
     */
    public ResultCode register(String username, String email, String password,
                               String confirmPassword, LocalDate dateOfBirth, String phone) {
        LocalDate today = LocalDate.now();

        // REG-01: Kiểm tra input cơ bản & ngày sinh không ở tương lai
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

        // REG-06: Kiểm tra mật khẩu mạnh & không chứa username
        if (!AccountValidator.isValidPassword(password, username)) {
            return ResultCode.WEAK_PASSWORD;
        }

        // REG-07: Xác nhận mật khẩu khớp
        if (!password.equals(confirmPassword)) {
            return ResultCode.PASSWORD_MISMATCH;
        }

        // REG-08: Kiểm tra độ tuổi (>= 18)
        int age = AccountValidator.calculateAge(dateOfBirth, today);
        if (age < 18) {
            return ResultCode.UNDERAGE;
        }

        // REG-09: Kiểm tra Phone (null hoặc rỗng được chấp nhận, khoảng trắng hoặc sai định dạng là lỗi)
        if (phone != null) {
            if (phone.isBlank()) {
                return ResultCode.INVALID_PHONE;
            }
            if (!AccountValidator.isValidPhone(phone)) {
                return ResultCode.INVALID_PHONE;
            }
        }

        // REG-03: Kiểm tra trùng Username (không phân biệt hoa thường)
        String lowerUsername = key(username);
        if (accountsByUsername.containsKey(lowerUsername)) {
            return ResultCode.DUPLICATE_USERNAME;
        }

        // REG-05: Kiểm tra trùng Email (không phân biệt hoa thường)
        String lowerEmail = key(email);
        if (emailToUsernameMap.containsKey(lowerEmail)) {
            return ResultCode.DUPLICATE_EMAIL;
        }

        // REG-10: Đăng ký thành công -> Tạo salt, hash mật khẩu, lưu tài khoản
        String salt = PasswordHasher.generateSalt();
        String passwordHash = PasswordHasher.hash(salt, password);
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

    /**
     * Xử lý đăng nhập tài khoản theo quy tắc BR-LOG
     */
    public ResultCode login(String username, String password) {
        if (isBlank(username) || isBlank(password)) {
            return ResultCode.INVALID_INPUT;
        }

        Account acc = accountsByUsername.get(key(username));
        if (acc == null) {
            return ResultCode.INVALID_CREDENTIALS; // Không tiết lộ tài khoản có tồn tại hay không
        }

        if (acc.getStatus() == AccountStatus.DISABLED) {
            return ResultCode.ACCOUNT_DISABLED;
        }

        if (acc.isLocked()) {
            return ResultCode.ACCOUNT_LOCKED;
        }

        if (!PasswordHasher.matches(acc.getSalt(), password, acc.getCurrentPasswordHash())) {
            acc.incrementFailedAttempts();
            if (acc.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
                acc.lock();
                return ResultCode.ACCOUNT_LOCKED;
            }
            return ResultCode.INVALID_CREDENTIALS;
        }

        // Đăng nhập thành công -> Reset bộ đếm số lần sai
        acc.resetFailedAttempts();
        return ResultCode.SUCCESS;
    }

    /**
     * Vô hiệu hóa tài khoản (Admin action)
     */
    public ResultCode disableAccount(String username) {
        if (isBlank(username)) return ResultCode.USER_NOT_FOUND;
        Account acc = accountsByUsername.get(key(username));
        if (acc == null) return ResultCode.USER_NOT_FOUND;

        acc.setStatus(AccountStatus.DISABLED);
        return ResultCode.SUCCESS;
    }

    /**
     * Mở khóa tài khoản (Admin action - BR-ADM-03)
     */
    public ResultCode unlockAccount(String username) {
        if (isBlank(username)) return ResultCode.USER_NOT_FOUND;
        Optional<Account> acc = findByUsername(username);
        if (acc.isEmpty()) return ResultCode.USER_NOT_FOUND;

        acc.get().unlock(); // Đặt lại trạng thái khóa và reset failedAttempts
        return ResultCode.SUCCESS;
    }

    /**
     * Tìm kiếm tài khoản dựa vào username
     */
    public Optional<Account> findByUsername(String username) {
        if (isBlank(username)) return Optional.empty();
        return Optional.ofNullable(accountsByUsername.get(key(username)));
    }

    /**
     * Kiểm tra xem tài khoản có đang bị khóa hay không
     */
    public boolean isLocked(String username) {
        Optional<Account> acc = findByUsername(username);
        return acc.map(Account::isLocked).orElse(false);
    }

    // Các hàm tiện ích phụ trợ
    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String key(String s) {
        return s.toLowerCase(Locale.ROOT);
    }
}