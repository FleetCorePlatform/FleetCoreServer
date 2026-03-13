package io.fleetcoreplatform.Services;

import io.fleetcoreplatform.Configs.ApplicationConfig;
import io.fleetcoreplatform.Exceptions.*;
import io.fleetcoreplatform.Managers.IoTCore.Enums.MissionDocumentEnums;
import io.fleetcoreplatform.Managers.Kinesis.KinesisVideoManager;
import io.fleetcoreplatform.Models.DroneIdentity;
import io.fleetcoreplatform.Managers.Cognito.CognitoManager;
import io.fleetcoreplatform.Managers.Database.DbModels.*;
import io.fleetcoreplatform.Managers.Database.Mappers.*;
import io.fleetcoreplatform.Managers.IoTCore.IotDataPlaneManager;
import io.fleetcoreplatform.Managers.IoTCore.IotManager;
import io.fleetcoreplatform.Managers.S3.StorageManager;
import io.fleetcoreplatform.MissionPlanner;
import io.fleetcoreplatform.Models.*;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletionException;

import org.jboss.logging.Logger;
import org.postgis.Geometry;
import software.amazon.awssdk.services.iot.model.CertificateStatus;
import software.amazon.awssdk.services.iot.model.Job;
import software.amazon.awssdk.services.iot.model.JobExecutionSummary;

@Startup
@ApplicationScoped
public class CoreService {
    @Inject IotManager iotManager;
    @Inject IotDataPlaneManager iotDataPlaneManager;
    @Inject StorageManager storageManager;
    @Inject CognitoManager cognitoManager;
    @Inject ApplicationConfig config;
    @Inject DroneMapper droneMapper;
    @Inject GroupMapper groupMapper;
    @Inject OutpostMapper outpostMapper;
    @Inject MissionMapper missionMapper;
    @Inject CoordinatorMapper coordinatorMapper;
    @Inject KinesisVideoManager kinesisVideoManager;
    @Inject Logger logger;

    /**
     * Groups multiple manager operations to register a new drone in IoT Core, and RDS
     *
     * @param group The name of the groups to create the drone in
     * @param droneName Desired name of the drone
     * @param address Public IP address of the drone
     * @param agentVersion Version of the OnboardAgent client
     */
    public RegisteredDroneResponse registerNewDrone(
            UUID group,
            String droneName,
            String address,
            String agentVersion,
            DroneHomePositionModel homePosition,
            String model,
            List<String> capabilities)
            throws NotFoundException, GroupHasNoOutpostException, CannotUpdateCertificate, KinesisCannotCreateChannelException {
        DbGroup dbGroup = groupMapper.findByUuid(group);

        if (dbGroup == null) {
            throw new NotFoundException("Group not found with name " + group);
        }

        UUID uuid = UUID.randomUUID();
        String thingNameUuid = uuid.toString();

        var attributes = iotManager.getGroupAttributes(group.toString());

        if (attributes == null || attributes.get("outpost") == null) {
            throw new GroupHasNoOutpostException("Target group has no outpost attribute");
        }

        var createdCheck = kinesisVideoManager.createSignalingChannel(uuid);
        if (createdCheck == null) {
            throw new KinesisCannotCreateChannelException("Cannot create kinesis video signaling channel for drone");
        }

        IoTCertContainer certContainer = iotManager.generateCertificate();
        try {
            iotManager.updateCertificate(certContainer.certificateARN(), CertificateStatus.ACTIVE);
        } catch (CompletionException ex) {
            throw new CannotUpdateCertificate(ex.getMessage());
        }

        iotManager.createThing(thingNameUuid, attributes.get("outpost"), group.toString());
        String policyName = iotManager.createPolicy(thingNameUuid);


        iotManager.attachPolicyToCertificate(certContainer.certificateARN(), policyName);

        iotManager.attachCertificate(thingNameUuid, certContainer.certificateARN());

        String groupARN = iotManager.getGroupARN(group.toString());
        iotManager.addDeviceToGroup(thingNameUuid, groupARN);


        UUID groupUUID = dbGroup.getUuid();
        Timestamp addedDate = new Timestamp(System.currentTimeMillis());

        // Signaling channel name (UUID) is same as the thing name (UUID) so the drone know to access its "own" signaling channel
        droneMapper.insertDrone(
                uuid, droneName, groupUUID, address, agentVersion, addedDate, homePosition, model, capabilities, uuid);

        return new RegisteredDroneResponse(thingNameUuid, certContainer);
    }

    public void updateDrone(UUID droneUuid, UpdateDroneModel data) throws NotFoundException {
        DbDrone drone = droneMapper.findByUuid(droneUuid);
        if (drone == null) {
            throw new NotFoundException("Drone not found with UUID " + droneUuid);
        }

        if (data.address() != null) {
            drone.setAddress(data.address());
        }
        if (data.droneName() != null) {
            drone.setName(data.droneName());
        }
        if (data.agentVersion() != null) {
            drone.setManager_version(data.agentVersion());
        }
        if (data.homePosition() != null) {
            drone.setHome_position(
                    new org.postgis.Point(
                            data.homePosition().x(),
                            data.homePosition().y(),
                            data.homePosition().z()));
        }

        droneMapper.updateDrone(droneUuid, drone);
    }

    public void removeDrone(UUID droneUuid) throws NotFoundException {
        DbDrone dbDrone = droneMapper.findByUuid(droneUuid);
        if (dbDrone == null) {
            throw new NotFoundException("Drone not found with UUID " + droneUuid);
        }

        String thingName = dbDrone.getUuid().toString();

        iotManager.detachCertificates(thingName);
        iotManager.deleteCertificates(thingName);

        String groupARN = iotManager.getThingGroup(thingName);

        if (groupARN != null) {
            iotManager.removeThingFromGroup(thingName, groupARN);
        } else {
            System.err.printf("%s not in a group!", thingName);
        }

        iotManager.removeThing(thingName);
        droneMapper.deleteDrone(droneUuid);

        kinesisVideoManager.deleteSignalingChannel(droneUuid);
    }

    public void removeDroneFromGroup(UUID droneUUID) throws NotFoundException {
        DbDrone dbDrone = droneMapper.findByUuid(droneUUID);
        if (dbDrone == null) {
            throw new NotFoundException("Drone not found with UUID " + droneUUID);
        }
        DbGroup dbGroup = groupMapper.findByUuid(dbDrone.getGroup_uuid());
        if (dbGroup == null) {
            return;
        }

        String groupARN = iotManager.getGroupARN(dbGroup.getUuid().toString());
        iotManager.removeThingFromGroup(dbDrone.getUuid().toString(), groupARN);

        droneMapper.ungroupDrone(droneUUID);
    }

    public void addDroneToGroup(UUID droneUUID, UUID groupUUID) throws NotFoundException {
        DbDrone dbDrone = droneMapper.findByUuid(droneUUID);
        if (dbDrone == null) {
            throw new NotFoundException("Drone not found with UUID " + droneUUID);
        }
        DbGroup dbGroup = groupMapper.findByUuid(groupUUID);
        if (dbGroup == null) {
            throw new NotFoundException("Group not found with UUID " + groupUUID);
        }

        String groupARN = iotManager.getGroupARN(dbGroup.getUuid().toString());

        droneMapper.addToGroup(droneUUID, dbGroup.getUuid());
        iotManager.addDeviceToGroup(dbDrone.getUuid().toString(), groupARN);
    }

    public void createNewGroup(String groupName, UUID outpostUuid) throws NotFoundException {
        UUID groupUUID = UUID.randomUUID();
        DbOutpost dbOutpost = outpostMapper.findByUuid(outpostUuid);

        if (dbOutpost == null) {
            throw new NotFoundException("Outpost not found with UUID " + outpostUuid.toString());
        }

        Timestamp createdDate = new Timestamp(System.currentTimeMillis());

        iotManager.createThingGroup(groupUUID.toString(), outpostUuid.toString());
        groupMapper.insert(groupUUID, dbOutpost.getUuid(), groupName, createdDate);
    }

    public void tryDeleteGroup(UUID groupUuid) throws GroupNotEmptyException {
        DbGroup group = groupMapper.findByUuid(groupUuid);
        if (group == null) {
            throw new NotFoundException("Group not found with UUID " + groupUuid.toString());
        }

        if (!droneMapper.listDronesByGroupUuid(group.getUuid(), 1).isEmpty()) {
            throw new GroupNotEmptyException("Group " + group.getName() + " is not empty");
        }
        ;
        iotManager.removeThingGroup(group.getUuid().toString());
        groupMapper.deleteByUuid(groupUuid);
    }

    public CognitoCreatedResponseModel registerNewCoordinator(
            String email, String firstName, String lastName) {
        CognitoCreatedResponseModel cognitoUser =
                cognitoManager.createUser(email, firstName, lastName);

        if (cognitoUser == null) {
            return null;
        }

        UUID uuid = UUID.randomUUID();
        Timestamp timestamp = Timestamp.from(Instant.now());

        coordinatorMapper.insert(
                uuid, cognitoUser.cognito_sub(), firstName, lastName, email, timestamp);

        return cognitoUser;
    }

    public void updateCoordinator(
            UUID coordinatorUuid, UpdateCoordinatorModel updateCoordinatorModel)
            throws NotFoundException {
        DbCoordinator coordinator = coordinatorMapper.findByUuid(coordinatorUuid);
        if (coordinator == null) {
            throw new NotFoundException(
                    "Coordinator not found with UUID " + coordinatorUuid.toString());
        }

        cognitoManager.updateUser(
                coordinator.getEmail(),
                updateCoordinatorModel.firstName(),
                updateCoordinatorModel.lastName());

        try {
            coordinatorMapper.update(coordinatorUuid, updateCoordinatorModel);
        } catch (Exception ex) {
            logger.errorf("Error updating coordinator %s", ex.getMessage());
        }
    }

    /**
     * Coordinates multiple managers to create an IoT Core mission for a group of drones
     *
     * @param outpostUuid The UUID of the outpost the groups is in
     * @param groupUUID UUID of the group
     * @param coordinatorUUID The UUID of the coordinator performing this operation
     * @throws IOException If there was an error generating the mission bundle
     * @throws NotFoundException If the outpost, or group doesn't exist
     */
    public UUID createGroupMission(
            UUID outpostUuid, UUID groupUUID, List<UUID> droneUuids, UUID coordinatorUUID, Integer altitude, String jobName, String scheduled)
            throws IOException, NotFoundException {
        DbGroup dbgroup = groupMapper.findByUuid(groupUUID);
        if (dbgroup == null) {
            throw new NotFoundException("Group not found with UUID " + groupUUID.toString());
        }

        DbOutpost dbOutpost = outpostMapper.findByUuid(outpostUuid);
        if (dbOutpost == null) {
            throw new NotFoundException("Outpost cannot be found with name " + outpostUuid);
        }

        UUID missionUuid = UUID.randomUUID();

        String group = dbgroup.getName();

        Timestamp startedAt = new Timestamp(System.currentTimeMillis());

        String s3Timestamp =
                startedAt
                        .toLocalDateTime()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSS"));

        String missionPath = "missions/" + outpostUuid + "/" + group + "/mission-" + s3Timestamp;

        String outpostName = dbOutpost.getName();

        Geometry area = dbOutpost.getArea().toGeometry();

        List<DbDrone> allDrones = droneMapper.listNoMaintenanceDronesByGroupUuid(groupUUID, 100);

        List<DbDrone> selectedDrones = (droneUuids == null || droneUuids.isEmpty())
            ? allDrones
            : allDrones.stream().filter(d -> droneUuids.contains(d.getUuid())).toList();

        if (selectedDrones.isEmpty()) {
            throw new NotFoundException("No available drones found for the specified selection.");
        }

        ArrayList<DroneIdentity> droneIdentities = getDroneIdentities(groupUUID, selectedDrones);

        int missionAltitude =
                Objects.requireNonNullElseGet(
                        altitude,
                        () ->
                                Math.toIntExact(
                                        Math.round(
                                                        selectedDrones.stream()
                                                                .findFirst()
                                                                .get()
                                                                .getHome_position()
                                                                .z())
                                                + 25));

        File missionBundle =
                MissionPlanner.buildMission(
                        area, droneIdentities.toArray(new DroneIdentity[0]), missionAltitude);
        String key = storageManager.uploadMissionBundle(missionPath, missionBundle);

        String downloadUrl = storageManager.getPresignedObjectUrl(key, 30);

        String groupARN = iotManager.getGroupARN(dbgroup.getUuid().toString());

        String iotMissionName = missionUuid.toString();

        if (droneUuids == null || droneUuids.isEmpty()) {
            // Create 'Download-File' mission -> download mission file from url and execute mission from it
            iotManager.createIoTJob(
                    new GroupTarget(groupARN),
                    MissionDocumentEnums.DOWNLOAD,
                    iotMissionName,
                    downloadUrl,
                    "/tmp/missions/",
                    outpostName,
                    dbgroup.getUuid().toString(),
                    config.s3().bucketName(),
                    scheduled);
        } else {
            List<String> targetArns = selectedDrones.stream().map(d -> iotManager.getThingARN(d.getUuid().toString())).toList();
            iotManager.createIoTJob(
                    targetArns,
                    MissionDocumentEnums.DOWNLOAD,
                    iotMissionName,
                    downloadUrl,
                    "/tmp/missions/",
                    outpostName,
                    dbgroup.getUuid().toString(),
                    config.s3().bucketName(),
                    scheduled);
        }

        String bundleUrl = storageManager.getInternalObjectUrl(key);

        missionMapper.insert(
                missionUuid, groupUUID, jobName, bundleUrl, startedAt, coordinatorUUID);

        for (DbDrone drone : selectedDrones) {
            missionMapper.insertMissionDrone(missionUuid, drone.getUuid());
        }

        return missionUuid;
    }

    public UUID createSoloMission(
            PolygonPoint2D[] waypoints, UUID droneUuid, UUID coordinatorUUID, Integer altitude, String jobName, int speed, boolean returnToLaunch, String scheduled)
            throws IOException, NotFoundException {
        DbDrone dbDrone = droneMapper.findByUuid(droneUuid);
        if (dbDrone == null) {
            throw new NotFoundException("Drone not found with UUID " + droneUuid.toString());
        }

        UUID missionUuid = UUID.randomUUID();
        String thingName = dbDrone.getUuid().toString();
        Timestamp startedAt = new Timestamp(System.currentTimeMillis());

        String s3Timestamp =
                startedAt
                        .toLocalDateTime()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSS"));

        String missionPath = "missions/solo/" + thingName + "/mission-" + s3Timestamp;

        int missionAltitude = altitude != null ? altitude : Math.toIntExact(
                Math.round(dbDrone.getHome_position().z()) + 25);

        DroneIdentity droneIdentity = getDroneIdentity(droneUuid);

        File missionBundle = MissionPlanner.buildManualMission(waypoints, droneIdentity, missionAltitude, speed, returnToLaunch);
        String key = storageManager.uploadMissionBundle(missionPath, missionBundle);
        String downloadUrl = storageManager.getPresignedObjectUrl(key, 30);
        String droneARN = iotManager.getThingARN(thingName);
        String iotMissionName = missionUuid.toString();

        DbGroup dbGroup = groupMapper.findByUuid(dbDrone.getGroup_uuid());
        DbOutpost dbOutpost = outpostMapper.findByUuid(dbGroup.getOutpost_uuid());

        iotManager.createIoTJob(
                new DroneTarget(droneARN),
                MissionDocumentEnums.DOWNLOAD,
                iotMissionName,
                downloadUrl,
                "/tmp/missions/",
                dbOutpost.getName(),
                config.s3().bucketName(),
                scheduled
        );

        String bundleUrl = storageManager.getInternalObjectUrl(key);

        missionMapper.insert(
                missionUuid, null, jobName, bundleUrl, startedAt, coordinatorUUID);
        missionMapper.insertMissionDrone(missionUuid, droneUuid);

        return missionUuid;
    }

    public void cancelJob(MissionCancellationContext context) {
        DbOutpost dbOutpost = outpostMapper.findByUuid(context.outpostUuid());
        if (dbOutpost == null) {
            throw new NotFoundException("Outpost cannot be found with UUID " + context.outpostUuid());
        }

        String outpostName = dbOutpost.getName();
        String iotMissionName = String.format("%s-canceljob", context.missionUuid().toString());

        if (context.droneUuid() != null) {
            DbDrone dbDrone = droneMapper.findByUuid(context.droneUuid());
            if (dbDrone == null) {
                throw new NotFoundException("Drone not found with UUID " + context.droneUuid());
            }
            String droneARN = iotManager.getThingARN(dbDrone.getUuid().toString());

            iotManager.createIoTJob(
                    new DroneTarget(droneARN),
                    MissionDocumentEnums.CANCEL,
                    iotMissionName,
                    "",
                    "",
                    outpostName,
                    "",
                    null);
        } else if (context.groupUuid() != null) {
            DbGroup dbgroup = groupMapper.findByUuid(context.groupUuid());
            if (dbgroup == null) {
                throw new NotFoundException("Group not found with UUID " + context.groupUuid());
            }
            String group = dbgroup.getName();
            String groupARN = iotManager.getGroupARN(dbgroup.getUuid().toString());

            iotManager.createIoTJob(
                    new GroupTarget(groupARN),
                    MissionDocumentEnums.CANCEL,
                    iotMissionName,
                    "",
                    "",
                    outpostName,
                    dbgroup.getUuid().toString(),
                    "",
                    null);
        } else {
            throw new IllegalStateException("Mission context contains neither a group nor a drone target.");
        }
    }

    private ArrayList<DroneIdentity> getDroneIdentities(
            UUID groupUUID, List<DbDrone> drones) {
        ArrayList<DroneIdentity> droneIdentities = new ArrayList<>();

        if (drones.isEmpty()) {
            throw new NotFoundException(
                    "No drones found in group with UUID" + groupUUID.toString());
        }

        for (DbDrone drone : drones) {
            DroneHomePositionModel home = drone.getHome_position();

            droneIdentities.add(
                    new DroneIdentity(
                            drone.getUuid().toString(),
                            new DroneIdentity.Home(home.x(), home.y(), home.z())));
        }
        return droneIdentities;
    }

    private DroneIdentity getDroneIdentity(UUID droneUuid) {
        DbDrone drone = droneMapper.findByUuid(droneUuid);
        if (drone == null) {
            throw new NotFoundException("No drone found with UUID" + droneUuid.toString());
        }

        DroneHomePositionModel home = drone.getHome_position();

        return new DroneIdentity(
                drone.getUuid().toString(),
                new DroneIdentity.Home(home.x(), home.y(), home.z())
        );
    }

    public DroneExecutionStatusResponseModel getThingMissionStatus(
            UUID droneUuid, UUID missionUuid, String sub) throws NotFoundException {
        DbMission mission = missionMapper.findByIdAndCoordinator(missionUuid, sub);
        if (mission == null) {
            throw new NotFoundException("Mission not found with UUID " + missionUuid.toString());
        }

        DbDrone drone = droneMapper.findByUuid(droneUuid);
        if (drone == null) {
            throw new NotFoundException("Drone not found with UUID " + droneUuid.toString());
        }

        String thingName = drone.getUuid().toString();

        JobExecutionSummary executionSummary = iotManager.getThingJob(thingName, mission.getName());
        if (executionSummary == null) {
            return null;
        }

        return new DroneExecutionStatusResponseModel(
                droneUuid, executionSummary.status(), Timestamp.from(executionSummary.lastUpdatedAt()));
    }

    public MissionExecutionStatusModel getMissionStatus(UUID missionUuid, String sub)
            throws NotFoundException {
        DbMission mission = missionMapper.findByIdAndCoordinator(missionUuid, sub);
        if (mission == null) {
            throw new NotFoundException("Mission not found with UUID " + missionUuid.toString());
        }

        Job job = iotManager.getJob(mission.getUuid().toString());

        return new MissionExecutionStatusModel(job.status(), mission.getStart_time(), job.completedAt() != null ? Timestamp.from(job.completedAt()) : null, job.schedulingConfig().startTime() != null ? job.schedulingConfig().startTime() : null);
    }

    public void deleteOutpost(UUID outpostUuid)
        throws OutpostNotEmptyException{
        int count = outpostMapper.getDroneCountInOutpost(outpostUuid);
        if (count > 0) {
            throw new OutpostNotEmptyException("Outpost is not empty");
        }

        int rowsAffected = outpostMapper.delete(outpostUuid);
        if (rowsAffected == 0) {
            logger.error(String.format("Error while deleting outpost %s", outpostUuid.toString()));
        }
    }
}
