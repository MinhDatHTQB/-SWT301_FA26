package minhdatqb.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class AccountServiceTest {

    AccountService service;

    // Các hằng số dữ liệu hợp lệ mẫu dùng chung cho các test case
    private static final String VALID_USER = "alice_01";
    private static final String VALID_EMAIL = "alice@example.com";
    private static final String VALID_PASS = "Secret@123";
    private static final LocalDate VALID_DOB = LocalDate.now().minusYears(20); // 20 tuổi, hợp lệ
    private static final String VALID_PHONE = "0912345678";

    @BeforeEach
    void setUp() {
        service = new AccountService(); // mỗi test một service mới -> đảm bảo độc lập
    }

    @Nested
    class Register {

        @Test
        void register_Success_ShouldStoreAccountWithCorrectState() {
            ResultCode result = service.register(
                    VALID_USER, VALID_EMAIL, VALID_PASS, VALID_PASS, VALID_DOB, VALID_PHONE
            );

            assertEquals(ResultCode.SUCCESS, result);

            Account account = service.findByUsername(VALID_USER).orElse(null);
            assertNotNull(account);
            assertEquals(VALID_USER, account.getUsername());
            assertEquals(VALID_EMAIL.toLowerCase(), account.getEmail()); // Đảm bảo email lưu lowercase
            assertEquals(AccountStatus.ACTIVE, account.getStatus());
            assertEquals(0, account.getFailedAttempts());
            assertFalse(account.isLocked());

            // Kiểm tra mật khẩu không lưu dạng rõ mà được hash
            assertNotEquals(VALID_PASS, account.getCurrentPasswordHash());
            assertNotNull(account.getSalt());
        }

        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("minhdatqb.example.AccountServiceTest#invalidRegisterInputs")
        void register_InvalidInput_ReturnsExpectedCode(String desc, String u, String e, String p, String c,
                                                       LocalDate dob, String phone, ResultCode expected) {
            assertEquals(expected, service.register(u, e, p, c, dob, phone));
            assertTrue(service.findByUsername(u).isEmpty()); // Đảm bảo đăng ký thất bại thì KHÔNG tạo tài khoản
        }

        @ParameterizedTest(name = "[{index}] username null/empty/blank: '{0}'")
        @NullAndEmptySource
        @ValueSource(strings = {" "})
        void register_BlankUsername_ReturnsInvalidInput(String username) {
            ResultCode result = service.register(username, VALID_EMAIL, VALID_PASS, VALID_PASS, VALID_DOB, VALID_PHONE);
            assertEquals(ResultCode.INVALID_INPUT, result);
        }

        @ParameterizedTest(name = "[{index}] email null/empty/blank: '{0}'")
        @NullAndEmptySource
        @ValueSource(strings = {" "})
        void register_BlankEmail_ReturnsInvalidInput(String email) {
            ResultCode result = service.register(VALID_USER, email, VALID_PASS, VALID_PASS, VALID_DOB, VALID_PHONE);
            assertEquals(ResultCode.INVALID_INPUT, result);
        }

        @ParameterizedTest(name = "[{index}] password null/empty/blank: '{0}'")
        @NullAndEmptySource
        @ValueSource(strings = {" "})
        void register_BlankPassword_ReturnsInvalidInput(String password) {
            ResultCode result = service.register(VALID_USER, VALID_EMAIL, password, password, VALID_DOB, VALID_PHONE);
            assertEquals(ResultCode.INVALID_INPUT, result);
        }

        @Test
        void register_DuplicateUsername_ReturnsDuplicateUsername() {
            service.register(VALID_USER, VALID_EMAIL, VALID_PASS, VALID_PASS, VALID_DOB, VALID_PHONE);
            ResultCode result = service.register("ALICE_01", "other@example.com", VALID_PASS, VALID_PASS, VALID_DOB, VALID_PHONE);
            assertEquals(ResultCode.DUPLICATE_USERNAME, result);
        }

        @Test
        void register_DuplicateEmail_ReturnsDuplicateEmail() {
            service.register(VALID_USER, VALID_EMAIL, VALID_PASS, VALID_PASS, VALID_DOB, VALID_PHONE);
            ResultCode result = service.register("bob_02", "ALICE@EXAMPLE.COM", VALID_PASS, VALID_PASS, VALID_DOB, VALID_PHONE);
            assertEquals(ResultCode.DUPLICATE_EMAIL, result);
        }

        @ParameterizedTest(name = "[{index}] today - {0} năm + {1} ngày -> {2}")
        @CsvSource({
                "18, 0, SUCCESS",
                "18, 1, UNDERAGE",
                "17, 0, UNDERAGE",
                "0, 1, INVALID_INPUT"
        })
        void register_AgeBoundary(int yearsAgo, int plusDays, ResultCode expected) {
            LocalDate dob = LocalDate.now().minusYears(yearsAgo).plusDays(plusDays);
            ResultCode result = service.register("user_" + yearsAgo + "_" + plusDays, VALID_EMAIL, VALID_PASS, VALID_PASS, dob, null);
            assertEquals(expected, result);
        }

        @ParameterizedTest(name = "[{index}] phone: '{0}' -> {1}")
        @CsvSource({
                " , SUCCESS",
                "'', SUCCESS",
                "'   ', INVALID_PHONE",
                "12345, INVALID_PHONE"
        })
        void register_PhoneVariations(String phone, ResultCode expected) {
            String actualPhone = (phone != null && phone.equals(" ")) ? "   " : (phone != null && phone.isEmpty() ? "" : phone);
            ResultCode result = service.register("user_phone_test", "phone@example.com", VALID_PASS, VALID_PASS, VALID_DOB, actualPhone);
            assertEquals(expected, result);
        }
    }

    @Nested
    class Login {

        private static final String USER = "alice_01";
        private static final String PASS = "Secret@123";
        private static final String WRONG_PASS = "Wrong@123";

        @BeforeEach
        void registerDefaultUser() {
            service.register(USER, "alice@example.com", PASS, PASS, VALID_DOB, VALID_PHONE);
        }

        @Test
        void login_ValidCredentials_ReturnsSuccessAndResetsAttempts() {
            service.login(USER, WRONG_PASS);
            service.login(USER, WRONG_PASS);

            assertEquals(ResultCode.SUCCESS, service.login(USER, PASS));
            assertEquals(0, service.findByUsername(USER).get().getFailedAttempts());
            assertFalse(service.isLocked(USER));
        }

        @Test
        void login_NonExistentUser_ReturnsInvalidCredentials() {
            assertEquals(ResultCode.INVALID_CREDENTIALS, service.login("ghost_user", PASS));
        }

        @ParameterizedTest(name = "[{index}] tài khoản DISABLED, thử pass đúng/sai -> ACCOUNT_DISABLED")
        @ValueSource(strings = {"Secret@123", "Wrong@123"})
        void login_DisabledAccount_ReturnsAccountDisabled(String password) {
            service.disableAccount(USER);
            assertEquals(ResultCode.ACCOUNT_DISABLED, service.login(USER, password));
        }

        @ParameterizedTest(name = "[{index}] số lần sai: {0} -> trả về INVALID_CREDENTIALS, failedAttempts={0}")
        @ValueSource(ints = {1, 2, 3, 4})
        void login_FailedAttemptsUnderLimit_IncrementsCounterAndDoesNotLock(int attempts) {
            for (int i = 0; i < attempts; i++) {
                assertEquals(ResultCode.INVALID_CREDENTIALS, service.login(USER, WRONG_PASS));
            }
            assertEquals(attempts, service.findByUsername(USER).get().getFailedAttempts());
            assertFalse(service.isLocked(USER));
        }

        @Test
        void login_FifthFailure_LocksAccount() {
            for (int i = 0; i < 4; i++) {
                service.login(USER, WRONG_PASS);
            }
            assertEquals(ResultCode.ACCOUNT_LOCKED, service.login(USER, WRONG_PASS));
            assertTrue(service.isLocked(USER));
        }

        @Test
        void login_WhenAlreadyLocked_AttemptsDoNotChangeCounterAndReturnLocked() {
            for (int i = 0; i < 5; i++) {
                service.login(USER, WRONG_PASS);
            }
            assertTrue(service.isLocked(USER));
            int attemptsBefore = service.findByUsername(USER).get().getFailedAttempts();

            assertEquals(ResultCode.ACCOUNT_LOCKED, service.login(USER, PASS));
            assertEquals(ResultCode.ACCOUNT_LOCKED, service.login(USER, WRONG_PASS));
            assertEquals(attemptsBefore, service.findByUsername(USER).get().getFailedAttempts());
        }

        @ParameterizedTest(name = "[{index}] {0} lần sai -> expected={1}, locked={2}")
        @CsvSource({
                "4, SUCCESS, false",
                "5, ACCOUNT_LOCKED, true",
                "6, ACCOUNT_LOCKED, true"
        })
        void login_CorrectPasswordAfterNFailures(int failures, ResultCode expected, boolean locked) {
            for (int i = 0; i < failures; i++) {
                service.login(USER, WRONG_PASS);
            }
            ResultCode result = service.login(USER, PASS);
            assertEquals(expected, result);
            assertEquals(locked, service.isLocked(USER));
        }

        @Test
        void login_AfterAdminUnlock_CounterRestartsAndCanLogin() {
            for (int i = 0; i < 5; i++) {
                service.login(USER, WRONG_PASS);
            }
            assertEquals(ResultCode.SUCCESS, service.unlockAccount(USER));

            assertFalse(service.isLocked(USER));
            assertEquals(ResultCode.INVALID_CREDENTIALS, service.login(USER, WRONG_PASS));
            assertEquals(1, service.findByUsername(USER).get().getFailedAttempts());
            assertEquals(ResultCode.SUCCESS, service.login(USER, PASS));
        }

        @Test
        void login_UsernameCaseInsensitive_PasswordCaseSensitive() {
            assertEquals(ResultCode.SUCCESS, service.login("ALICE_01", PASS));
            assertEquals(ResultCode.INVALID_CREDENTIALS, service.login(USER, "secret@123"));
        }

        @ParameterizedTest(name = "[{index}] username: '{0}', password: '{1}' -> INVALID_INPUT")
        @NullAndEmptySource
        @ValueSource(strings = {" "})
        void login_BlankUsernameOrPassword_ReturnsInvalidInput(String invalidInput) {
            assertEquals(ResultCode.INVALID_INPUT, service.login(invalidInput, PASS));
            assertEquals(ResultCode.INVALID_INPUT, service.login(USER, invalidInput));
        }
    }

    static Stream<Arguments> invalidRegisterInputs() {
        return Stream.of(
                Arguments.of("ngày sinh ở tương lai", VALID_USER, VALID_EMAIL, VALID_PASS, VALID_PASS, LocalDate.now().plusDays(1), VALID_PHONE, ResultCode.INVALID_INPUT),
                Arguments.of("username sai định dạng", "1alice", VALID_EMAIL, VALID_PASS, VALID_PASS, VALID_DOB, VALID_PHONE, ResultCode.INVALID_USERNAME),
                Arguments.of("email sai định dạng", VALID_USER, "invalid-email", VALID_PASS, VALID_PASS, VALID_DOB, VALID_PHONE, ResultCode.INVALID_EMAIL),
                Arguments.of("mật khẩu yếu", VALID_USER, VALID_EMAIL, "weak", "weak", VALID_DOB, VALID_PHONE, ResultCode.WEAK_PASSWORD),
                Arguments.of("xác nhận mật khẩu không khớp", VALID_USER, VALID_EMAIL, VALID_PASS, "Different@123", VALID_DOB, VALID_PHONE, ResultCode.PASSWORD_MISMATCH),
                Arguments.of("dưới 18 tuổi", VALID_USER, VALID_EMAIL, VALID_PASS, VALID_PASS, LocalDate.now().minusYears(17), VALID_PHONE, ResultCode.UNDERAGE),
                Arguments.of("số điện thoại không hợp lệ", VALID_USER, VALID_EMAIL, VALID_PASS, VALID_PASS, VALID_DOB, "0212345678", ResultCode.INVALID_PHONE),
                Arguments.of("thứ tự ưu tiên: username sai + email sai", "1alice", "bad-email", VALID_PASS, VALID_PASS, VALID_DOB, VALID_PHONE, ResultCode.INVALID_USERNAME),
                Arguments.of("thứ tự ưu tiên: email sai + mk yếu", VALID_USER, "bad-email", "weak", "weak", VALID_DOB, VALID_PHONE, ResultCode.INVALID_EMAIL),
                Arguments.of("thứ tự ưu tiên: mk yếu + confirm lệch", VALID_USER, VALID_EMAIL, "weak", "Other@123", VALID_DOB, VALID_PHONE, ResultCode.WEAK_PASSWORD)
        );
    }
}