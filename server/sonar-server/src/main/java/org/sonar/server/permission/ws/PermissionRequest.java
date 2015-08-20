/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.permission.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.WebService.SelectionMode;
import org.sonar.core.permission.GlobalPermissions;

import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.SELECTED;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.server.permission.ws.Parameters.PARAM_GROUP_ID;
import static org.sonar.server.permission.ws.Parameters.PARAM_GROUP_NAME;
import static org.sonar.server.permission.ws.Parameters.PARAM_PERMISSION;
import static org.sonar.server.permission.ws.Parameters.PARAM_PROJECT_KEY;
import static org.sonar.server.permission.ws.Parameters.PARAM_PROJECT_UUID;
import static org.sonar.server.permission.ws.Parameters.PARAM_USER_LOGIN;
import static org.sonar.server.permission.PermissionValueValidator.validateGlobalPermission;
import static org.sonar.server.permission.PermissionValueValidator.validateProjectPermission;
import static org.sonar.server.ws.WsUtils.checkRequest;

class PermissionRequest {
  private final String permission;
  private final String userLogin;
  private final Long groupId;
  private final String groupName;
  private final String projectUuid;
  private final String projectKey;
  private final boolean hasProject;
  private final Integer page;
  private final Integer pageSize;
  private final String selected;
  private final String query;

  private PermissionRequest(Builder builder) {
    permission = builder.permission;
    userLogin = builder.userLogin;
    groupId = builder.groupId;
    groupName = builder.groupName;
    projectUuid = builder.projectUuid;
    projectKey = builder.projectKey;
    hasProject = builder.hasProject;
    page = builder.page;
    pageSize = builder.pageSize;
    selected = builder.selected;
    query = builder.query;
  }

  static class Builder {

    private final Request request;

    private boolean withUser;
    private boolean withGroup;
    private boolean withPagination;

    private String permission;
    private String userLogin;

    private Long groupId;
    private String groupName;
    private String projectUuid;
    private String projectKey;
    private boolean hasProject;
    private Integer page;
    private Integer pageSize;
    private String selected;
    private String query;

    Builder(Request request) {
      this.request = request;
    }

    PermissionRequest build() {
      permission = request.mandatoryParam(PARAM_PERMISSION);
      setUserLogin(request);
      setGroup(request);
      setProject(request);
      setPaging(request);
      setSelected(request);
      setQuery(request);
      checkPermissionParameter();

      return new PermissionRequest(this);
    }

    Builder withUser() {
      this.withUser = true;
      return this;
    }

    Builder withGroup() {
      this.withGroup = true;
      return this;
    }

    Builder withPagination() {
      this.withPagination = true;
      return this;
    }

    private void setQuery(Request request) {
      if (request.hasParam(TEXT_QUERY)) {
        query = request.param(TEXT_QUERY);
        if (query != null) {
          selected = SelectionMode.ALL.value();
        }
      }
    }

    private void setSelected(Request request) {
      if (request.hasParam(SELECTED)) {
        selected = request.mandatoryParam(SELECTED);
      }
    }

    private void setPaging(Request request) {
      if (withPagination) {
        page = request.mandatoryParamAsInt(PAGE);
        pageSize = request.mandatoryParamAsInt(PAGE_SIZE);
      }
    }

    private void setUserLogin(Request request) {
      if (withUser) {
        userLogin = request.mandatoryParam(PARAM_USER_LOGIN);
      }
    }

    private void setGroup(Request request) {
      if (withGroup) {
        Long groupIdParam = request.paramAsLong(PARAM_GROUP_ID);
        String groupNameParam = request.param(PARAM_GROUP_NAME);
        checkRequest(groupIdParam != null ^ groupNameParam != null, "Group name or group id must be provided, not both.");
        this.groupId = groupIdParam;
        this.groupName = groupNameParam;
      }
    }

    private void setProject(Request request) {
      if (request.hasParam(PARAM_PROJECT_UUID) || request.hasParam(PARAM_PROJECT_KEY)) {
        String projectUuidParam = request.param(PARAM_PROJECT_UUID);
        String projectKeyParam = request.param(PARAM_PROJECT_KEY);

        if (projectUuidParam != null || projectKeyParam != null) {
          checkRequest(projectUuidParam != null ^ projectKeyParam != null, "Project id or project key can be provided, not both.");
          this.projectUuid = projectUuidParam;
          this.projectKey = projectKeyParam;
          hasProject = true;
        }
      }
    }

    private void checkPermissionParameter() {
      if (hasProject) {
        validateProjectPermission(permission);
      } else if (!GlobalPermissions.ALL.contains(permission)) {
        validateGlobalPermission(permission);
      }
    }
  }

  String permission() {
    return permission;
  }

  String userLogin() {
    return userLogin;
  }

  Long groupId() {
    return groupId;
  }

  String groupName() {
    return groupName;
  }

  String projectUuid() {
    return projectUuid;
  }

  String projectKey() {
    return projectKey;
  }

  Integer page() {
    return page;
  }

  Integer pageSize() {
    return pageSize;
  }

  String selected() {
    return selected;
  }

  String query() {
    return query;
  }

  boolean hasProject() {
    return hasProject;
  }
}
