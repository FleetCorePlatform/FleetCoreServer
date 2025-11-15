package io.fleetcoreplatform.Managers.Database.Mappers;

import io.fleetcoreplatform.Managers.Database.DbModels.DbDrone;
import io.fleetcoreplatform.Managers.Database.Providers.DbDroneUpdateProvider;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.*;

@Mapper
public interface DroneMapper {
    @Select("SELECT * FROM drones WHERE uuid = #{uuid, jdbcType=OTHER}")
    DbDrone findByUuid(@Param("uuid") UUID uuid);

    @Select("SELECT * FROM drones LIMIT ${limit}")
    List<DbDrone> listDrones(@Param("limit") int limit);

    @Select("SELECT * FROM drones WHERE group_uuid = #{group_uuid, jdbcType=OTHER} LIMIT ${limit}")
    List<DbDrone> listDronesByGroupUuid(
            @Param("group_uuid") UUID group_uuid, @Param("limit") int limit);

    @Select("SELECT * FROM drones WHERE name = #{name}")
    DbDrone findByName(@Param("name") String name);

    @Insert(
            "INSERT INTO drones (uuid, name, group_uuid, address, manager_version,"
                    + " first_discovered) VALUES (#{uuid, jdbcType=OTHER}, #{name}, #{groupUuid,"
                    + " jdbcType=OTHER}, #{address}, #{managerVersion}, #{firstDiscovered})")
    void insertDrone(
            @Param("uuid") UUID uuid,
            @Param("name") String name,
            @Param("groupUuid") UUID groupUuid,
            @Param("address") String address,
            @Param("managerVersion") String managerVersion,
            @Param("firstDiscovered") Timestamp firstDiscovered);

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
