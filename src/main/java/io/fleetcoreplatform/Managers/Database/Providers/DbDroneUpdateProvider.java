package io.fleetcoreplatform.Managers.Database.Providers;

import io.fleetcoreplatform.Managers.Database.DbModels.DbDrone;
import org.apache.ibatis.builder.annotation.ProviderMethodResolver;

public class DbDroneUpdateProvider implements ProviderMethodResolver {
    public String update(DbDrone drone) {
        StringBuilder query = new StringBuilder("UPDATE drones SET ");

        if (drone.getName() != null) {
            query.append("name = #{drone.name}, ");
        }
        if (drone.getAddress() != null) {
            query.append("address = #{drone.address}, ");
        }
        if (drone.getManager_version() != null) {
            query.append("manager_version = #{drone.manager_version}, ");
        }
        if (drone.getGroup_uuid() != null) {
            query.append("group_uuid = #{drone.group_uuid, jdbcType=OTHER}, ");
        }
        if (drone.getHome_position() != null) {
            query.append("home_position = st_pointz(#{drone.home_position.x}, #{drone.home_position.y}, #{drone.home_position.z}), ");
        }
        if (drone.getCapabilities() != null) {
            query.append("capabilities = #{drone.capabilities, typeHandler=io.fleetcoreplatform.Managers.Database.TypeHandlers.StringArrayTypeHandler}, ");
        }

        query.setLength(query.length() - 2);
        query.append(" WHERE uuid = #{uuid, jdbcType=OTHER}");
        return query.toString();
    }
}
