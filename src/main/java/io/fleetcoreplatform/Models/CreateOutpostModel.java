package io.fleetcoreplatform.Models;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateOutpostModel(
        String name,
        BigDecimal latitude,
        BigDecimal longitude,
        OutpostAreaModel area,
        UUID coordinatorUUID) {}
