package io.fleetcoreplatform.Managers.Database.Mappers;

import io.fleetcoreplatform.Managers.Database.DbModels.DbOutpost;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.UUID;
import org.apache.ibatis.annotations.*;

@Mapper
public interface OutpostMapper {
    @Insert(
            "INSERT INTO outposts (uuid, name, latitude, longitude, area, created_by, created_at)"
                    + " VALUES (#{uuid, jdbcType=OTHER}, #{name}, #{latitude}, #{longitude},"
                    + " ST_GeomFromText(#{area}, 4326), #{created_by}, #{created_at})")
    void insert(
            @Param("uuid") UUID uuid,
            @Param("name") String name,
            @Param("latitude") BigDecimal latitude,
            @Param("longitude") BigDecimal longitude,
            @Param("area") String area, // WKT string like "POLYGON((...))"
            @Param("created_by") UUID created_by,
            @Param("created_at") Timestamp created_at);

    @Select(
            "SELECT uuid, name, latitude, longitude, ST_AsText(area) AS area, created_by,"
                    + " created_at FROM outposts WHERE uuid = #{uuid, jdbcType=OTHER}")
    @Results({@Result(property = "area", column = "area")})
    DbOutpost findByUuid(@Param("uuid") UUID uuid);

    @Select(
            "SELECT uuid, name, latitude, longitude, ST_AsText(area) AS area, created_by,"
                    + " created_at FROM outposts WHERE name = #{name}")
    @Results({@Result(property = "area", column = "area")})
    DbOutpost findByName(@Param("name") String name);

    @Delete("DELETE FROM outposts WHERE uuid = #{uuid, jdbcType=OTHER}")
    void delete(@Param("uuid") UUID uuid);
}
