package io.fleetcoreplatform.Managers.Database.Mappers;

import io.fleetcoreplatform.Managers.Database.DbModels.DbDroneMaintenance;
import java.sql.Timestamp;
import java.util.UUID;
import org.apache.ibatis.annotations.*;

@Mapper
public interface DroneMaintenanceMapper {
    @Insert(
            "INSERT INTO drone_maintenance (uuid, drone_uuid, performed_by, maintenance_type,"
                    + " description, performed_at) VALUES (#{uuid, jdbcType=OTHER}, #{drone_uuid,"
                    + " jdbcType=OTHER}, #{performed_by, jdbcType=OTHER}, #{maintenance_type},"
                    + " #{description}, #{performed_at})")
    void insert(
            @Param("uuid") UUID uuid,
            @Param("drone_uuid") UUID drone_uuid,
            @Param("performed_by") UUID performed_by,
            @Param("maintenance_type") String maintenance_type,
            @Param("description") String description,
            @Param("performed_at") Timestamp performed_at);

    @Select("SELECT * FROM drone_maintenance WHERE uuid = #{uuid, jdbcType=OTHER}")
    DbDroneMaintenance findById(@Param("uuid") UUID uuid);

    @Update(
            "UPDATE drone_maintenance SET drone_uuid = #{drone_uuid, jdbcType=OTHER}, performed_by"
                    + " = #{performed_by}, maintenance_type = #{maintenance_type}, description ="
                    + " #{description}, performed_at = #{performed_at} WHERE uuid = #{uuid}")
    void update(
            @Param("uuid") UUID uuid,
            @Param("drone_uuid") UUID drone_uuid,
            @Param("performed_by") UUID performed_by,
            @Param("maintenance_type") String maintenance_type,
            @Param("description") String description,
            @Param("performed_at") Timestamp performed_at);

    @Delete("DELETE FROM drone_maintenance WHERE uuid = #{uuid, jdbcType=OTHER}")
    void delete(@Param("uuid") UUID uuid);
}
