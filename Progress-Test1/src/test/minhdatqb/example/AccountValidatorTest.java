package example;

import minhdatqb.example.AccountValidator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AccountValidatorTest {

    @ParameterizedTest(name = "[{index}] username hợp lệ: {0}")
    @ValueSource(strings = {"alice", "Alice_01", "Z____"})
    void isValidUsername_Valid(String username) {
        assertEquals(true, AccountValidator.isValidUsername(username));
    }

    @ParameterizedTest(name = "[{index}] username không hợp lệ: {0}")
    @ValueSource(strings = {"ab_1", "1alice", "_alice", "ali ce", "alice!", "alice-01"})
    void isValidUsername_Invalid(String username) {
        assertEquals(false, AccountValidator.isValidUsername(username));
    }

    @ParameterizedTest(name = "[{index}] username null/empty/blank: '{0}'")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void isValidUsername_NullEmptyBlank(String username) {
        assertEquals(false, AccountValidator.isValidUsername(username));
    }

    @ParameterizedTest(name = "[{index}] độ dài username {0} -> {1}")
    @MethodSource("usernameLengths")
    void isValidUsername_BoundaryLength(int length, boolean expected) {
        assertEquals(expected, AccountValidator.isValidUsername("a".repeat(length)));
    }

    static Stream<Arguments> usernameLengths() {
        return Stream.of(
                Arguments.of(4, false),
                Arguments.of(5, true),
                Arguments.of(6, true),
                Arguments.of(19, true),
                Arguments.of(20, true),
                Arguments.of(21, false)
        );
    }

    @ParameterizedTest(name = "[{index}] email: {0} -> {1}")
    @CsvSource({
            "test@example.com, true",
            "user.name+tag@sub.domain.co, true",
            "invalid-email, false",
            "missing-tld@domain, false",
            "@domain.com, false"
    })
    void isValidEmail_Partitions(String email, boolean expected) {
        assertEquals(expected, AccountValidator.isValidEmail(email));
    }

    @ParameterizedTest(name = "[{index}] email độ dài biên {0} -> {1}")
    @MethodSource("emailBoundaryLengths")
    void isValidEmail_BoundaryLength(String email, boolean expected) {
        assertEquals(expected, AccountValidator.isValidEmail(email));
    }

    static Stream<Arguments> emailBoundaryLengths() {
        return Stream.of(
                Arguments.of("a".repeat(88) + "@gmail.com", true),   // Tổng độ dài 99
                Arguments.of("a".repeat(89) + "@gmail.com", true),   // Tổng độ dài 100 (Biên)
                Arguments.of("a".repeat(90) + "@gmail.com", false)   // Tổng độ dài 101 (Vượt biên)
        );
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @CsvSource(delimiter = '|', value = {
            "Secret@123    | alice_01 | true  | hợp lệ",
            "secret@123    | alice_01 | false | thiếu chữ hoa",
            "'Secret @123' | alice_01 | false | chứa khoảng trắng",
            "Xalice_01@1    | alice_01 | false | chứa username",
            "Xalice_01@1    |          | true  | username null -> bỏ qua"
    })
    void isValidPassword_Partitions(String pw, String user, boolean expected, String desc) {
        String actualUser = (user == null || user.isBlank()) ? null : user.trim();
        assertEquals(expected, AccountValidator.isValidPassword(pw, actualUser));
    }

    @ParameterizedTest(name = "[{index}] password độ dài biên len={0} -> {1}")
    @MethodSource("passwordBoundaryLengths")
    void isValidPassword_BoundaryLength(int length, boolean expected) {
        assertEquals(expected, AccountValidator.isValidPassword(getPasswordOfLength(length), "alice"));
    }

    static Stream<Arguments> passwordBoundaryLengths() {
        return Stream.of(
                Arguments.of(7, false),   // Dưới biên tối thiểu 8
                Arguments.of(8, true),    // Biên tối thiểu 8
                Arguments.of(32, true),   // Biên tối đa 32
                Arguments.of(33, false)   // Vượt biên tối đa 32
        );
    }

    private static String getPasswordOfLength(int len) {
        if (len < 8) {
            return "Ab@1";
        }
        StringBuilder sb = new StringBuilder("Ab@1");
        while (sb.length() < len) {
            sb.append("a");
        }
        return sb.toString();
    }

    @ParameterizedTest(name = "[{index}] phone hợp lệ: {0}")
    @ValueSource(strings = {"0312345678", "0512345678", "0712345678", "0812345678", "0912345678"})
    void isValidPhone_Valid(String phone) {
        assertEquals(true, AccountValidator.isValidPhone(phone));
    }

    @ParameterizedTest(name = "[{index}] phone không hợp lệ: {0}")
    @ValueSource(strings = {"1312345678", "0212345678", "031234567", "03123456789", "abc"})
    void isValidPhone_Invalid(String phone) {
        assertEquals(false, AccountValidator.isValidPhone(phone));
    }

    @ParameterizedTest(name = "[{index}] sinh {0}, hôm nay {1} -> {2} tuổi")
    @CsvSource({
            "2008-09-28, 2026-09-28, 18",   // đúng sinh nhật 18
            "2008-09-29, 2026-09-28, 17",   // 18 tuổi trừ 1 ngày
            "2008-02-29, 2026-02-28, 17",   // năm nhuận
            "2008-02-29, 2026-03-01, 18"
    })
    void calculateAge_Boundaries(LocalDate dob, LocalDate today, int expected) {
        assertEquals(expected, AccountValidator.calculateAge(dob, today));
    }
}