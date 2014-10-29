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

import org.sonar.api.ServerComponent;
import org.sonar.api.resources.Scopes;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.purge.PurgeDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.search.IndexClient;

public class ComponentCleanerService implements ServerComponent {

  private final DbClient dbClient;
  private final PurgeDao purgeDao;
  private final IndexClient indexClient;

  public ComponentCleanerService(DbClient dbClient, PurgeDao purgeDao, IndexClient indexClient) {
    this.dbClient = dbClient;
    this.purgeDao = purgeDao;
    this.indexClient = indexClient;
  }

  public void delete(String projectKey) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      ComponentDto project = dbClient.componentDao().getByKey(dbSession, projectKey);
      if (!Scopes.PROJECT.equals(project.scope())) {
        throw new IllegalArgumentException("Only project can be deleted");
      }
      purgeDao.deleteResourceTree(project.getId());
      deletePermissionIndexes(dbSession, project.uuid());
      dbSession.commit();

      deleteIssuesFromIndex(project.uuid());
    } finally {
      MyBatis.closeQuietly(dbSession);
    }
  }

  private void deleteIssuesFromIndex(String projectUuid) {
    indexClient.get(IssueIndex.class).deleteByProjectUuid(projectUuid);
  }

  private void deletePermissionIndexes(DbSession session, String projectUuid) {
    dbClient.issueAuthorizationDao().deleteByKey(session, projectUuid);
  }

}
