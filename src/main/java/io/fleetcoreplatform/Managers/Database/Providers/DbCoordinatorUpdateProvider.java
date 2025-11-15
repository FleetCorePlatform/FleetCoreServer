package io.fleetcoreplatform.Managers.Database.Providers;

import io.fleetcoreplatform.Models.UpdateCoordinatorModel;
import org.apache.ibatis.builder.annotation.ProviderMethodResolver;

public class DbCoordinatorUpdateProvider implements ProviderMethodResolver {
    public String update(UpdateCoordinatorModel coordinator) {
        StringBuilder query = new StringBuilder("UPDATE coordinators SET ");

        if (coordinator.firstName() != null) {
            query.append("first_name = #{coordinator.firstName}, ");
        }
        if (coordinator.lastName() != null) {
            query.append("last_name = #{coordinator.lastName}, ");
        }

        query.setLength(query.length() - 2);
        query.append(" WHERE uuid = #{uuid, jdbcType=OTHER}");
        return query.toString();
    }
}
