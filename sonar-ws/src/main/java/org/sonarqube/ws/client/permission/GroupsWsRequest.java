package org.sonarqube.ws.client.permission;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class GroupsWsRequest {
  private String permission;
  private String projectId;
  private String projectKey;
  private int page;
  private int pageSize;
  private String query;
  private String selected;

  public String getPermission() {
    return permission;
  }

  public GroupsWsRequest setPermission(String permission) {
    this.permission = permission;
    return this;
  }

  @CheckForNull
  public String getProjectId() {
    return projectId;
  }

  public GroupsWsRequest setProjectId(String projectId) {
    this.projectId = projectId;
    return this;
  }

  @CheckForNull
  public String getProjectKey() {
    return projectKey;
  }

  public GroupsWsRequest setProjectKey(String projectKey) {
    this.projectKey = projectKey;
    return this;
  }

  public int getPage() {
    return page;
  }

  public GroupsWsRequest setPage(int page) {
    this.page = page;
    return this;
  }

  public int getPageSize() {
    return pageSize;
  }

  public GroupsWsRequest setPageSize(int pageSize) {
    this.pageSize = pageSize;
    return this;
  }

  @CheckForNull
  public String getQuery() {
    return query;
  }

  public GroupsWsRequest setQuery(@Nullable String query) {
    this.query = query;
    return this;
  }

  public String getSelected() {
    return selected;
  }

  public GroupsWsRequest setSelected(String selected) {
    this.selected = selected;
    return this;
  }
}
