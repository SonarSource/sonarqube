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

import static org.sonar.server.permission.ws.WsPermissionParameters.PARAM_TEMPLATE_ID;
import static org.sonar.server.permission.ws.WsPermissionParameters.PARAM_TEMPLATE_NAME;
import static org.sonar.server.ws.WsUtils.checkRequest;

/**
 * Template from a WS request. Guaranties the template id or the template name is provided, not both.
 */
public class WsTemplateRef {

  private final String uuid;
  private final String name;

  private WsTemplateRef(@Nullable String uuid, @Nullable String name) {
    checkRequest(uuid != null ^ name != null, "Template name or template id must be provided, not both.");

    this.uuid = uuid;
    this.name = name;
  }

  public static WsTemplateRef fromRequest(Request wsRequest) {
    String uuid = wsRequest.param(PARAM_TEMPLATE_ID);
    String name = wsRequest.param(PARAM_TEMPLATE_NAME);

    return new WsTemplateRef(uuid, name);
  }

  public static WsTemplateRef newTemplateRef(@Nullable String uuid, @Nullable String name) {
    return new WsTemplateRef(uuid, name);
  }

  @CheckForNull
  public String uuid() {
    return this.uuid;
  }

  @CheckForNull
  public String name() {
    return this.name;
  }
}
