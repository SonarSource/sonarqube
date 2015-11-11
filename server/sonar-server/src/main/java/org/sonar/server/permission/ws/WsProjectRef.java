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
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;

import static org.sonar.server.permission.ws.PermissionsWsParameters.PARAM_PROJECT_ID;
import static org.sonar.server.permission.ws.PermissionsWsParameters.PARAM_PROJECT_KEY;
import static org.sonar.server.ws.WsUtils.checkRequest;

/**
 * Project identifiers from a WS request. Guaranties the project id and project key are not provided at the same time.
 */
public class WsProjectRef {
  private final String uuid;
  private final String key;

  private WsProjectRef(@Nullable String uuid, @Nullable String key) {
    this.uuid = uuid;
    this.key = key;
    checkRequest(this.uuid != null ^ this.key != null, "Project id or project key can be provided, not both.");
  }

  private WsProjectRef(Request wsRequest) {
    this(wsRequest.param(PARAM_PROJECT_ID), wsRequest.param(PARAM_PROJECT_KEY));
  }

  public static Optional<WsProjectRef> newOptionalWsProjectRef(Request wsRequest) {
    if (!hasProjectParam(wsRequest)) {
      return Optional.absent();
    }

    return Optional.of(new WsProjectRef(wsRequest));
  }

  public static Optional<WsProjectRef> newOptionalWsProjectRef(@Nullable String uuid, @Nullable String key) {
    if (uuid == null && key == null) {
      return Optional.absent();
    }

    return Optional.of(new WsProjectRef(uuid, key));
  }

  public static WsProjectRef newWsProjectRef(Request wsRequest) {
    checkRequest(hasProjectParam(wsRequest), "Project id or project key must be provided, not both.");

    return new WsProjectRef(wsRequest);
  }

  @CheckForNull
  public String uuid() {
    return this.uuid;
  }

  @CheckForNull
  public String key() {
    return this.key;
  }

  private static boolean hasProjectParam(Request wsRequest) {
    return wsRequest.hasParam(PARAM_PROJECT_ID) || wsRequest.hasParam(PARAM_PROJECT_KEY);
  }
}
