package minhdatqb;

import minhdatqb.example.Account;
import minhdatqb.example.AccountService;
import minhdatqb.example.AccountStatus;
import minhdatqb.example.ResultCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class AccountServiceTest {

    static final String USER = "alice_01";
    static final String EMAIL = "alice@example.com";
    static final String PASS = "Secret@123";
    static final String WRONG = "Wrong@123";
    static final LocalDate DOB = LocalDate.of(2000, 1, 15);
    static final String PHONE = "0912345678";
    static final LocalDate CHILD_DOB = LocalDate.now().minusYears(10);

    AccountService service;

    @BeforeEach
    void setUp() {
        service = new AccountService();
    }

    /** Arrange dùng chung: đăng ký tài khoản mẫu thành công. */
    void registerDefault() {
        assertEquals(ResultCode.SUCCESS, service.register(USER, EMAIL, PASS, PASS, DOB, PHONE));
    }

    Account account() {
        return service.findByUsername(USER).orElseThrow();
    }

    void failLogin(int times) {
        for (int i = 0; i < times; i++) {
            service.login(USER, WRONG);
        }
    }

    @Nested
    @DisplayName("Module Đăng ký (Register)")
    class Register {

        @Test
        @DisplayName("Đăng ký thành công - Kiểm tra toàn bộ trạng thái tài khoản")
        void register_Success_VerifyState() {
            ResultCode result = service.register(USER, EMAIL, PASS, PASS, DOB, PHONE);

            assertEquals(ResultCode.SUCCESS, result);

            Optional<Account> accOpt = service.findByUsername(USER);
            assertTrue(accOpt.isPresent());

            Account acc = accOpt.get();
            assertEquals(AccountStatus.ACTIVE, acc.getStatus());
            assertEquals(0, acc.getFailedAttempts());
            assertFalse(acc.isLocked());
            assertNotEquals(PASS, acc.getCurrentPasswordHash()); // Hash không trùng mật khẩu rõ
            assertEquals(EMAIL.toLowerCase(), acc.getEmail());   // Email được lưu chữ thường
        }

        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("minhdatqb.AccountServiceTest#invalidRegisterInputs")
        void register_InvalidInputs_ReturnsExpectedCode(String desc, String u, String e, String p, String c,
                                                        LocalDate dob, String phone, ResultCode expected) {
            assertEquals(expected, service.register(u, e, p, c, dob, phone));
            assertTrue(service.findByUsername(u).isEmpty()); // Đảm bảo không tạo tài khoản
        }

        @ParameterizedTest(name = "[{index}] Null/Empty Username -> INVALID_INPUT")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void register_NullAndEmptyUsername(String username) {
            assertEquals(ResultCode.INVALID_INPUT, service.register(username, EMAIL, PASS, PASS, DOB, PHONE));
        }

        @ParameterizedTest(name = "[{index}] Null/Empty Email -> INVALID_INPUT")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void register_NullAndEmptyEmail(String email) {
            assertEquals(ResultCode.INVALID_INPUT, service.register(USER, email, PASS, PASS, DOB, PHONE));
        }

        @ParameterizedTest(name = "[{index}] Null/Empty Password -> INVALID_INPUT")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void register_NullAndEmptyPassword(String pass) {
            assertEquals(ResultCode.INVALID_INPUT, service.register(USER, EMAIL, pass, PASS, DOB, PHONE));
        }

        @ParameterizedTest(name = "[{index}] Trùng username: {0}")
        @ValueSource(strings = {"alice_01", "ALICE_01", "Alice_01"})
        void register_DuplicateUsername_CaseInsensitive(String duplicateUser) {
            service.register(USER, EMAIL, PASS, PASS, DOB, PHONE);
            assertEquals(ResultCode.DUPLICATE_USERNAME, service.register(duplicateUser, "other@example.com", PASS, PASS, DOB, PHONE));
        }

        @ParameterizedTest(name = "[{index}] Trùng email: {0}")
        @ValueSource(strings = {"alice@example.com", "ALICE@EXAMPLE.COM", "Alice@Example.Com"})
        void register_DuplicateEmail_CaseInsensitive(String duplicateEmail) {
            service.register(USER, EMAIL, PASS, PASS, DOB, PHONE);
            assertEquals(ResultCode.DUPLICATE_EMAIL, service.register("bob_0123", duplicateEmail, PASS, PASS, DOB, PHONE));
        }

        @ParameterizedTest(name = "[{index}] today - {0} năm + {1} ngày -> {2}")
        @CsvSource({
                "18, 0, SUCCESS",
                "18, 1, UNDERAGE",
                "0, 1, INVALID_INPUT"
        })
        void register_AgeBoundary(int yearsAgo, int plusDays, ResultCode expected) {
            LocalDate dob = LocalDate.now().minusYears(yearsAgo).plusDays(plusDays);
            assertEquals(expected, service.register("user_test", "test@domain.com", PASS, PASS, dob, null));
        }
    }

    static Stream<Arguments> invalidRegisterInputs() {
        return Stream.of(
                // Mỗi BR-REG một dòng
                Arguments.of("REG-01: Ngày sinh ở tương lai", USER, EMAIL, PASS, PASS, LocalDate.now().plusDays(1), PHONE, ResultCode.INVALID_INPUT),
                Arguments.of("REG-02: Username không hợp lệ", "1alice", EMAIL, PASS, PASS, DOB, PHONE, ResultCode.INVALID_USERNAME),
                Arguments.of("REG-04: Email không hợp lệ", USER, "bad_email", PASS, PASS, DOB, PHONE, ResultCode.INVALID_EMAIL),
                Arguments.of("REG-06: Mật khẩu yếu", USER, EMAIL, "weak", "weak", DOB, PHONE, ResultCode.WEAK_PASSWORD),
                Arguments.of("REG-07: Confirm password không khớp", USER, EMAIL, PASS, "Different@123", DOB, PHONE, ResultCode.PASSWORD_MISMATCH),
                Arguments.of("REG-08: Chưa đủ 18 tuổi", USER, EMAIL, PASS, PASS, LocalDate.now().minusYears(17), PHONE, ResultCode.UNDERAGE),
                Arguments.of("REG-09: Phone không hợp lệ", USER, EMAIL, PASS, PASS, DOB, "   ", ResultCode.INVALID_PHONE),
// 3 test kiểm tra thứ tự ưu tiên (Priority Order) khi vi phạm nhiều quy tắc cùng lúc
                Arguments.of("Priority: Username sai + Email sai -> REG-02", "1alice", "bad_email", PASS, PASS, DOB, PHONE, ResultCode.INVALID_USERNAME),
                Arguments.of("Priority: Email sai + MK yếu -> REG-04", USER, "bad_email", "weak", "weak", DOB, PHONE, ResultCode.INVALID_EMAIL),
                Arguments.of("Priority: MK yếu + Confirm lệch -> REG-06", USER, EMAIL, "weak", "Different@123", DOB, PHONE, ResultCode.WEAK_PASSWORD)
        );
    }

    @Nested
    @DisplayName("login()")
    class Login {

        @BeforeEach
        void registerUser() {
            registerDefault();
        }

        @Test
        void login_CorrectCredentials_Success() {
            assertEquals(ResultCode.SUCCESS, service.login(USER, PASS));
            assertEquals(0, account().getFailedAttempts());
            assertFalse(service.isLocked(USER));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  "})
        void login_UsernameNullEmptyBlank_ReturnsInvalidInput(String username) {
            assertEquals(ResultCode.INVALID_INPUT, service.login(username, PASS));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  "})
        void login_PasswordNullEmptyBlank_ReturnsInvalidInput(String password) {
            assertEquals(ResultCode.INVALID_INPUT, service.login(USER, password));
            assertEquals(0, account().getFailedAttempts());
        }

        @ParameterizedTest(name = "[{index}] username \"{0}\"")
        @ValueSource(strings = {"alice_01", "ALICE_01", "Alice_01"})
        void login_UsernameIgnoreCase_Success(String username) {
            assertEquals(ResultCode.SUCCESS, service.login(username, PASS));
        }

        @ParameterizedTest(name = "[{index}] password \"{0}\"")
        @ValueSource(strings = {"secret@123", "SECRET@123"})
        void login_PasswordCaseSensitive_ReturnsInvalidCredentials(String password) {
            assertEquals(ResultCode.INVALID_CREDENTIALS, service.login(USER, password));
        }

        @Test
        void login_UnknownUserAndWrongPassword_ReturnSameCode() {
            assertEquals(ResultCode.INVALID_CREDENTIALS, service.login("nobody_1", PASS));
            assertEquals(ResultCode.INVALID_CREDENTIALS, service.login(USER, WRONG));
        }

        @ParameterizedTest(name = "[{index}] sai {0} lần -> chưa khóa")
        @ValueSource(ints = {1, 2, 3, 4})
        void login_WrongPasswordLessThan5Times_IncrementsCounter(int times) {
            failLogin(times - 1);

            ResultCode result = service.login(USER, WRONG);
            assertEquals(ResultCode.INVALID_CREDENTIALS, result);
            assertEquals(times, account().getFailedAttempts());
            assertFalse(service.isLocked(USER));
        }

        @Test
        void login_WrongPassword5thTime_LocksAccount() {
            failLogin(4);

            ResultCode result = service.login(USER, WRONG);

            assertEquals(ResultCode.ACCOUNT_LOCKED, result);
            assertEquals(5, account().getFailedAttempts());
            assertTrue(service.isLocked(USER));
        }

        @ParameterizedTest(name = "[{index}] đang khóa + password \"{0}\"")
        @ValueSource(strings = {PASS, WRONG})
        void login_WhileLocked_RejectsWithoutIncrement(String password) {
            failLogin(5);

            assertEquals(ResultCode.ACCOUNT_LOCKED, service.login(USER, password));
            assertEquals(5, account().getFailedAttempts());
            assertTrue(service.isLocked(USER));
        }

        @ParameterizedTest(name = "[{index}] {0} lần sai -> {1}, locked={2}")
        @CsvSource({
                "3, SUCCESS,        false",
                "4, SUCCESS,        false",
                "5, ACCOUNT_LOCKED, true",
                "6, ACCOUNT_LOCKED, true"
        })
        void login_CorrectPasswordAfterNFailures(int failures, ResultCode expected, boolean locked) {
            failLogin(failures);

            assertEquals(expected, service.login(USER, PASS));
            assertEquals(locked, service.isLocked(USER));
        }

        @Test
        void login_AfterAdminUnlock_CounterRestartsAndCanLogin() {
            failLogin(5);
            assertEquals(ResultCode.SUCCESS, service.unlockAccount(USER));

            assertFalse(service.isLocked(USER));
            assertEquals(0, account().getFailedAttempts());
            assertEquals(ResultCode.INVALID_CREDENTIALS, service.login(USER, WRONG));
            assertEquals(1, account().getFailedAttempts());
            assertEquals(ResultCode.SUCCESS, service.login(USER, PASS));
        }

        @Test
        void login_SuccessAfterFailures_ResetsCounter() {
            failLogin(3);
            assertEquals(ResultCode.SUCCESS, service.login(USER, PASS));
            assertEquals(0, account().getFailedAttempts());
        }

        @ParameterizedTest(name = "[{index}] DISABLED + password \"{0}\"")
        @ValueSource(strings = {PASS, WRONG})
        void login_DisabledAccount_ReturnsAccountDisabled(String password) {
            service.disableAccount(USER);
            assertEquals(ResultCode.ACCOUNT_DISABLED, service.login(USER, password));
            assertEquals(0, account().getFailedAttempts());
        }
    }

    // ======================================================================
    @Nested
    @DisplayName("Quản trị & truy vấn")
    class Admin {

        @Test
        void disableAccount_ExistingUser_SetsDisabled() {
            registerDefault();
            assertEquals(ResultCode.SUCCESS, service.disableAccount("ALICE_01"));
            assertEquals(AccountStatus.DISABLED, account().getStatus());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "nobody_1"})
        void disableAccount_BlankOrUnknown_ReturnsUserNotFound(String username) {
            assertEquals(ResultCode.USER_NOT_FOUND, service.disableAccount(username));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "nobody_1"})
        void unlockAccount_BlankOrUnknown_ReturnsUserNotFound(String username) {
            assertEquals(ResultCode.USER_NOT_FOUND, service.unlockAccount(username));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "nobody_1"})
        void findByUsername_BlankOrUnknown_ReturnsEmpty(String username) {
            assertTrue(service.findByUsername(username).isEmpty());
            assertFalse(service.isLocked(username));
        }
    }
}
