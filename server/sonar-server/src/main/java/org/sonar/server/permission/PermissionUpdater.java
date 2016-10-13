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
package org.sonar.server.permission;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.issue.index.IssueAuthorizationIndexer;

public class PermissionUpdater {

  private final DbClient dbClient;
  private final IssueAuthorizationIndexer issueAuthorizationIndexer;
  private final UserPermissionChanger userPermissionChanger;
  private final GroupPermissionChanger groupPermissionChanger;

  public PermissionUpdater(DbClient dbClient, IssueAuthorizationIndexer issueAuthorizationIndexer,
    UserPermissionChanger userPermissionChanger, GroupPermissionChanger groupPermissionChanger) {
    this.dbClient = dbClient;
    this.issueAuthorizationIndexer = issueAuthorizationIndexer;
    this.userPermissionChanger = userPermissionChanger;
    this.groupPermissionChanger = groupPermissionChanger;
  }

  public void apply(DbSession dbSession, Collection<PermissionChange> changes) {
    Set<Long> projectIds = new HashSet<>();
    for (PermissionChange change : changes) {
      boolean changed = doApply(dbSession, change);
      Optional<ProjectRef> projectId = change.getProjectRef();
      if (changed && projectId.isPresent()) {
        projectIds.add(projectId.get().getId());
      }
    }
    for (Long projectId : projectIds) {
      dbClient.resourceDao().updateAuthorizationDate(projectId, dbSession);
    }
    dbSession.commit();

    if (!projectIds.isEmpty()) {
      issueAuthorizationIndexer.index();
    }
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
