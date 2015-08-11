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
import org.sonar.core.permission.ComponentPermissions;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.server.exceptions.BadRequestException;

import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.SELECTED;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.server.permission.ws.PermissionWsCommons.GLOBAL_PERMISSIONS_ONE_LINE;
import static org.sonar.server.permission.ws.PermissionWsCommons.PARAM_GROUP_ID;
import static org.sonar.server.permission.ws.PermissionWsCommons.PARAM_GROUP_NAME;
import static org.sonar.server.permission.ws.PermissionWsCommons.PARAM_PROJECT_KEY;
import static org.sonar.server.permission.ws.PermissionWsCommons.PARAM_PROJECT_UUID;
import static org.sonar.server.permission.ws.PermissionWsCommons.PARAM_USER_LOGIN;
import static org.sonar.server.permission.ws.PermissionWsCommons.PROJECT_PERMISSIONS_ONE_LINE;
import static org.sonar.server.ws.WsUtils.checkRequest;

public class PermissionRequest {
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
    permission = builder._permission;
    userLogin = builder._userLogin;
    groupId = builder._groupId;
    groupName = builder._groupName;
    projectUuid = builder._projectUuid;
    projectKey = builder._projectKey;
    hasProject = builder._hasProject;
    page = builder._page;
    pageSize = builder._pageSize;
    selected = builder._selected;
    query = builder._query;
  }

  public static class Builder {

    private final Request request;

    private boolean withUser;
    private boolean withGroup;
    private boolean withPagination;

    private String _permission;
    private String _userLogin;

    private Long _groupId;
    private String _groupName;
    private String _projectUuid;
    private String _projectKey;
    private boolean _hasProject;
    private Integer _page;
    private Integer _pageSize;
    private String _selected;
    private String _query;

    public Builder(Request request) {
      this.request = request;
    }

    public PermissionRequest build() {
      _permission = request.mandatoryParam(PermissionWsCommons.PARAM_PERMISSION);
      setUserLogin(request);
      setGroup(request);
      setProject(request);
      setPaging(request);
      setSelected(request);
      setQuery(request);
      checkPermissionParameter();

      return new PermissionRequest(this);
    }

    public Builder withUser() {
      this.withUser = true;
      return this;
    }

    public Builder withGroup() {
      this.withGroup = true;
      return this;
    }

    public Builder withPagination() {
      this.withPagination = true;
      return this;
    }

    private void setQuery(Request request) {
      if (request.hasParam(TEXT_QUERY)) {
        _query = request.param(TEXT_QUERY);
        if (_query != null) {
          _selected = SelectionMode.ALL.value();
        }
      }
    }

    private void setSelected(Request request) {
      if (request.hasParam(SELECTED)) {
        _selected = request.mandatoryParam(SELECTED);
      }
    }

    private void setPaging(Request request) {
      if (withPagination) {
        _page = request.mandatoryParamAsInt(PAGE);
        _pageSize = request.mandatoryParamAsInt(PAGE_SIZE);
      }
    }

    private void setUserLogin(Request request) {
      if (withUser) {
        _userLogin = request.mandatoryParam(PARAM_USER_LOGIN);
      }
    }

    private void setGroup(Request request) {
      if (withGroup) {
        Long groupId = request.paramAsLong(PARAM_GROUP_ID);
        String groupName = request.param(PARAM_GROUP_NAME);
        checkRequest(groupId != null ^ groupName != null, "Group name or group id must be provided, not both.");
        _groupId = groupId;
        _groupName = groupName;
      }
    }

    private void setProject(Request request) {
      if (request.hasParam(PARAM_PROJECT_UUID) || request.hasParam(PARAM_PROJECT_KEY)) {
        String projectUuid = request.param(PARAM_PROJECT_UUID);
        String projectKey = request.param(PARAM_PROJECT_KEY);

        if (projectUuid != null || projectKey != null) {
          checkRequest(projectUuid != null ^ projectKey != null, "Project id or project key can be provided, not both.");
          _projectUuid = projectUuid;
          _projectKey = projectKey;
          _hasProject = true;
        }
      }
    }

    private void checkPermissionParameter() {
      if (_hasProject) {
        if (!ComponentPermissions.ALL.contains(_permission)) {
          throw new BadRequestException(String.format("Incorrect value '%s' for project permissions. Values allowed: %s.", _permission, PROJECT_PERMISSIONS_ONE_LINE));
        }
      } else {
        if (!GlobalPermissions.ALL.contains(_permission)) {
          throw new BadRequestException(String.format("Incorrect value '%s' for global permissions. Values allowed: %s.", _permission, GLOBAL_PERMISSIONS_ONE_LINE));
        }
      }
    }

  }

  public String permission() {
    return permission;
  }

  public String userLogin() {
    return userLogin;
  }

  public Long groupId() {
    return groupId;
  }

  public String groupName() {
    return groupName;
  }

  public String projectUuid() {
    return projectUuid;
  }

  public String projectKey() {
    return projectKey;
  }

  public Integer page() {
    return page;
  }

  public Integer pageSize() {
    return pageSize;
  }

  public String selected() {
    return selected;
  }

  public String query() {
    return query;
  }

  public boolean hasProject() {
    return hasProject;
  }
}
