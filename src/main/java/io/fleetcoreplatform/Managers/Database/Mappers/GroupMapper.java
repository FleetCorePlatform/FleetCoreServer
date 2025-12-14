package io.fleetcoreplatform.Managers.Database.Mappers;

import io.fleetcoreplatform.Managers.Database.DbModels.DbGroup;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.*;

@Mapper
public interface GroupMapper {
    @Insert(
            "INSERT INTO groups (uuid, outpost_uuid, name, created_at) VALUES (#{uuid,"
                    + " jdbcType=OTHER}, #{outpost_uuid}, #{name}, #{created_at})")
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
}
