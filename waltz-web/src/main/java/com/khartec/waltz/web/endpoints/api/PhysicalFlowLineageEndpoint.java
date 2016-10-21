package com.khartec.waltz.web.endpoints.api;

import com.khartec.waltz.model.EntityKind;
import com.khartec.waltz.model.EntityReference;
import com.khartec.waltz.model.LastUpdate;
import com.khartec.waltz.model.Severity;
import com.khartec.waltz.model.changelog.ChangeLog;
import com.khartec.waltz.model.changelog.ImmutableChangeLog;
import com.khartec.waltz.model.command.CommandResponse;
import com.khartec.waltz.model.physical_flow_lineage.*;
import com.khartec.waltz.model.user.Role;
import com.khartec.waltz.service.changelog.ChangeLogService;
import com.khartec.waltz.service.physical_flow_lineage.PhysicalFlowLineageService;
import com.khartec.waltz.service.user.UserRoleService;
import com.khartec.waltz.web.ListRoute;
import com.khartec.waltz.web.endpoints.Endpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import spark.Request;
import spark.Response;

import java.io.IOException;

import static com.khartec.waltz.common.Checks.checkNotNull;
import static com.khartec.waltz.web.WebUtilities.*;
import static com.khartec.waltz.web.endpoints.EndpointUtilities.*;

@Service
public class PhysicalFlowLineageEndpoint implements Endpoint {

    private static final String BASE_URL = mkPath("api", "physical-flow-lineage");


    private final ChangeLogService changeLogService;
    private final PhysicalFlowLineageService physicalFlowLineageService;
    private final UserRoleService userRoleService;


    @Autowired
    public PhysicalFlowLineageEndpoint(ChangeLogService changeLogService,
                                       PhysicalFlowLineageService physicalFlowLineageService,
                                       UserRoleService userRoleService) {
        checkNotNull(changeLogService, "changeLogService cannot be null");
        checkNotNull(physicalFlowLineageService, "physicalFlowLineageService cannot be null");
        checkNotNull(userRoleService, "userRoleService cannot be null");

        this.changeLogService = changeLogService;
        this.physicalFlowLineageService = physicalFlowLineageService;
        this.userRoleService = userRoleService;
    }


    @Override
    public void register() {
        String findByPhysicalFlowIdPath = mkPath(
                BASE_URL,
                "physical-flow",
                ":id");

        String findContributionsByPhysicalFlowIdPath = mkPath(
                BASE_URL,
                "physical-flow",
                ":id",
                "contributions");

        String removeContributionPath = mkPath(
                BASE_URL,
                "physical-flow",
                ":describedFlowId",
                "contributions",
                ":contributorFlowId");

        String addContributionPath = mkPath(
                BASE_URL,
                "physical-flow",
                ":describedFlowId",
                "contributions");

        ListRoute<PhysicalFlowLineage> findContributionsByPhysicalFlowIdRoute =
                (request, response)
                        -> physicalFlowLineageService.findContributionsByPhysicalFlowId(getId(request));

        ListRoute<PhysicalFlowLineage> findByPhysicalFlowIdRoute =
                (request, response)
                        -> physicalFlowLineageService.findByPhysicalFlowId(getId(request));

        getForList(findByPhysicalFlowIdPath, findByPhysicalFlowIdRoute);
        getForList(findContributionsByPhysicalFlowIdPath, findContributionsByPhysicalFlowIdRoute);

        deleteForDatum(removeContributionPath, this::removeContribution);
        putForDatum(addContributionPath, this::addContribution);
    }


    private CommandResponse<PhysicalFlowLineageRemoveCommand> removeContribution(Request request, Response response) {
        requireRole(userRoleService, request, Role.LINEAGE_EDITOR);

        PhysicalFlowLineageRemoveCommand removeCommand = ImmutablePhysicalFlowLineageRemoveCommand.builder()
                .describedFlowId(getLong(request,"describedFlowId"))
                .contributingFlowId(getLong(request, "contributorFlowId"))
                .build();

        logChange(getUsername(request),
                EntityReference.mkRef(EntityKind.PHYSICAL_FLOW, removeCommand.describedFlowId()),
                String.format("Removing contribution of flow: %s from lineage", removeCommand.contributingFlowId()));

        return physicalFlowLineageService.removeContribution(removeCommand);
    }


    private CommandResponse<PhysicalFlowLineageAddCommand> addContribution(Request request, Response response) throws IOException {
        requireRole(userRoleService, request, Role.LINEAGE_EDITOR);

        Long contributingFlowId = readBody(request, Long.class);

        String username = getUsername(request);

        PhysicalFlowLineageAddCommand addCommand = ImmutablePhysicalFlowLineageAddCommand.builder()
                .describedFlowId(getLong(request,"describedFlowId"))
                .contributingFlowId(contributingFlowId)
                .lastUpdate(LastUpdate.mkForUser(username))
                .build();

        logChange(username,
                EntityReference.mkRef(EntityKind.PHYSICAL_FLOW, addCommand.describedFlowId()),
                String.format("Adding contribution of flow: %s to lineage", addCommand.contributingFlowId()));

        return physicalFlowLineageService.addContribution(addCommand);
    }


    private void logChange(String userId,
                           EntityReference ref,
                           String message) {
        ChangeLog logEntry = ImmutableChangeLog.builder()
                .parentReference(ref)
                .message(message)
                .severity(Severity.INFORMATION)
                .userId(userId)
                .build();

        changeLogService.write(logEntry);
    }
}
