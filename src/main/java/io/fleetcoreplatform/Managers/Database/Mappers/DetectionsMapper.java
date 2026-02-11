package io.fleetcoreplatform.Managers.Database.Mappers;

import io.fleetcoreplatform.Managers.Database.DbModels.DbDetection;
import io.fleetcoreplatform.Managers.Database.TypeHandlers.PolygonPointTypeHandler;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.*;

@Mapper
public interface DetectionsMapper {
    @Select("""
        SELECT d.uuid, d.mission_uuid, d.detected_by_drone_uuid, d.object, d.confidence, d.false_positive, d.detected_at, d.location, d.image_key
        FROM detections d
        INNER JOIN missions m ON d.mission_uuid = m.uuid
        INNER JOIN groups g ON m.group_uuid = g.uuid
        INNER JOIN outposts o ON g.outpost_uuid = o.uuid
        INNER JOIN coordinators c ON o.created_by = c.uuid
        WHERE d.uuid = #{uuid, jdbcType=OTHER}
          AND c.cognito_sub = #{cognitoSub}
    """)
    @Results(value = {
        @Result(property = "uuid", column = "uuid", id = true),
        @Result(property = "mission_uuid", column = "mission_uuid"),
        @Result(property = "detected_by_drone_uuid", column = "detected_by_drone_uuid"),
        @Result(property = "object", column = "object"),
        @Result(property = "confidence", column = "confidence"),
        @Result(property = "false_positive", column = "false_positive"),
        @Result(property = "detected_at", column = "detected_at"),
        @Result(property = "image_key", column = "image_key"),
        @Result(property = "location", column = "location", typeHandler = PolygonPointTypeHandler.class)
    })
    DbDetection findByUuidAndCoordinator(
            @Param("uuid") UUID uuid,
            @Param("cognitoSub") String cognitoSub);

    @Select("""
        SELECT d.uuid, d.mission_uuid, d.detected_by_drone_uuid, d.object, d.confidence, d.false_positive, d.detected_at, d.location, d.image_key
        FROM detections d
        INNER JOIN missions m ON d.mission_uuid = m.uuid
        INNER JOIN groups g ON m.group_uuid = g.uuid
        INNER JOIN outposts o ON g.outpost_uuid = o.uuid
        INNER JOIN coordinators c ON o.created_by = c.uuid
        WHERE d.mission_uuid = #{missionUuid, jdbcType=OTHER}
          AND c.cognito_sub = #{cognitoSub}
        ORDER BY d.detected_at DESC
    """)
    // FIX: Explicitly define results
    @Results(value = {
        @Result(property = "uuid", column = "uuid", id = true),
        @Result(property = "mission_uuid", column = "mission_uuid"),
        @Result(property = "detected_by_drone_uuid", column = "detected_by_drone_uuid"),
        @Result(property = "object", column = "object"),
        @Result(property = "confidence", column = "confidence"),
        @Result(property = "false_positive", column = "false_positive"),
        @Result(property = "detected_at", column = "detected_at"),
        @Result(property = "image_key", column = "image_key"),
        @Result(property = "location", column = "location", typeHandler = PolygonPointTypeHandler.class)
    })
    List<DbDetection> listByMissionAndCoordinator(
            @Param("missionUuid") UUID missionUuid,
            @Param("cognitoSub") String cognitoSub);

    @Select("""
        SELECT d.uuid, d.mission_uuid, d.detected_by_drone_uuid, d.object, d.confidence, d.false_positive, d.detected_at, d.location, d.image_key
        FROM detections d
        INNER JOIN missions m ON d.mission_uuid = m.uuid
        INNER JOIN groups g ON m.group_uuid = g.uuid
        INNER JOIN outposts o ON g.outpost_uuid = o.uuid
        INNER JOIN coordinators c ON o.created_by = c.uuid
        WHERE d.mission_uuid = #{missionUuid, jdbcType=OTHER}
          AND c.cognito_sub = #{cognitoSub}
          AND g.uuid = #{groupUuid, jdbcType=OTHER}
        ORDER BY d.detected_at DESC
    """)
    // FIX: Explicitly define results (This was the one crashing)
    @Results(value = {
        @Result(property = "uuid", column = "uuid", id = true),
        @Result(property = "mission_uuid", column = "mission_uuid"),
        @Result(property = "detected_by_drone_uuid", column = "detected_by_drone_uuid"),
        @Result(property = "object", column = "object"),
        @Result(property = "confidence", column = "confidence"),
        @Result(property = "false_positive", column = "false_positive"),
        @Result(property = "detected_at", column = "detected_at"),
        @Result(property = "image_key", column = "image_key"),
        @Result(property = "location", column = "location", typeHandler = PolygonPointTypeHandler.class)
    })
    List<DbDetection> listByMissionGroupAndCoordinator(
            @Param("missionUuid") UUID missionUuid,
            @Param("groupUuid") UUID groupUuid,
            @Param("cognitoSub") String cognitoSub);

    @Select("""
        SELECT d.uuid, d.mission_uuid, d.detected_by_drone_uuid, d.object, d.confidence, d.false_positive, d.detected_at, d.location, d.image_key
        FROM detections d
        INNER JOIN drones dr ON d.detected_by_drone_uuid = dr.uuid
        INNER JOIN groups g ON dr.group_uuid = g.uuid
        INNER JOIN outposts o ON g.outpost_uuid = o.uuid
        INNER JOIN coordinators c ON o.created_by = c.uuid
        WHERE d.detected_by_drone_uuid = #{droneUuid, jdbcType=OTHER}
          AND c.cognito_sub = #{cognitoSub}
        ORDER BY d.detected_at DESC
        LIMIT #{limit}
    """)
    @Results(value = {
        @Result(property = "uuid", column = "uuid", id = true),
        @Result(property = "mission_uuid", column = "mission_uuid"),
        @Result(property = "detected_by_drone_uuid", column = "detected_by_drone_uuid"),
        @Result(property = "object", column = "object"),
        @Result(property = "confidence", column = "confidence"),
        @Result(property = "false_positive", column = "false_positive"),
        @Result(property = "detected_at", column = "detected_at"),
        @Result(property = "image_key", column = "image_key"),
        @Result(property = "location", column = "location", typeHandler = PolygonPointTypeHandler.class)
    })
    List<DbDetection> listByDroneAndCoordinator(
            @Param("droneUuid") UUID droneUuid,
            @Param("cognitoSub") String cognitoSub,
            @Param("limit") int limit);

    @Update("""
        UPDATE detections
        SET false_positive = #{status}
        WHERE uuid = #{uuid, jdbcType=OTHER}
          AND mission_uuid IN (
              SELECT m.uuid
              FROM missions m
              INNER JOIN groups g ON m.group_uuid = g.uuid
              INNER JOIN outposts o ON g.outpost_uuid = o.uuid
              INNER JOIN coordinators c ON o.created_by = c.uuid
              WHERE c.cognito_sub = #{cognitoSub}
          )
    """)
    int validateDetection(
        @Param("uuid") UUID uuid,
        @Param("status") boolean status,
        @Param("cognitoSub") String cognitoSub
    );

    @Delete("""
        DELETE FROM detections d
        USING missions m, groups g, outposts o, coordinators c
        WHERE d.mission_uuid = m.uuid
          AND m.group_uuid = g.uuid
          AND g.outpost_uuid = o.uuid
          AND o.created_by = c.uuid
          AND d.uuid = #{uuid, jdbcType=OTHER}
          AND c.cognito_sub = #{cognitoSub}
    """)
    void deleteDetectionAndCoordinator(
            @Param("uuid") UUID uuid,
            @Param("cognitoSub") String cognitoSub);

    @Insert("""
        INSERT INTO detections (
            uuid, mission_uuid, detected_by_drone_uuid,
            object, confidence, false_positive,
            detected_at, location, image_key
        ) VALUES (
            #{uuid, jdbcType=OTHER},
            #{mission_uuid, jdbcType=OTHER},
            #{detected_by_drone_uuid, jdbcType=OTHER},
            #{object},
            #{confidence},
            #{false_positive},
            #{detected_at},
            ST_SetSRID(ST_MakePoint(#{location.x}, #{location.y}), 4326),
            #{image_key}
        )
    """)
    void insertDetection(DbDetection detection);
}