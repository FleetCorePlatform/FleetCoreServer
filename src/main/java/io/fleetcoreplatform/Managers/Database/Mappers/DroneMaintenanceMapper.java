package io.fleetcoreplatform.Managers.Database.Mappers;

import io.fleetcoreplatform.Managers.Database.DbModels.DbDroneMaintenance;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import io.fleetcoreplatform.Models.MaintenanceSummary;
import org.apache.ibatis.annotations.*;

@Mapper
public interface DroneMaintenanceMapper {
    @Insert(
            "INSERT INTO drone_maintenance (uuid, drone_uuid, performed_by, maintenance_type,"
                    + " description, created_at, performed_at) VALUES (#{uuid, jdbcType=OTHER}, #{drone_uuid,"
                    + " jdbcType=OTHER}, #{performed_by, jdbcType=OTHER}, #{maintenance_type},"
                    + " #{description}, #{created_at}, #{performed_at})")
    void insert(
            @Param("uuid") UUID uuid,
            @Param("drone_uuid") UUID drone_uuid,
            @Param("performed_by") UUID performed_by,
            @Param("maintenance_type") String maintenance_type,
            @Param("description") String description,
            @Param("created_at") Timestamp created_at,
            @Param("performed_at") Timestamp performed_at);

    @Select("""
        SELECT m.* FROM drone_maintenance m
        INNER JOIN drones d ON m.drone_uuid = d.uuid
        INNER JOIN groups g ON d.group_uuid = g.uuid
        INNER JOIN outposts o ON g.outpost_uuid = o.uuid
        INNER JOIN coordinators c ON o.created_by = c.uuid
        WHERE m.uuid = #{uuid, jdbcType=OTHER}
          AND c.cognito_sub = #{cognitoSub}
    """)
    DbDroneMaintenance findByUuidAndCoordinator(@Param("uuid") UUID uuid, @Param("cognitoSub") String cognitoSub);

    @Update("""
        UPDATE drone_maintenance
        SET performed_by = #{performed_by, jdbcType=OTHER},
            performed_at = #{performed_at}
        WHERE uuid = #{uuid, jdbcType=OTHER}
    """)
    void markAsComplete(DbDroneMaintenance maintenance);

    @Select("""
        SELECT
            m.uuid,
            m.drone_uuid,
            d.name as drone_name,
            g.name as drone_group_name,
            m.performed_by,
            m.maintenance_type,
            m.description,
            m.created_at,
            m.performed_at
        FROM drone_maintenance m
        INNER JOIN drones d ON m.drone_uuid = d.uuid
        INNER JOIN groups g ON d.group_uuid = g.uuid
        INNER JOIN outposts o ON g.outpost_uuid = o.uuid
        INNER JOIN coordinators c ON o.created_by = c.uuid
        WHERE o.uuid = #{outpost_uuid, jdbcType=OTHER}
          AND c.cognito_sub = #{cognito_sub}
    """)
    List<MaintenanceSummary> listByOutpostAndCoordinator(
        @Param("outpost_uuid") UUID outpost_uuid,
        @Param("cognito_sub") String cognitoSub
    );

    @Delete("DELETE FROM drone_maintenance WHERE uuid = #{uuid, jdbcType=OTHER}")
    void delete(@Param("uuid") UUID uuid);
}
