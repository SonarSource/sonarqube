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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.es.Indexers;

import static java.util.stream.Collectors.groupingBy;
import static org.sonar.api.utils.Preconditions.checkState;
import static org.sonar.server.es.Indexers.EntityEvent.PERMISSION_CHANGE;

/**
 * Add or remove global/project permissions to a group. This class does not verify that caller has administration right on the related project.
 */
public class PermissionUpdater {

  private final Indexers indexers;
  private final UserPermissionChanger userPermissionChanger;
  private final GroupPermissionChanger groupPermissionChanger;
  private final DbClient dbClient;

  public PermissionUpdater(Indexers indexers,
    UserPermissionChanger userPermissionChanger, GroupPermissionChanger groupPermissionChanger, DbClient dbClient) {
    this.indexers = indexers;
    this.userPermissionChanger = userPermissionChanger;
    this.groupPermissionChanger = groupPermissionChanger;
    this.dbClient = dbClient;
  }

  public void applyForUser(DbSession dbSession, Collection<UserPermissionChange> changes) {
    List<String> projectOrViewUuids = new ArrayList<>();
    for (UserPermissionChange change : changes) {
      boolean changed = userPermissionChanger.apply(dbSession, change);
      String projectUuid = change.getProjectUuid();
      if (changed && projectUuid != null) {
        projectOrViewUuids.add(projectUuid);
      }
    }
    indexers.commitAndIndexOnEntityEvent(dbSession, projectOrViewUuids, PERMISSION_CHANGE);
  }

  public void applyForGroups(DbSession dbSession, Collection<GroupPermissionChange> groupsPermissionChanges) {
    checkState(groupsPermissionChanges.stream().map(PermissionChange::getProjectUuid).distinct().count() <= 1,
      "Only one project per group of changes is supported");

    List<String> projectOrViewUuids = new ArrayList<>();
    Map<GroupUuidOrAnyone, List<GroupPermissionChange>> groupUuidToChanges = groupsPermissionChanges.stream().collect(groupingBy(GroupPermissionChange::getGroupUuidOrAnyone));
    groupUuidToChanges.values().forEach(groupPermissionChanges -> applyForSingleGroup(dbSession, projectOrViewUuids, groupPermissionChanges));

    indexers.commitAndIndexOnEntityEvent(dbSession, projectOrViewUuids, PERMISSION_CHANGE);
  }

  private void applyForSingleGroup(DbSession dbSession, List<String> projectOrViewUuids, List<GroupPermissionChange> groupPermissionChanges) {
    GroupPermissionChange anyGroupPermissionChange = groupPermissionChanges.iterator().next();
    Set<String> existingPermissions = loadExistingEntityPermissions(dbSession, anyGroupPermissionChange);
    for (GroupPermissionChange groupPermissionChange : groupPermissionChanges) {
      if (doApplyForGroup(dbSession, existingPermissions, groupPermissionChange) && groupPermissionChange.getProjectUuid() != null) {
        projectOrViewUuids.add(groupPermissionChange.getProjectUuid());
      }
    }
  }

  private boolean doApplyForGroup(DbSession dbSession, Set<String> existingPermissions, GroupPermissionChange change) {
    return groupPermissionChanger.apply(dbSession, existingPermissions, change);
  }

  private Set<String> loadExistingEntityPermissions(DbSession dbSession, GroupPermissionChange change) {
    String projectUuid = change.getProjectUuid();
    String groupUuid = change.getGroupUuidOrAnyone().getUuid();
    if (projectUuid != null) {
      return new HashSet<>(dbClient.groupPermissionDao().selectEntityPermissionsOfGroup(dbSession, groupUuid, projectUuid));
    }
    return new HashSet<>(dbClient.groupPermissionDao().selectGlobalPermissionsOfGroup(dbSession, groupUuid));
  }
}
