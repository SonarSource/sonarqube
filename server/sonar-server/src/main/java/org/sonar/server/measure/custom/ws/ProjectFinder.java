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

package org.sonar.server.measure.custom.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.DbSession;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.NotFoundException;

import static com.google.common.base.Preconditions.checkArgument;

class ProjectFinder {
  private ProjectFinder() {
    // utility class
  }

  static ComponentDto searchProject(DbSession dbSession, DbClient dbClient, Request request) {
    String projectUuid = request.param(CreateAction.PARAM_PROJECT_ID);
    String projectKey = request.param(CreateAction.PARAM_PROJECT_KEY);
    checkArgument(projectUuid != null ^ projectKey != null, "The project key or the project id must be provided, not both.");

    if (projectUuid != null) {
      ComponentDto project = dbClient.componentDao().selectNullableByUuid(dbSession, projectUuid);
      if (project == null) {
        throw new NotFoundException(String.format("Project id '%s' not found", projectUuid));
      }

      return project;
    }

    ComponentDto project = dbClient.componentDao().selectNullableByKey(dbSession, projectKey);
    if (project == null) {
      throw new NotFoundException(String.format("Project key '%s' not found", projectKey));
    }

    return project;
  }

}
