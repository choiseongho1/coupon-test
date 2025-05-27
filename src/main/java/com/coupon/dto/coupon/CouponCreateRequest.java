package com.coupon.dto.coupon;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CouponCreateRequest {

    @NotBlank(message = "쿠폰 제목은 필수 입력값입니다.")
    private String title;

    @Min(value = 1, message = "총 수량은 1개 이상이어야 합니다.")
    private int totalQuantity;

    @NotNull(message = "유효 시작일은 필수 입력값입니다.")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime validFrom;

    @NotNull(message = "유효 종료일은 필수 입력값입니다.")
    @Future(message = "유효 종료일은 현재 시간 이후여야 합니다.")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime validTo;
}
