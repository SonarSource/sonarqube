/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import org.sonar.db.DbSession;
import org.sonar.server.es.Indexers;

/**
 * Add or remove global/project permissions to a group. This class does not verify that caller has administration right on the related project.
 */
public class PermissionUpdater {

  private final Indexers indexers;
  private final UserPermissionChanger userPermissionChanger;
  private final GroupPermissionChanger groupPermissionChanger;

  public PermissionUpdater(Indexers indexers,
    UserPermissionChanger userPermissionChanger, GroupPermissionChanger groupPermissionChanger) {
    this.indexers = indexers;
    this.userPermissionChanger = userPermissionChanger;
    this.groupPermissionChanger = groupPermissionChanger;
  }

  public void apply(DbSession dbSession, Collection<PermissionChange> changes) {
    List<String> projectOrViewUuids = new ArrayList<>();
    for (PermissionChange change : changes) {
      boolean changed = doApply(dbSession, change);
      String projectUuid = change.getProjectUuid();
      if (changed && projectUuid != null) {
        projectOrViewUuids.add(projectUuid);
      }
    }
    indexers.commitAndIndexOnEntityEvent(dbSession, projectOrViewUuids, Indexers.EntityEvent.PERMISSION_CHANGE);
  }

  private boolean doApply(DbSession dbSession, PermissionChange change) {
    if (change instanceof UserPermissionChange userPermissionChange) {
      return userPermissionChanger.apply(dbSession, userPermissionChange);
    }
    if (change instanceof GroupPermissionChange groupPermissionChange) {
      return groupPermissionChanger.apply(dbSession, groupPermissionChange);
    }
    throw new UnsupportedOperationException("Unsupported permission change: " + change.getClass());

  }
}
