package io.fleetcoreplatform.Managers.Database.Mappers;

import io.fleetcoreplatform.Managers.Database.DbModels.DbSeatToken;
import java.sql.Timestamp;
import java.util.UUID;
import org.apache.ibatis.annotations.*;

@Mapper
public interface SeatTokenMapper {
    @Insert(
            "INSERT INTO seat_tokens (uuid, created_by, \"group\", created_at) VALUES (#{uuid,"
                    + " jdbcType=OTHER}, #{created_by, jdbcType=OTHER}, #{group}, #{created_at})")
    void insert(
            @Param("uuid") UUID uuid,
            @Param("created_by") UUID created_by,
            @Param("group") UUID group,
            @Param("created_at") Timestamp created_at);

    @Select("SELECT * FROM seat_tokens WHERE uuid = #{uuid, jdbcType=OTHER}")
    DbSeatToken findByUuid(UUID uuid);

    @Update(
            "UPDATE seat_tokens SET created_by = #{created_by, jdbcType=OTHER}, \"group\" ="
                    + " #{group, jdbcType=OTHER}, created_at = #{created_at} WHERE uuid = #{uuid,"
                    + " jdbcType=OTHER}")
    void update(
            @Param("uuid") UUID uuid,
            @Param("created_by") UUID created_by,
            @Param("group") UUID group,
            @Param("created_at") Timestamp created_at);

    @Delete("DELETE FROM seat_tokens WHERE uuid = #{uuid, jdbcType=OTHER}")
    void delete(@Param("uuid") UUID uuid);
}
