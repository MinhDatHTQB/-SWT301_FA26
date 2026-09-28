package minhdatqb.example;

/** Kết quả của yêu cầu quên mật khẩu: mã kết quả + token (null nếu không thành công). */
 public record TokenResult(ResultCode code, String token) {
}