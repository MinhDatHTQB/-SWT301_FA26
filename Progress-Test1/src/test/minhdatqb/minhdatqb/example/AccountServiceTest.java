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

            Account account = service.findAccountByUsername(VALID_USER);
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
            assertNull(service.findAccountByUsername(u)); // Đảm bảo đăng ký thất bại thì KHÔNG tạo tài khoản
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
            // Đăng ký lần 1 thành công
            service.register(VALID_USER, VALID_EMAIL, VALID_PASS, VALID_PASS, VALID_DOB, VALID_PHONE);

            // Đăng ký lần 2 trùng username (thử cả viết hoa/thường)
            ResultCode result = service.register("ALICE_01", "other@example.com", VALID_PASS, VALID_PASS, VALID_DOB, VALID_PHONE);
            assertEquals(ResultCode.DUPLICATE_USERNAME, result);
        }

        @Test
        void register_DuplicateEmail_ReturnsDuplicateEmail() {
            // Đăng ký lần 1 thành công
            service.register(VALID_USER, VALID_EMAIL, VALID_PASS, VALID_PASS, VALID_DOB, VALID_PHONE);

            // Đăng ký lần 2 trùng email (thử cả viết hoa/thường)
            ResultCode result = service.register("bob_02", "ALICE@EXAMPLE.COM", VALID_PASS, VALID_PASS, VALID_DOB, VALID_PHONE);
            assertEquals(ResultCode.DUPLICATE_EMAIL, result);
        }

        @ParameterizedTest(name = "[{index}] today - {0} năm + {1} ngày -> {2}")
        @CsvSource({
                "18, 0, SUCCESS",       // Đúng sinh nhật tròn 18 tuổi
                "18, 1, UNDERAGE",      // Chưa đủ 18 tuổi (thiếu 1 ngày)
                "17, 0, UNDERAGE",      // 17 tuổi
                "0, 1, INVALID_INPUT"   // Ngày sinh ở tương lai
        })
        void register_AgeBoundary(int yearsAgo, int plusDays, ResultCode expected) {
            LocalDate dob = LocalDate.now().minusYears(yearsAgo).plusDays(plusDays);
            ResultCode result = service.register("user_" + yearsAgo + "_" + plusDays, VALID_EMAIL, VALID_PASS, VALID_PASS, dob, null);
            assertEquals(expected, result);
        }

        @ParameterizedTest(name = "[{index}] phone: '{0}' -> {1}")
        @CsvSource({
                " , SUCCESS",             // null phone được chấp nhận
                "'', SUCCESS",           // rỗng được chấp nhận
                "'   ', INVALID_PHONE",   // chỉ khoảng trắng -> lỗi
                "12345, INVALID_PHONE"   // sai định dạng -> lỗi
        })
        void register_PhoneVariations(String phone, ResultCode expected) {
            String actualPhone = (phone != null && phone.equals(" ")) ? "   " : (phone != null && phone.isEmpty() ? "" : phone);
            ResultCode result = service.register("user_phone_test", "phone@example.com", VALID_PASS, VALID_PASS, VALID_DOB, actualPhone);
            assertEquals(expected, result);
        }
    }

    static Stream<Arguments> invalidRegisterInputs() {
        return Stream.of(
                // REG-01: Thiếu thông tin bắt buộc / ngày sinh tương lai
                Arguments.of("ngày sinh ở tương lai", VALID_USER, VALID_EMAIL, VALID_PASS, VALID_PASS, LocalDate.now().plusDays(1), VALID_PHONE, ResultCode.INVALID_INPUT),

                // REG-02: Username sai định dạng
                Arguments.of("username sai định dạng", "1alice", VALID_EMAIL, VALID_PASS, VALID_PASS, VALID_DOB, VALID_PHONE, ResultCode.INVALID_USERNAME),

                // REG-04: Email sai định dạng
                Arguments.of("email sai định dạng", VALID_USER, "invalid-email", VALID_PASS, VALID_PASS, VALID_DOB, VALID_PHONE, ResultCode.INVALID_EMAIL),

                // REG-06: Mật khẩu yếu
                Arguments.of("mật khẩu yếu", VALID_USER, VALID_EMAIL, "weak", "weak", VALID_DOB, VALID_PHONE, ResultCode.WEAK_PASSWORD),

                // REG-07: Xác nhận mật khẩu không khớp
                Arguments.of("xác nhận mật khẩu không khớp", VALID_USER, VALID_EMAIL, VALID_PASS, "Different@123", VALID_DOB, VALID_PHONE, ResultCode.PASSWORD_MISMATCH),

                // REG-08: Dưới độ tuổi quy định (< 18 tuổi)
                Arguments.of("dưới 18 tuổi", VALID_USER, VALID_EMAIL, VALID_PASS, VALID_PASS, LocalDate.now().minusYears(17), VALID_PHONE, ResultCode.UNDERAGE),

                // REG-09: Số điện thoại không hợp lệ
                Arguments.of("số điện thoại không hợp lệ", VALID_USER, VALID_EMAIL, VALID_PASS, VALID_PASS, VALID_DOB, "0212345678", ResultCode.INVALID_PHONE),

                // Kiểm tra thứ tự ưu tiên (nhiều lỗi cùng lúc, trả về lỗi theo thứ tự ưu tiên từ trên xuống)
                Arguments.of("thứ tự ưu tiên: username sai + email sai", "1alice", "bad-email", VALID_PASS, VALID_PASS, VALID_DOB, VALID_PHONE, ResultCode.INVALID_USERNAME),
                Arguments.of("thứ tự ưu tiên: email sai + mk yếu", VALID_USER, "bad-email", "weak", "weak", VALID_DOB, VALID_PHONE, ResultCode.INVALID_EMAIL),
                Arguments.of("thứ tự ưu tiên: mk yếu + confirm lệch", VALID_USER, VALID_EMAIL, "weak", "Other@123", VALID_DOB, VALID_PHONE, ResultCode.WEAK_PASSWORD)
        );
    }
}