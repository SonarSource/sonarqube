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

package org.sonar.server.component;

import com.google.common.base.Optional;
import javax.annotation.Nullable;
import org.sonar.api.resources.Qualifiers;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;

import static com.google.common.base.Preconditions.checkArgument;

public class ComponentFinder {

  private final DbClient dbClient;

  public ComponentFinder(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  public ComponentDto getByUuidOrKey(DbSession dbSession, @Nullable String componentUuid, @Nullable String componentKey) {
    checkArgument(componentUuid != null ^ componentKey != null, "The component key or the component id must be provided, not both.");

    if (componentUuid != null) {
      return getByUuid(dbSession, componentUuid);
    }
    return getByKey(dbSession, componentKey);
  }

  public ComponentDto getByKey(DbSession dbSession, String key) {
    return getIfPresentOrFail(dbClient.componentDao().selectByKey(dbSession, key), String.format("Component key '%s' not found", key));
  }

  public ComponentDto getByUuid(DbSession dbSession, String uuid) {
    return getIfPresentOrFail(dbClient.componentDao().selectByUuid(dbSession, uuid), String.format("Component id '%s' not found", uuid));
  }

  public ComponentDto getProjectByUuidOrKey(DbSession dbSession, @Nullable String projectUuid, @Nullable String projectKey) {
    ComponentDto project;
    if (projectUuid != null) {
      project = getIfPresentOrFail(dbClient.componentDao().selectByUuid(dbSession, projectUuid), String.format("Project id '%s' not found", projectUuid));
    } else {
      project = getIfPresentOrFail(dbClient.componentDao().selectByKey(dbSession, projectKey), String.format("Project key '%s' not found", projectKey));
    }
    checkIsProjectOrModule(project);

    return project;
  }

  private static ComponentDto getIfPresentOrFail(Optional<ComponentDto> component, String errorMsg) {
    if (!component.isPresent()) {
      throw new NotFoundException(errorMsg);
    }
    return component.get();
  }

  private static void checkIsProjectOrModule(ComponentDto component) {
    if (!Qualifiers.PROJECT.equals(component.qualifier()) && !Qualifiers.MODULE.equals(component.qualifier())) {
      throw new BadRequestException(String.format("Component '%s' (id: %s) must be a project or a module.", component.key(), component.uuid()));
    }
  }
}
