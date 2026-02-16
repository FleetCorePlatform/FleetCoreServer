package io.fleetcoreplatform.Managers.Database.Mappers;

import io.fleetcoreplatform.Managers.Database.DbModels.DbDrone;
import io.fleetcoreplatform.Managers.Database.Providers.DbDroneUpdateProvider;
import io.fleetcoreplatform.Managers.Database.TypeHandlers.GeometryTypeHandler;
import io.fleetcoreplatform.Managers.Database.TypeHandlers.StringArrayTypeHandler;
import io.fleetcoreplatform.Models.DroneHomePositionModel;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import org.apache.ibatis.annotations.*;

@Mapper
public interface DroneMapper {
    @Select(
            "SELECT uuid, name, group_uuid, address, manager_version, first_discovered,"
                    + " home_position, model, capabilities, signaling_channel_name FROM drones WHERE uuid = #{uuid,"
                    + " jdbcType=OTHER}")
    @Results({
        @Result(
                property = "home_position",
                column = "home_position",
                typeHandler = GeometryTypeHandler.class),
        @Result(property = "capabilities", column = "capabilities", typeHandler = StringArrayTypeHandler.class)
    })
    DbDrone findByUuid(@Param("uuid") UUID uuid);

    @Select("""
        SELECT d.uuid, d.name, d.group_uuid, d.address, d.manager_version, d.first_discovered, d.home_position, d.model, d.capabilities, d.signaling_channel_name FROM drones d
        INNER JOIN groups g ON d.group_uuid = g.uuid
        INNER JOIN outposts o ON g.outpost_uuid = o.uuid
        INNER JOIN coordinators c ON o.created_by = c.uuid
        WHERE d.uuid = #{uuid, jdbcType=OTHER}
          AND c.cognito_sub = #{cognitoSub}
    """)
    @Results({
        @Result(
                property = "home_position",
                column = "home_position",
                typeHandler = GeometryTypeHandler.class),
        @Result(property = "capabilities", column = "capabilities", typeHandler = StringArrayTypeHandler.class)
    })
    DbDrone findByUuidAndCoordinator(@Param("uuid") UUID uuid, @Param("cognitoSub") String cognitoSub);

    @Select(
            "SELECT uuid, name, group_uuid, address, manager_version, first_discovered, "
                    + "home_position, model, capabilities, signaling_channel_name FROM drones LIMIT ${limit}")
    @Results({
        @Result(
                property = "home_position",
                column = "home_position",
                typeHandler = GeometryTypeHandler.class),
        @Result(property = "capabilities", column = "capabilities", typeHandler = StringArrayTypeHandler.class)
    })
    List<DbDrone> listDrones(@Param("limit") int limit);

    @Select(
            "SELECT uuid, name, group_uuid, address, manager_version, first_discovered,"
                    + " home_position, model, capabilities, signaling_channel_name FROM drones WHERE group_uuid ="
                    + " #{groupUuid, jdbcType=OTHER} LIMIT ${limit}")
    @Results({
        @Result(
                property = "home_position",
                column = "home_position",
                typeHandler = GeometryTypeHandler.class),
        @Result(property = "capabilities", column = "capabilities", typeHandler = StringArrayTypeHandler.class)
    })
    List<DbDrone> listDronesByGroupUuid(
            @Param("groupUuid") UUID groupUuid, @Param("limit") int limit);

    @Select(
            """
        SELECT d.uuid, d.name, d.group_uuid, d.address, d.manager_version, d.first_discovered, d.home_position, d.model, d.capabilities, d.signaling_channel_name
        FROM drones d
        INNER JOIN groups g ON d.group_uuid = g.uuid
        INNER JOIN outposts o ON g.outpost_uuid = o.uuid
        INNER JOIN coordinators c ON o.created_by = c.uuid
        WHERE d.group_uuid = #{groupUuid, jdbcType=OTHER}
          AND c.cognito_sub = #{cognitoSub}
        LIMIT ${limit}
    """)
    @Results({
        @Result(
                property = "home_position",
                column = "home_position",
                typeHandler = GeometryTypeHandler.class),
        @Result(property = "capabilities", column = "capabilities", typeHandler = StringArrayTypeHandler.class)
    })
    List<DbDrone> listDronesByGroupAndCoordinator(
            @Param("groupUuid") UUID groupUuid,
            @Param("cognitoSub") String cognitoSub,
            @Param("limit") int limit);

    @Select(
            "SELECT uuid, name, group_uuid, address, manager_version, first_discovered, "
                    + "home_position, model, capabilities, signaling_channel_name FROM drones WHERE name = #{name}")
    @Results({
        @Result(
                property = "home_position",
                column = "home_position",
                typeHandler = GeometryTypeHandler.class),
        @Result(property = "capabilities", column = "capabilities", typeHandler = StringArrayTypeHandler.class)
    })
    DbDrone findByName(@Param("name") String name);

    @Insert(
            "INSERT INTO drones (uuid, name, group_uuid, address, manager_version,"
                    + " first_discovered, home_position, model, capabilities, signaling_channel_name) VALUES (#{uuid, jdbcType=OTHER}, #{name},"
                    + " #{groupUuid, jdbcType=OTHER}, #{address}, #{managerVersion},"
                    + " #{firstDiscovered}, st_pointz(#{homePosition.x}, #{homePosition.y},"
                    + " #{homePosition.z}), #{model}, #{capabilities, typeHandler=io.fleetcoreplatform.Managers.Database.TypeHandlers.StringArrayTypeHandler}, #{signalingChannelName, jdbcType=OTHER})")
    void insertDrone(
            @Param("uuid") UUID uuid,
            @Param("name") String name,
            @Param("groupUuid") UUID groupUuid,
            @Param("address") String address,
            @Param("managerVersion") String managerVersion,
            @Param("firstDiscovered") Timestamp firstDiscovered,
            @Param("homePosition") DroneHomePositionModel homePosition,
            @Param("model") String model,
            @Param("capabilities") List<String> capabilities,
            @Param("signalingChannelName") UUID signalingChannelName
    );

    @UpdateProvider(type = DbDroneUpdateProvider.class, method = "update")
    void updateDrone(@Param("uuid") UUID uuid, @Param("drone") DbDrone drone);

    @Update("UPDATE drones SET group_uuid = null WHERE uuid = #{uuid, jdbcType=OTHER}")
    void ungroupDrone(@Param("uuid") UUID uuid);

    @Update(
            "UPDATE drones SET group_uuid = #{group_uuid, jdbcType=OTHER} WHERE uuid = #{uuid,"
                    + " jdbcType=OTHER}")
    void addToGroup(@Param("uuid") UUID uuid, @Param("group_uuid") UUID group_uuid);

    @Delete("DELETE FROM drones WHERE uuid = #{uuid, jdbcType=OTHER}")
    void deleteDrone(UUID uuid);
}