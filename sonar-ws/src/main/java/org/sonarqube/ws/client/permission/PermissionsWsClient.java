package org.sonarqube.ws.client.permission;

import org.sonarqube.ws.WsPermissions;
import org.sonarqube.ws.client.WsClient;

import static org.sonarqube.ws.client.WsRequest.newGetRequest;

public class PermissionsWsClient {
  private final WsClient wsClient;

  public PermissionsWsClient(WsClient wsClient) {
    this.wsClient = wsClient;
  }

  public WsPermissions.WsGroupsResponse groups(WsGroupsRequest request) {
    return wsClient.execute(newGetRequest("api/permissions/groups")
        .setParam("permission", request.getPermission())
        .setParam("projectId", request.getProjectId())
        .setParam("projectKey", request.getProjectKey()),
      WsPermissions.WsGroupsResponse.parser());
  }
}
