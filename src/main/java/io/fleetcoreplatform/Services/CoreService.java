package io.fleetcoreplatform.Services;

import io.fleetcoreplatform.Algorithms.PolygonCoverageAlgorithm;
import io.fleetcoreplatform.Configs.ApplicationConfig;
import io.fleetcoreplatform.Exceptions.GroupNotEmptyException;
import io.fleetcoreplatform.Managers.Cognito.CognitoManager;
import io.fleetcoreplatform.Managers.Database.DatabaseManager;
import io.fleetcoreplatform.Managers.Database.DbModels.DbCoordinator;
import io.fleetcoreplatform.Managers.Database.DbModels.DbDrone;
import io.fleetcoreplatform.Managers.Database.DbModels.DbGroup;
import io.fleetcoreplatform.Managers.Database.DbModels.DbOutpost;
import io.fleetcoreplatform.Managers.Database.Mappers.*;
import io.fleetcoreplatform.Managers.IoTCore.IotDataPlaneManager;
import io.fleetcoreplatform.Managers.IoTCore.IotManager;
import io.fleetcoreplatform.Managers.S3.StorageManager;
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
import java.util.Map;
import java.util.UUID;
import org.jboss.logging.Logger;
import org.postgis.Geometry;
import software.amazon.awssdk.services.iot.model.JobExecution;

@Startup
@ApplicationScoped
public class CoreService {
    @Inject IotManager iotManager;
    @Inject IotDataPlaneManager iotDataPlaneManager;
    @Inject DatabaseManager dbManager;
    @Inject StorageManager storageManager;
    @Inject CognitoManager cognitoManager;
    @Inject ApplicationConfig config;
    @Inject DroneMapper droneMapper;
    @Inject GroupMapper groupMapper;
    @Inject OutpostMapper outpostMapper;
    @Inject MissionMapper missionMapper;
    @Inject CoordinatorMapper coordinatorMapper;
    @Inject Logger logger;

    /**
     * Groups multiple manager operations to register a new drone in IoT Core, and RDS
     *
     * @param group The name of the groups to create the drone in
     * @param droneName Desired name of the drone
     * @param address Public IP address of the drone
     * @param px4Version PX4 firmware version running on the pixhawk hardware
     * @param agentVersion Version of the OnboardAgent client
     */
    public IoTCertContainer registerNewDrone(
            String group, String droneName, String address, String px4Version, String agentVersion)
            throws NotFoundException {
        DbGroup dbGroup = groupMapper.findByName(group);

        if (dbGroup == null) {
            throw new NotFoundException("Group not found with name " + group);
        }

        UUID uuid = UUID.randomUUID();

        IoTCertContainer certContainer = iotManager.generateCertificate();
        iotManager.createThing(droneName, px4Version, agentVersion);

        String policyName = iotManager.createPolicy(droneName);
        iotManager.attachPolicyToCertificate(certContainer.getCertificateARN(), policyName);

        iotManager.attachCertificate(droneName, certContainer.getCertificateARN());

        String groupARN = iotManager.getGroupARN(group);
        iotManager.addDeviceToGroup(droneName, groupARN);

        UUID groupUUID = dbGroup.getUuid();
        Timestamp addedDate = new Timestamp(System.currentTimeMillis());
        droneMapper.insertDrone(uuid, droneName, groupUUID, address, agentVersion, addedDate);

        return certContainer;
    }

    public void updateDrone(UUID droneUuid, DroneRequestModel data) throws NotFoundException {
        DbDrone drone = droneMapper.findByUuid(droneUuid);
        if (drone == null) {
            throw new NotFoundException("Drone not found with UUID " + droneUuid);
        }

        DbGroup currentGroup = groupMapper.findByUuid(drone.getGroup_uuid());

        String currentGroupARN = iotManager.getGroupARN(currentGroup.getName());
        String newGroupARN = iotManager.getGroupARN(data.groupName());

        if (data.address() != null) {
            drone.setAddress(data.address());
        }
        if (data.droneName() != null) {
            drone.setName(data.droneName());
        }
        if (data.agentVersion() != null) {
            drone.setManager_version(data.agentVersion());
        }
        if (data.groupName() != null) {
            DbGroup dbGroup = groupMapper.findByName(data.groupName());
            if (dbGroup == null) {
                throw new NotFoundException("Group not found with name " + data.groupName());
            }
            drone.setGroup_uuid(dbGroup.getUuid());
        }

        droneMapper.updateDrone(droneUuid, drone);

        iotManager.removeThingFromGroup(drone.getName(), currentGroupARN);
        iotManager.addDeviceToGroup(drone.getName(), newGroupARN);
    }

    public void removeDrone(String droneName) throws NotFoundException {
        DbDrone dbDrone = droneMapper.findByName(droneName);
        if (dbDrone == null) {
            throw new NotFoundException("Drone not found with name " + droneName);
        }

        UUID droneUUID = dbDrone.getUuid();

        iotManager.detachCertificates(droneName);
        iotManager.deleteCertificates(droneName);

        String groupARN = iotManager.getThingGroup(droneName);

        if (groupARN != null) {
            iotManager.removeThingFromGroup(droneName, groupARN);
        } else {
            System.err.printf("%s not in a group!", droneName);
        }

        iotManager.removeThing(droneName);
        droneMapper.deleteDrone(droneUUID);
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

        String groupARN = iotManager.getGroupARN(dbGroup.getName());
        iotManager.removeThingFromGroup(dbDrone.getName(), groupARN);

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

        String groupARN = iotManager.getGroupARN(dbGroup.getName());

        droneMapper.addToGroup(droneUUID, dbGroup.getUuid());
        iotManager.addDeviceToGroup(dbDrone.getName(), groupARN);
    }

    public void createNewGroup(String groupName, UUID outpostUuid) throws NotFoundException {
        UUID groupUUID = UUID.randomUUID();
        DbOutpost dbOutpost = outpostMapper.findByUuid(outpostUuid);

        if (dbOutpost == null) {
            throw new NotFoundException("Outpost not found with UUID " + outpostUuid.toString());
        }

        String outpostName = dbOutpost.getName();

        Timestamp createdDate = new Timestamp(System.currentTimeMillis());

        iotManager.createThingGroup(groupName, outpostName);
        groupMapper.insert(groupUUID, dbOutpost.getUuid(), groupName, createdDate);
    }

    public void tryDeleteGroup(String groupName) throws GroupNotEmptyException {
        DbGroup group = groupMapper.findByName(groupName);
        if (group == null) {
            throw new NotFoundException("Group not found with name " + groupName);
        }

        if (!droneMapper.listDronesByGroupUuid(group.getUuid(), 1).isEmpty()) {
            throw new GroupNotEmptyException("Group " + groupName + " is not empty");
        }
        ;
        iotManager.removeThingGroup(groupName);
        groupMapper.deleteByName(groupName);
    }

    public void updateGroup(UUID groupUuid, UpdateGroupOutpostModel data) throws NotFoundException {
        DbGroup group = groupMapper.findByUuid(groupUuid);
        DbOutpost outpost = outpostMapper.findByUuid(data.outpost_uuid());

        if (group == null) {
            throw new NotFoundException("Group not found with UUID " + groupUuid.toString());
        }
        if (outpost == null) {
            throw new NotFoundException("Outpost not found with UUID" + data.outpost_uuid());
        }

        groupMapper.updateGroupOutpost(groupUuid, data.outpost_uuid());
        iotManager.updateThingGroupOutpost(group.getName(), outpost.getName());
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
     * @param outpost The name of the outpost the groups is in
     * @param group Name of the group of drones
     * @param coordinatorUUID The UUID of the coordinator performing this operation
     * @throws IOException If there was an error generating the mission bundle
     * @throws NotFoundException If the outpost, or group doesn't exist
     */
    public UUID createNewMission(String outpost, String group, UUID coordinatorUUID)
            throws IOException, NotFoundException {
        Timestamp startedAt = new Timestamp(System.currentTimeMillis());

        String s3Timestamp =
                startedAt
                        .toLocalDateTime()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSS"));

        String missionPath = "missions/" + outpost + "/" + group + "/mission-" + s3Timestamp;

        Geometry area = dbManager.getOutpostGeometry(outpost);
        if (area == null) {
            throw new NotFoundException("Outpost cannot be found with name " + outpost);
        }

        File missionBundle = PolygonCoverageAlgorithm.calculateSinge(area);
        String key = storageManager.uploadMissionBundle(missionPath, missionBundle);

        String presignedUrl = storageManager.getPresignedObjectUrl(key);

        String groupARN = iotManager.getGroupARN(group);

        String iotMissionName = outpost + "-" + group + "_" + s3Timestamp;

        // Create 'Download-File' mission -> download mission file from url and execute mission from
        // it
        iotManager.createIoTJob(
                iotMissionName,
                groupARN,
                config.iot().newMissionJobArn(),
                Map.ofEntries(
                        Map.entry("downloadUrl", presignedUrl),
                        Map.entry("filePath", "/tmp/missions/")));

        DbGroup dbgroup = groupMapper.findByName(group);
        if (dbgroup == null) {
            throw new NotFoundException("Group not found with name " + group);
        }

        UUID groupUUID = dbgroup.getUuid();

        String bundleUrl = storageManager.getInternalObjectUrl(key);

        UUID missionUuid = UUID.randomUUID();
        missionMapper.insert(
                missionUuid, groupUUID, iotMissionName, bundleUrl, startedAt, coordinatorUUID);

        return missionUuid;
    }

    public DroneExecutionStatusResponseModel getMissionStatus(UUID droneUuid)
            throws NotFoundException {
        DbDrone drone = droneMapper.findByUuid(droneUuid);
        if (drone == null) {
            throw new NotFoundException("Drone not found with UUID " + droneUuid.toString());
        }

        String thingName = drone.getName();

        String lastJopId = iotManager.getLastJobId(thingName);
        if (lastJopId == null) {
            return null;
        }

        JobExecution jobStatus = iotManager.getJobExecutionStatus(lastJopId, thingName);

        return new DroneExecutionStatusResponseModel(
                droneUuid, jobStatus.status(), jobStatus.startedAt());
    }
}
