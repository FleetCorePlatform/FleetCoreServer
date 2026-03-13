package io.fleetcoreplatform.Managers.Database.Mappers;

import io.fleetcoreplatform.Managers.Database.DbModels.DbMission;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import io.fleetcoreplatform.Models.MissionCancellationContext;
import io.fleetcoreplatform.Models.MissionSummary;
import io.fleetcoreplatform.Models.SoloMissionSummary;
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

    @Insert(
            "INSERT INTO mission_drones (mission_uuid, drone_uuid)"
                    + " VALUES (#{mission_uuid, jdbcType=OTHER}, #{drone_uuid, jdbcType=OTHER})")
    void insertMissionDrone(
            @Param("mission_uuid") UUID mission_uuid,
            @Param("drone_uuid") UUID drone_uuid);

    @Select("SELECT * FROM missions WHERE uuid = #{uuid, jdbcType=OTHER}")
    DbMission findById(@Param("uuid") UUID uuid);

    @Update(
            "UPDATE missions SET group_uuid = #{group_uuid, jdbcType=OTHER}, name = #{name},"
                    + " bundle_url = #{bundle_url}, start_time = #{start_time}, created_by ="
                    + " #{created_by, jdbcType=OTHER} WHERE uuid = #{uuid, jdbcType=OTHER}")
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
        LEFT JOIN groups g ON m.group_uuid = g.uuid
        LEFT JOIN outposts o ON g.outpost_uuid = o.uuid
        INNER JOIN coordinators c ON m.created_by = c.uuid
        WHERE o.uuid = #{outpostUuid, jdbcType=OTHER}
          AND c.cognito_sub = #{cognitoSub}
    """)
    List<DbMission> listMissionsByCoordinatorAndOutpost(String cognitoSub, UUID outpostUuid);

    @Select("""
        SELECT
            m.name,
            m.uuid AS missionUuid,
            m.start_time AS startTime,
            COUNT(d.uuid) AS detectionCount
        FROM missions m
        INNER JOIN groups g ON m.group_uuid = g.uuid
        INNER JOIN outposts o ON g.outpost_uuid = o.uuid
        INNER JOIN coordinators c ON m.created_by = c.uuid
        LEFT JOIN detections d ON m.uuid = d.mission_uuid
        WHERE
            g.uuid = #{groupUuid, jdbcType=OTHER}
            AND c.cognito_sub = #{cognitoSub}
        GROUP BY m.uuid, m.name, m.start_time
    """)
    List<MissionSummary> selectMissionSummariesByGroupAndCoordinator(
        @Param("groupUuid") UUID groupUuid,
        @Param("cognitoSub") String cognitoSub
    );

    @Select("""
        SELECT
            m.name,
            m.uuid AS missionUuid,
            m.start_time AS startTime,
            COUNT(det.uuid) AS detectionCount,
            d.uuid AS droneUuid,
            d.name AS droneName
        FROM missions m
        INNER JOIN mission_drones md ON m.uuid = md.mission_uuid
        INNER JOIN drones d ON md.drone_uuid = d.uuid
        INNER JOIN groups g ON d.group_uuid = g.uuid
        INNER JOIN outposts o ON g.outpost_uuid = o.uuid
        INNER JOIN coordinators c ON m.created_by = c.uuid
        LEFT JOIN detections det ON m.uuid = det.mission_uuid
        WHERE
            o.uuid = #{outpostUuid, jdbcType=OTHER}
            AND c.cognito_sub = #{cognitoSub}
            AND m.group_uuid IS NULL
        GROUP BY m.uuid, m.name, m.start_time, d.uuid, d.name
    """)
    List<SoloMissionSummary> selectSoloMissionSummariesByOutpostAndCoordinator(
        @Param("outpostUuid") UUID outpostUuid,
        @Param("cognitoSub") String cognitoSub
    );

    @Select("""
        SELECT
            m.uuid as missionUuid,
            m.group_uuid as groupUuid,
            CASE WHEN m.group_uuid IS NULL THEN md.drone_uuid ELSE NULL END as droneUuid,
            COALESCE(o1.uuid, o2.uuid) as outpostUuid
        FROM missions m
        LEFT JOIN groups g1 ON m.group_uuid = g1.uuid
        LEFT JOIN outposts o1 ON g1.outpost_uuid = o1.uuid
        LEFT JOIN mission_drones md ON m.uuid = md.mission_uuid AND m.group_uuid IS NULL
        LEFT JOIN drones d ON md.drone_uuid = d.uuid
        LEFT JOIN groups g2 ON d.group_uuid = g2.uuid
        LEFT JOIN outposts o2 ON g2.outpost_uuid = o2.uuid
        INNER JOIN coordinators c ON m.created_by = c.uuid
        WHERE m.uuid = #{missionUuid, jdbcType=OTHER}
          AND c.cognito_sub = #{cognitoSub}
        LIMIT 1
    """)
    MissionCancellationContext findCancellationContext(
            @Param("missionUuid") UUID missionUuid,
            @Param("cognitoSub") String cognitoSub
    );

    @Select("""
        SELECT
            m.name,
            m.uuid AS missionUuid,
            m.start_time AS startTime,
            COUNT(d.uuid) AS detectionCount
        FROM missions m
        LEFT JOIN groups g ON m.group_uuid = g.uuid
        LEFT JOIN outposts o ON g.outpost_uuid = o.uuid
        INNER JOIN coordinators c ON m.created_by = c.uuid
        LEFT JOIN detections d ON m.uuid = d.mission_uuid
        WHERE
            c.cognito_sub = #{cognitoSub}
        GROUP BY m.uuid, m.name, m.start_time
        ORDER BY m.start_time DESC
        LIMIT #{count}
    """)
    List<MissionSummary> selectLatestMissionSummariesByCoordinator(
        @Param("cognitoSub") String cognitoSub,
        @Param("count") Integer count
    );

    @Select("""
        SELECT COUNT(*)
        FROM missions m
        INNER JOIN coordinators c ON m.created_by = c.uuid
        WHERE c.cognito_sub = #{cognitoSub}
    """)
    int countMissionsByCoordinator(String cognitoSub);


    @Select("""
        SELECT
            m.name,
            m.uuid AS missionUuid,
            m.start_time AS startTime,
            COUNT(det.uuid) AS detectionCount,
            d.uuid AS droneUuid,
            d.name AS droneName
        FROM missions m
        INNER JOIN mission_drones md ON m.uuid = md.mission_uuid
        INNER JOIN drones d ON md.drone_uuid = d.uuid
        INNER JOIN groups g ON d.group_uuid = g.uuid
        INNER JOIN outposts o ON g.outpost_uuid = o.uuid
        INNER JOIN coordinators c ON m.created_by = c.uuid
        LEFT JOIN detections det ON m.uuid = det.mission_uuid
        WHERE
            g.uuid = #{groupUuid, jdbcType=OTHER}
            AND c.cognito_sub = #{cognitoSub}
        GROUP BY m.uuid, m.name, m.start_time, d.uuid, d.name
    """)
    List<SoloMissionSummary> selectSoloMissionSummariesByGroupAndCoordinator(
        @Param("groupUuid") UUID groupUuid,
        @Param("cognitoSub") String cognitoSub
    );
}