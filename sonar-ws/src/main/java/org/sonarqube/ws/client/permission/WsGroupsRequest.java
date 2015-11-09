package org.sonarqube.ws.client.permission;

public class WsGroupsRequest {
  private String permission;
  private String projectId;
  private String projectKey;

  public String getPermission() {
    return permission;
  }

  public WsGroupsRequest setPermission(String permission) {
    this.permission = permission;
    return this;
  }

  public String getProjectId() {
    return projectId;
  }

  public WsGroupsRequest setProjectId(String projectId) {
    this.projectId = projectId;
    return this;
  }

  public String getProjectKey() {
    return projectKey;
  }

  public WsGroupsRequest setProjectKey(String projectKey) {
    this.projectKey = projectKey;
    return this;
  }
}
