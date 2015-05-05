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
import org.sonar.core.purge.IdUuidPair;
import org.sonar.core.purge.PurgeDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.issue.index.IssueAuthorizationIndexer;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.source.index.SourceLineIndexer;
import org.sonar.server.test.index.TestIndexer;

public class ComponentCleanerService implements ServerComponent {

  private final DbClient dbClient;
  private final PurgeDao purgeDao;
  private final IssueAuthorizationIndexer issueAuthorizationIndexer;
  private final IssueIndexer issueIndexer;
  private final SourceLineIndexer sourceLineIndexer;
  private final TestIndexer testIndexer;

  public ComponentCleanerService(DbClient dbClient, PurgeDao purgeDao, IssueAuthorizationIndexer issueAuthorizationIndexer, IssueIndexer issueIndexer,
                                 SourceLineIndexer sourceLineIndexer, TestIndexer testIndexer) {
    this.dbClient = dbClient;
    this.purgeDao = purgeDao;
    this.issueAuthorizationIndexer = issueAuthorizationIndexer;
    this.issueIndexer = issueIndexer;
    this.sourceLineIndexer = sourceLineIndexer;
    this.testIndexer = testIndexer;
  }

  public void delete(String projectKey) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      ComponentDto project = dbClient.componentDao().getByKey(dbSession, projectKey);
      if (!Scopes.PROJECT.equals(project.scope())) {
        throw new IllegalArgumentException("Only projects can be deleted");
      }
      purgeDao.deleteResourceTree(new IdUuidPair(project.getId(), project.uuid()));
      dbSession.commit();

      deleteFromIndices(project.uuid());
    } finally {
      MyBatis.closeQuietly(dbSession);
    }
  }

  private void deleteFromIndices(String projectUuid) {
    // optimization : index "issues" is refreshed once at the end
    issueAuthorizationIndexer.deleteProject(projectUuid, false);
    issueIndexer.deleteProject(projectUuid, true);
    sourceLineIndexer.deleteByProject(projectUuid);
    testIndexer.deleteByProject(projectUuid);
  }

}
