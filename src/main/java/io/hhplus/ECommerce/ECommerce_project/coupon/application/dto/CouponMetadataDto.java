package io.hhplus.ECommerce.ECommerce_project.coupon.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponMetadataDto {
    private int totalQuantity;
    private boolean isActive;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    /**
     * Redis Hash에 저장할 Map으로 변환
     */
    public Map<String, String> toHashMap() {
        Map<String, String> map = new HashMap<>();
        map.put("totalQuantity", String.valueOf(totalQuantity));
        map.put("isActive", String.valueOf(isActive));
        map.put("startDate", startDate.toString());
        map.put("endDate", endDate.toString());
        return map;
    }

    /**
     * Redis Hash Map에서 DTO로 변환
     */
    public static CouponMetadataDto fromHashMap(Map<Object, Object> hashMap) {
        if (hashMap == null || hashMap.isEmpty()) {
            return null;
        }

        return CouponMetadataDto.builder()
                .totalQuantity(Integer.parseInt(hashMap.get("totalQuantity").toString()))
                .isActive(Boolean.parseBoolean(hashMap.get("isActive").toString()))
                .startDate(LocalDateTime.parse(hashMap.get("startDate").toString()))
                .endDate(LocalDateTime.parse(hashMap.get("endDate").toString()))
                .build();
    }
}
