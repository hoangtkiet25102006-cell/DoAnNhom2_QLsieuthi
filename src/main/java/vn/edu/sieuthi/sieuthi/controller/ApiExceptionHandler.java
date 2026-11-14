package vn.edu.sieuthi.sieuthi.controller;

import java.util.Map;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> xuLyDuLieuKhongHopLe(IllegalArgumentException loi) {
        return ResponseEntity.badRequest().body(Map.of("message", loi.getMessage()));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Map<String, String>> xuLyMaTrung(DuplicateKeyException loi) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of("message", "Mã sản phẩm hoặc mã vạch đã tồn tại."));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> xuLyLoiHttp(ResponseStatusException loi) {
        String thongBao = loi.getReason() == null ? "Yêu cầu không hợp lệ." : loi.getReason();
        return ResponseEntity.status(loi.getStatusCode()).body(Map.of("message", thongBao));
    }
}
