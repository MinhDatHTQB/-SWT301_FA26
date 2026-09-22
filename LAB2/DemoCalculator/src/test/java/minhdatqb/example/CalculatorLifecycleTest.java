package minhdatqb.example;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CalculatorLifecycleTest {
    static Calculator calculator;

    @BeforeAll
    static void initAll() {
        System.out.println("==> Khởi tạo tài nguyên dùng chung");
        calculator = new Calculator();
    }

    @AfterAll
    static void cleanupAll() {
        System.out.println("==> Giải phóng tài nguyên dùng chung");
        calculator = null;
    }

    @BeforeEach
    void setUp(TestInfo info) {
        System.out.println("---> Bắt đầu test: " + info.getDisplayName());
    }

    @AfterEach
    void tearDown(TestInfo info) {
        System.out.println("---> Kết thúc test: " + info.getDisplayName());
    }

    @Test
    @DisplayName("Kiểm tra phép cộng với hai số dương")
    void testAddition() {
        // Arrange
        int a = 2, b = 3;

        // Act
        int result = calculator.add(a, b);

        // Assert
        assertEquals(5, result, "Addition should return 5");
    }

    @Test
    @DisplayName("Kiểm tra phép chia hợp lệ")
    void testDivide() {
        // Arrange
        int a = 6, b = 3;

        // Act
        int result = calculator.divide(a, b);

        // Assert
        assertEquals(2, result);
    }
}