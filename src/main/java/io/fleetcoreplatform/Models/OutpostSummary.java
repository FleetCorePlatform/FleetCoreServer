package io.fleetcoreplatform.Models;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

public record OutpostSummary(String name, BigDecimal latitude, BigDecimal longitude, Timestamp createdAt, List<OutpostGroupSummary> groups, OutpostAreaModel area) {}
