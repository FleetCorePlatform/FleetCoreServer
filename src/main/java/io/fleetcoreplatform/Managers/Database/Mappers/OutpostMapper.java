package io.fleetcoreplatform.Managers.Database.Mappers;

import io.fleetcoreplatform.Managers.Database.DbModels.DbOutpost;
import io.fleetcoreplatform.Managers.Database.TypeHandlers.GeometryTypeHandler;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.*;

@Mapper
public interface OutpostMapper {
    @Insert(
            "INSERT INTO outposts (uuid, name, latitude, longitude, area, created_by, created_at)"
                + " VALUES (#{uuid, jdbcType=OTHER}, #{name}, #{latitude}, #{longitude},"
                + " ST_GeomFromText(#{area}, 4326), #{created_by, jdbcType=OTHER}, #{created_at})")
    void insert(
            @Param("uuid") UUID uuid,
            @Param("name") String name,
            @Param("latitude") BigDecimal latitude,
            @Param("longitude") BigDecimal longitude,
            @Param("area") String area, // WKT string like "POLYGON((...))"
            @Param("created_by") UUID created_by,
            @Param("created_at") Timestamp created_at);

    @Select(
            "SELECT uuid, name, latitude, longitude, area, created_by,"
                    + " created_at FROM outposts WHERE uuid = #{uuid, jdbcType=OTHER}")
    @Results({@Result(property = "area", column = "area", typeHandler = GeometryTypeHandler.class)})
    DbOutpost findByUuid(@Param("uuid") UUID uuid);

    @Select("""
        SELECT o.uuid, o.name, o.latitude, o.longitude, o.area, o.created_by, o.created_at
        FROM outposts o
        INNER JOIN coordinators c ON o.created_by = c.uuid
        WHERE c.cognito_sub = #{cognitoSub}
    """)
    @Results({@Result(property = "area", column = "area", typeHandler = GeometryTypeHandler.class)})
    List<DbOutpost> listByCoordinator(
            @Param("cognitoSub") String cognitoSub);

    @Select("""
        SELECT o.uuid, o.name, o.latitude, o.longitude, o.area, o.created_by, o.created_at
        FROM outposts o
        INNER JOIN coordinators c ON o.created_by = c.uuid
        WHERE o.uuid = #{uuid, jdbcType=OTHER}
          AND c.cognito_sub = #{cognitoSub}
    """)
    @Results({@Result(property = "area", column = "area", typeHandler = GeometryTypeHandler.class)})
    DbOutpost findByUuidAndCoordinator(
            @Param("uuid") UUID uuid,
            @Param("cognitoSub") String cognitoSub);

    @Select(
            "SELECT uuid, name, latitude, longitude, area, created_by,"
                    + " created_at FROM outposts WHERE name = #{name}")
    @Results({@Result(property = "area", column = "area", typeHandler = GeometryTypeHandler.class)})
    DbOutpost findByName(@Param("name") String name);

    @Delete("DELETE FROM outposts WHERE uuid = #{uuid, jdbcType=OTHER}")
    void delete(@Param("uuid") UUID uuid);
}
