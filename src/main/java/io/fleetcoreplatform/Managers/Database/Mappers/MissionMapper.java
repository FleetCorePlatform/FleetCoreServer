package io.fleetcoreplatform.Managers.Database.Mappers;

import io.fleetcoreplatform.Managers.Database.DbModels.DbMission;
import java.sql.Timestamp;
import java.util.List;
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

    @Select("SELECT m.* FROM missions m INNER JOIN public.coordinators c ON m.created_by = c.uuid " +
            "WHERE m.uuid = #{uuid, jdbcType=OTHER} AND c.cognito_sub = #{sub}")
    DbMission findByIdAndCoordinator(@Param("uuid") UUID uuid, @Param("sub") String sub);

    @Select("SELECT m.* FROM missions m INNER JOIN public.coordinators c ON m.created_by = c.uuid " +
            "WHERE c.cognito_sub = #{sub}")
    List<DbMission> listByCoordinator(@Param("sub") String sub);

    @Select("""
        SELECT m.* FROM missions m
        INNER JOIN groups g ON m.group_uuid = g.uuid
        INNER JOIN outposts o ON g.outpost_uuid = o.uuid
        INNER JOIN coordinators c ON o.created_by = c.uuid
        WHERE o.uuid = #{outpostUuid, jdbcType=OTHER}
          AND c.cognito_sub = #{cognitoSub}
    """)
    List<DbMission> listMissionsByCoordinatorAndOutpost(String cognitoSub, UUID outpostUuid);
}
