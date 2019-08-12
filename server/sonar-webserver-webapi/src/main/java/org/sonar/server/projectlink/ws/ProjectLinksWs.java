/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.server.projectlink.ws;

import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.component.ComponentDto;

import static java.lang.String.format;
import static org.sonar.server.exceptions.BadRequestException.checkRequest;

public class ProjectLinksWs implements WebService {

  private final ProjectLinksWsAction[] actions;

  public ProjectLinksWs(ProjectLinksWsAction... actions) {
    this.actions = actions;
  }

  @Override
  public void define(Context context) {
    NewController controller = context.createController("api/project_links")
      .setDescription("Manage projects links.")
      .setSince("6.1");

    for (ProjectLinksWsAction action : actions) {
      action.define(controller);
    }

    controller.done();
  }

  static ComponentDto checkProject(ComponentDto component) {
    checkRequest(component.scope().equals(Scopes.PROJECT) && component.qualifier().equals(Qualifiers.PROJECT),
      format("Component '%s' must be a project.", component.getKey()));
    return component;
  }

}
