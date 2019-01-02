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
package org.sonar.server.permission;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.sonar.db.DbSession;
import org.sonar.server.es.ProjectIndexer;
import org.sonar.server.es.ProjectIndexers;

/**
 * Add or remove global/project permissions to a group. This class
 * does not verify that caller has administration right on the related
 * organization or project.
 */
public class PermissionUpdater {

  private final ProjectIndexers projectIndexers;
  private final UserPermissionChanger userPermissionChanger;
  private final GroupPermissionChanger groupPermissionChanger;

  public PermissionUpdater(ProjectIndexers projectIndexers,
    UserPermissionChanger userPermissionChanger, GroupPermissionChanger groupPermissionChanger) {
    this.projectIndexers = projectIndexers;
    this.userPermissionChanger = userPermissionChanger;
    this.groupPermissionChanger = groupPermissionChanger;
  }

  public void apply(DbSession dbSession, Collection<PermissionChange> changes) {
    List<String> projectOrViewUuids = new ArrayList<>();
    for (PermissionChange change : changes) {
      boolean changed = doApply(dbSession, change);
      Optional<ProjectId> projectId = change.getProjectId();
      if (changed && projectId.isPresent()) {
        projectOrViewUuids.add(projectId.get().getUuid());
      }
    }
    projectIndexers.commitAndIndexByProjectUuids(dbSession, projectOrViewUuids, ProjectIndexer.Cause.PERMISSION_CHANGE);
  }

  private boolean doApply(DbSession dbSession, PermissionChange change) {
    if (change instanceof UserPermissionChange) {
      return userPermissionChanger.apply(dbSession, (UserPermissionChange) change);
    }
    if (change instanceof GroupPermissionChange) {
      return groupPermissionChanger.apply(dbSession, (GroupPermissionChange) change);
    }
    throw new UnsupportedOperationException("Unsupported permission change: " + change.getClass());

  }
}
