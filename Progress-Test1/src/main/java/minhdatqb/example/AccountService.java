package minhdatqb.example;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Nghiệp vụ quản lý tài khoản, lưu trong bộ nhớ (HashMap), không phụ thuộc thời gian thực trừ ngày hiện tại khi đăng ký. */
public class AccountService {

    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final int PASSWORD_HISTORY_SIZE = 3;
    public static final int MIN_AGE = 18;

    private final Map<String, Account> accountsByUsername = new HashMap<>(); // key: username lowercase
    private final Map<String, String> usernameByEmail = new HashMap<>();     // email lowercase -> username key
    private final Map<String, String> usernameByToken = new HashMap<>();     // token -> username key
    private final Map<String, String> tokenByUsername = new HashMap<>();     // username key -> token hiện hành

    // ================= Đăng ký =================
    public ResultCode register(String username, String email, String password,
                               String confirmPassword, LocalDate dateOfBirth, String phone) {
        LocalDate today = LocalDate.now();
        // BR-REG-01
        if (isBlank(username) || isBlank(email) || isBlank(password) || isBlank(confirmPassword)
                || dateOfBirth == null || dateOfBirth.isAfter(today)) {
            return ResultCode.INVALID_INPUT;
        }
        // BR-REG-02
        if (!AccountValidator.isValidUsername(username)) {
            return ResultCode.INVALID_USERNAME;
        }
        // BR-REG-04
        if (!AccountValidator.isValidEmail(email)) {
            return ResultCode.INVALID_EMAIL;
        }
        // BR-REG-06
        if (!AccountValidator.isValidPassword(password, username)) {
            return ResultCode.WEAK_PASSWORD;
        }
        // BR-REG-07
        if (!password.equals(confirmPassword)) {
            return ResultCode.PASSWORD_MISMATCH;
        }
        // BR-REG-08
        if (AccountValidator.calculateAge(dateOfBirth, today) < MIN_AGE) {
            return ResultCode.UNDERAGE;
        }
        // BR-REG-09 (phone tùy chọn: null hoặc "" được chấp nhận)
        if (phone != null && !phone.isEmpty() && !AccountValidator.isValidPhone(phone)) {
            return ResultCode.INVALID_PHONE;
        }
        String userKey = key(username);
        String emailKey = key(email);
        // BR-REG-03
        if (accountsByUsername.containsKey(userKey)) {
            return ResultCode.DUPLICATE_USERNAME;
        }
        // BR-REG-05
        if (usernameByEmail.containsKey(emailKey)) {
            return ResultCode.DUPLICATE_EMAIL;
        }
        // BR-REG-10
        String salt = PasswordHasher.generateSalt();
        Account account = new Account(username, emailKey, dateOfBirth, phone, salt, PasswordHasher.hash(salt, password));
        accountsByUsername.put(userKey, account);
        usernameByEmail.put(emailKey, userKey);
        return ResultCode.SUCCESS;
    }

    // ================= Đăng nhập =================
    public ResultCode login(String username, String password) {
        // BR-LOG-01
        if (isBlank(username) || isBlank(password)) {
            return ResultCode.INVALID_INPUT;
        }
        // BR-LOG-02, 03 (user không tồn tại)
        Account account = accountsByUsername.get(key(username));
        if (account == null) {
            return ResultCode.INVALID_CREDENTIALS;
        }
        // BR-LOG-04
        if (account.getStatus() == AccountStatus.DISABLED) {
            return ResultCode.ACCOUNT_DISABLED;
        }
        // BR-LOG-06: đang khóa -> từ chối, không tăng bộ đếm
        if (account.isLocked()) {
            return ResultCode.ACCOUNT_LOCKED;
        }
        // BR-LOG-03, 05: sai mật khẩu
        if (!PasswordHasher.matches(account.getSalt(), password, account.getCurrentPasswordHash())) {
            account.incrementFailedAttempts();
            if (account.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
                account.lock();
                return ResultCode.ACCOUNT_LOCKED;
            }
            return ResultCode.INVALID_CREDENTIALS;
        }
        // BR-LOG-08
        account.resetFailedAttempts();
        return ResultCode.SUCCESS;
    }

    // ================= Quản trị & truy vấn =================
    public ResultCode disableAccount(String username) {
        Optional<Account> account = findByUsername(username);
        if (account.isEmpty()) {
            return ResultCode.USER_NOT_FOUND;
        }
        account.get().setStatus(AccountStatus.DISABLED);
        return ResultCode.SUCCESS;
    }

    /** BR-ADM-03: quản trị viên mở khóa tài khoản bị khóa do đăng nhập sai. */
    public ResultCode unlockAccount(String username) {
        Optional<Account> account = findByUsername(username);
        if (account.isEmpty()) {
            return ResultCode.USER_NOT_FOUND;
        }
        account.get().unlock();
        return ResultCode.SUCCESS;
    }

    public Optional<Account> findByUsername(String username) {
        if (isBlank(username)) {
            return Optional.empty();
        }
        return Optional.ofNullable(accountsByUsername.get(key(username)));
    }

    public boolean isLocked(String username) {
        return findByUsername(username).map(Account::isLocked).orElse(false);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String key(String s) {
        return s.toLowerCase(Locale.ROOT);
    }
}
