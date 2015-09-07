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
import javax.annotation.CheckForNull;
import org.sonar.api.server.ws.Request;

import static org.sonar.server.permission.ws.WsPermissionParameters.PARAM_PROJECT_ID;
import static org.sonar.server.permission.ws.WsPermissionParameters.PARAM_PROJECT_KEY;
import static org.sonar.server.ws.WsUtils.checkRequest;

/**
 * Project identifiers from a WS request. Guaranties the project id and project key are not provided at the same time.
 */
class WsProjectRef {
  private final String uuid;
  private final String key;

  private WsProjectRef(Request wsRequest) {
    this.uuid = wsRequest.param(PARAM_PROJECT_ID);
    this.key = wsRequest.param(PARAM_PROJECT_KEY);
    checkRequest(uuid != null ^ key != null, "Project id or project key can be provided, not both.");
  }

  static Optional<WsProjectRef> optionalFromRequest(Request wsRequest) {
    if (!hasProjectParam(wsRequest)) {
      return Optional.absent();
    }

    return Optional.of(new WsProjectRef(wsRequest));
  }

  static WsProjectRef fromRequest(Request wsRequest) {
    checkRequest(hasProjectParam(wsRequest), "Project id or project key must be provided, not both.");

    return new WsProjectRef(wsRequest);
  }

  @CheckForNull
  String uuid() {
    return this.uuid;
  }

  @CheckForNull
  String key() {
    return this.key;
  }

  private static boolean hasProjectParam(Request wsRequest) {
    return wsRequest.hasParam(PARAM_PROJECT_ID) || wsRequest.hasParam(PARAM_PROJECT_KEY);
  }
}
