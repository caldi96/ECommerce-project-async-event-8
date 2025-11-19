package io.hhplus.ECommerce.ECommerce_project.point.application.service;

import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.common.exception.PointException;
import io.hhplus.ECommerce.ECommerce_project.point.domain.entity.Point;
import io.hhplus.ECommerce.ECommerce_project.point.infrastructure.PointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PointFinderService {

    private final PointRepository pointRepository;

    public Point getPointWithLock(Long pointId) {
        return pointRepository.findByIdWithLock(pointId)
                .orElseThrow(() -> new PointException(ErrorCode.POINT_NOT_FOUND));
    }
}
