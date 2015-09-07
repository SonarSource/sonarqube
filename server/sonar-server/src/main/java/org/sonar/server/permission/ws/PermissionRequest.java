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

import com.google.common.base.Optional;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.WebService.SelectionMode;

import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.SELECTED;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.server.permission.ws.PermissionRequestValidator.validateGlobalPermission;
import static org.sonar.server.permission.ws.PermissionRequestValidator.validateProjectPermission;
import static org.sonar.server.permission.ws.WsPermissionParameters.PARAM_PERMISSION;
import static org.sonar.server.permission.ws.WsPermissionParameters.PARAM_USER_LOGIN;

class PermissionRequest {
  private final String permission;
  private final String userLogin;
  private final WsGroupRef group;
  private final Optional<WsProjectRef> project;
  private final Integer page;
  private final Integer pageSize;
  private final String selected;
  private final String query;

  private PermissionRequest(Builder builder) {
    permission = builder.permission;
    userLogin = builder.userLogin;
    group = builder.group;
    project = builder.project;
    page = builder.page;
    pageSize = builder.pageSize;
    selected = builder.selected;
    query = builder.query;
  }

  public static class Builder {

    private final Request request;

    private boolean withUser;
    private boolean withGroup;
    private boolean withPagination;

    private String permission;
    private String userLogin;

    private WsGroupRef group;
    private Optional<WsProjectRef> project;
    private Integer page;
    private Integer pageSize;
    private String selected;
    private String query;

    public Builder(Request request) {
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
        this.group = WsGroupRef.fromRequest(request);
      }
    }

    private void setProject(Request request) {
      this.project = WsProjectRef.optionalFromRequest(request);
    }

    private void checkPermissionParameter() {
      if (project.isPresent()) {
        validateProjectPermission(permission);
      } else {
        validateGlobalPermission(permission);
      }
    }
  }

  public String permission() {
    return permission;
  }

  public String userLogin() {
    return userLogin;
  }

  public WsGroupRef group() {
    return group;
  }

  public Optional<WsProjectRef> project() {
    return project;
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
}
