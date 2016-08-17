/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.settings.ws;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;

import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;
import static org.sonar.server.component.ComponentFinder.ParamNames.ID_AND_KEY;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;

public class SettingsWsComponentParameters {

  static final String PARAM_COMPONENT_ID = "componentId";
  static final String PARAM_COMPONENT_KEY = "componentKey";

  private final ComponentFinder componentFinder;
  private final UserSession userSession;

  public SettingsWsComponentParameters(ComponentFinder componentFinder, UserSession userSession) {
    this.componentFinder = componentFinder;
    this.userSession = userSession;
  }

  static void addComponentParameters(WebService.NewAction action) {
    action.createParam(PARAM_COMPONENT_ID)
      .setDescription("Component id")
      .setExampleValue(UUID_EXAMPLE_01);

    action.createParam(PARAM_COMPONENT_KEY)
      .setDescription("Component key")
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);
  }

  @CheckForNull
  ComponentDto getComponent(DbSession dbSession, Request request) {
    if (request.hasParam(PARAM_COMPONENT_ID) || request.hasParam(PARAM_COMPONENT_KEY)) {
      return componentFinder.getByUuidOrKey(dbSession, request.param(PARAM_COMPONENT_ID), request.param(PARAM_COMPONENT_KEY), ID_AND_KEY);
    }
    return null;
  }

  void checkAdminPermission(@Nullable ComponentDto component) {
    if (component == null) {
      userSession.checkPermission(GlobalPermissions.SYSTEM_ADMIN);
    } else {
      userSession.checkComponentUuidPermission(UserRole.ADMIN, component.uuid());
    }
  }

}
