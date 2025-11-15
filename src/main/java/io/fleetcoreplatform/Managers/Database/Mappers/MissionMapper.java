package io.fleetcoreplatform.Managers.Database.Mappers;

import io.fleetcoreplatform.Managers.Database.DbModels.DbMission;
import java.sql.Timestamp;
import java.util.UUID;
import org.apache.ibatis.annotations.*;

@Mapper
public interface MissionMapper {
    @Insert(
            "INSERT INTO missions (uuid, group_uuid, name, bundle_url, start_time, created_by)"
                    + " VALUES (#{uuid, jdbcType=OTHER}, #{group_uuid, jdbcType=OTHER}, #{name},"
                    + " #{bundle_url}, #{start_time}, #{created_by, jdbcType=OTHER})")
    void insert(
            @Param("uuid") UUID uuid,
            @Param("group_uuid") UUID group_uuid,
            @Param("name") String name,
            @Param("bundle_url") String bundle_url,
            @Param("start_time") Timestamp start_time,
            @Param("created_by") UUID created_by);

    @Select("SELECT * FROM missions WHERE uuid = #{uuid, jdbcType=OTHER}")
    DbMission findById(@Param("uuid") UUID uuid);

    @Update(
            "UPDATE missions SET group_uuid = #{group_uuid, jdbcType=OTHER}, name = #{name},"
                    + " bundle_url = #{bundle_url}, start_time = #{start_time}, created_by ="
                    + " #{created_by} WHERE uuid = #{uuid, jdbcType=OTHER}")
    void update(
            @Param("uuid") UUID uuid,
            @Param("group_uuid") UUID group_uuid,
            @Param("name") String name,
            @Param("bundle_url") String bundle_url,
            @Param("start_time") Timestamp start_time,
            @Param("created_by") UUID created_by);

    @Delete("DELETE FROM missions WHERE uuid = #{uuid, jdbcType=OTHER}")
    void delete(@Param("uuid") UUID uuid);
}
