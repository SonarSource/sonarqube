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

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;

import static org.sonar.server.permission.ws.WsPermissionParameters.PARAM_GROUP_ID;
import static org.sonar.server.permission.ws.WsPermissionParameters.PARAM_GROUP_NAME;
import static org.sonar.server.ws.WsUtils.checkRequest;

/**
 * Group from a WS request. Guaranties the group id or the group name is provided, not both.
 */
class WsGroupRef {

  private final Long id;
  private final String name;

  private WsGroupRef(@Nullable Long id, @Nullable String name) {
    checkRequest(id != null ^ name != null, "Group name or group id must be provided, not both.");

    this.id = id;
    this.name = name;
  }

  static WsGroupRef fromRequest(Request wsRequest) {
    Long id = wsRequest.paramAsLong(PARAM_GROUP_ID);
    String name = wsRequest.param(PARAM_GROUP_NAME);

    return new WsGroupRef(id, name);
  }

  @CheckForNull
  Long id() {
    return this.id;
  }

  @CheckForNull
  String name() {
    return this.name;
  }
}
