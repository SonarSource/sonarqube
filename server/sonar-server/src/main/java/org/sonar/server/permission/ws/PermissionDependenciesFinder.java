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
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.user.GroupDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.NotFoundException;

import static com.google.common.base.Preconditions.checkNotNull;

public class PermissionDependenciesFinder {
  private final DbClient dbClient;
  private final ComponentFinder componentFinder;

  public PermissionDependenciesFinder(DbClient dbClient, ComponentFinder componentFinder) {
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
  }

  GroupDto getGroup(DbSession dbSession, PermissionRequest request) {
    GroupDto group = null;

    Long groupId = request.groupId();
    if (groupId != null) {
      group = dbClient.groupDao().selectById(dbSession, groupId);
      if (group == null) {
        throw new NotFoundException(String.format("Group with id '%d' is not found", groupId));
      }
    }

    String groupName = request.groupName();
    if (groupName != null) {
      group = dbClient.groupDao().selectByKey(dbSession, groupName);
      if (group == null) {
        throw new NotFoundException(String.format("Group with name '%s' is not found", groupName));
      }
    }

    return checkNotNull(group);
  }

  Optional<ComponentDto> searchProject(DbSession dbSession, PermissionRequest request) {
    if (!request.hasProject()) {
      return Optional.absent();
    }

    return Optional.of(componentFinder.getProjectByUuidOrKey(dbSession, request.projectUuid(), request.projectKey()));
  }
}
