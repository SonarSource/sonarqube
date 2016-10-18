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
package org.sonar.server.es;

import org.sonar.api.config.Settings;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.permission.index.AuthorizationIndexer;
import org.sonar.server.component.es.ProjectMeasuresIndexer;
import org.sonar.server.test.index.TestIndexer;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.view.index.ViewIndexer;

public class IndexerStartupTask {

  private static final Logger LOG = Loggers.get(IndexerStartupTask.class);

  private final TestIndexer testIndexer;
  private final AuthorizationIndexer authorizationIndexer;
  private final IssueIndexer issueIndexer;
  private final UserIndexer userIndexer;
  private final ViewIndexer viewIndexer;
  private final ProjectMeasuresIndexer projectMeasuresIndexer;
  private final Settings settings;

  /**
   * Limitation - {@link org.sonar.server.es.BaseIndexer} are not injected through an array or a collection
   * because we need {@link AuthorizationIndexer} to be executed before
   * {@link org.sonar.server.issue.index.IssueIndexer}
   */
  public IndexerStartupTask(TestIndexer testIndexer, AuthorizationIndexer authorizationIndexer, IssueIndexer issueIndexer,
                            UserIndexer userIndexer, ViewIndexer viewIndexer, ProjectMeasuresIndexer projectMeasuresIndexer,
                            Settings settings) {
    this.testIndexer = testIndexer;
    this.authorizationIndexer = authorizationIndexer;
    this.issueIndexer = issueIndexer;
    this.userIndexer = userIndexer;
    this.viewIndexer = viewIndexer;
    this.projectMeasuresIndexer = projectMeasuresIndexer;
    this.settings = settings;
  }

  public void execute() {
    if (!settings.getBoolean("sonar.internal.es.disableIndexes")) {

      LOG.info("Index authorization");
      authorizationIndexer.index();

      LOG.info("Index issues");
      issueIndexer.index();

      LOG.info("Index tests");
      testIndexer.index();

      LOG.info("Index users");
      userIndexer.index();

      LOG.info("Index views");
      viewIndexer.index();

      LOG.info("Index project measures");
      projectMeasuresIndexer.index();
    }
  }

}
