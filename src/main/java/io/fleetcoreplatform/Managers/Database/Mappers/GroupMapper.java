package io.fleetcoreplatform.Managers.Database.Mappers;

import io.fleetcoreplatform.Managers.Database.DbModels.DbGroup;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import io.fleetcoreplatform.Managers.Database.TypeHandlers.DroneHomePositionTypeHandler;
import io.fleetcoreplatform.Models.DroneSummaryModel;
import org.apache.ibatis.annotations.*;

@Mapper
public interface GroupMapper {
    @Insert(
            "INSERT INTO groups (uuid, outpost_uuid, name, created_at) VALUES (#{uuid,"
                    + " jdbcType=OTHER}, #{outpost_uuid, jdbcType=OTHER}, #{name}, #{created_at})")
    void insert(
            @Param("uuid") UUID uuid,
            @Param("outpost_uuid") UUID outpost_uuid,
            @Param("name") String name,
            @Param("created_at") Timestamp created_at);

    @Select("SELECT * FROM groups WHERE uuid = #{uuid, jdbcType=OTHER}")
    DbGroup findByUuid(@Param("uuid") UUID uuid);

    @Select("SELECT * FROM groups WHERE outpost_uuid = #{outpost_uuid, jdbcType=OTHER}")
    List<DbGroup> listGroupsByOutpostUuid(@Param("outpost_uuid") UUID uuid);

    @Select("""
        SELECT g.* FROM groups g
        INNER JOIN outposts o ON g.outpost_uuid = o.uuid
        INNER JOIN coordinators c ON o.created_by = c.uuid
        WHERE g.outpost_uuid = #{outpostUuid, jdbcType=OTHER}
          AND c.cognito_sub = #{cognitoSub}
    """)
    List<DbGroup> listGroupsByOutpostUuidAndCoordinator(
            @Param("outpostUuid") UUID outpostUuid,
            @Param("cognitoSub") String cognitoSub);

    @Select("SELECT * FROM groups WHERE name = #{name}")
    DbGroup findByName(@Param("name") String name);

    @Update(
            "UPDATE groups SET outpost_uuid = #{outpost_uuid, jdbcType=OTHER}, name = #{name},"
                    + " created_at = #{created_at} WHERE uuid = #{uuid, jdbcType=OTHER}")
    void update(
            @Param("uuid") UUID uuid,
            @Param("outpost_uuid") UUID outpost_uuid,
            @Param("name") String name,
            @Param("created_at") Timestamp created_at);

    @Update(
            "UPDATE groups SET outpost_uuid = #{outpost_uuid, jdbcType=OTHER} WHERE uuid = #{uuid,"
                    + " jdbcType=OTHER}")
    void updateGroupOutpost(@Param("uuid") UUID uuid, @Param("outpost_uuid") UUID outpost_uuid);

    @Delete("DELETE FROM groups WHERE uuid = #{uuid, jdbcType=OTHER}")
    void deleteByUuid(@Param("uuid") UUID uuid);

    @Delete("DELETE FROM groups WHERE name = #{name}")
    void deleteByName(@Param("name") String name);

    @Select("""
        SELECT
            d.uuid,
            d.name,
            g.name as group_name,
            d.address,
            d.manager_version,
            d.first_discovered,
            d.home_position,
            CASE
                WHEN EXISTS (
                    SELECT 1 FROM drone_maintenance dm
                    WHERE dm.drone_uuid = d.uuid
                    AND dm.performed_at IS NULL
                ) THEN true
                ELSE false
            END as maintenance,
            NULL as remaining_percent,
            false as in_flight
        FROM drones d
        INNER JOIN groups g ON d.group_uuid = g.uuid
        INNER JOIN outposts o ON g.outpost_uuid = o.uuid
        INNER JOIN coordinators c ON o.created_by = c.uuid
        WHERE d.group_uuid = #{groupUuid, jdbcType=OTHER}
          AND c.cognito_sub = #{cognitoSub}
        LIMIT #{limit}
    """)
    @Results({
        @Result(property = "uuid", column = "uuid"),
        @Result(property = "name", column = "name"),
        @Result(property = "group_name", column = "group_name"),
        @Result(property = "address", column = "address"),
        @Result(property = "manager_version", column = "manager_version"),
        @Result(property = "first_discovered", column = "first_discovered"),
        @Result(property = "home_position", column = "home_position", typeHandler = DroneHomePositionTypeHandler.class),
        @Result(property = "maintenance", column = "maintenance"),
        @Result(property = "remaining_percent", column = "remaining_percent"),
        @Result(property = "inFlight", column = "in_flight")
    })
    List<DroneSummaryModel> listDronesByGroupAndCoordinator(
        @Param("groupUuid") UUID groupUuid,
        @Param("cognitoSub") String cognitoSub,
        @Param("limit") Integer limit
    );
}
