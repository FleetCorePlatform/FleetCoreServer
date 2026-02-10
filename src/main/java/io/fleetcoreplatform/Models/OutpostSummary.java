package io.fleetcoreplatform.Models;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

public record OutpostSummary(String name, UUID uuid, BigDecimal latitude, BigDecimal longitude, Timestamp createdAt, List<OutpostGroupSummary> groups, OutpostAreaModel area) {}
