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
package org.sonar.server.search;


import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.activity.index.ActivityIndexer;
import org.sonar.server.db.Dao;
import org.sonar.server.db.DbClient;
import org.sonar.server.issue.index.IssueAuthorizationIndexer;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.qualityprofile.index.ActiveRuleIndex;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.source.index.SourceLineIndexer;
import org.sonar.server.test.index.TestIndexer;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.view.index.ViewIndexer;

import java.util.Date;

public class IndexSynchronizer {

  private static final Logger LOG = Loggers.get(IndexSynchronizer.class);

  private final DbClient db;
  private final IndexClient index;
  private final SourceLineIndexer sourceLineIndexer;
  private final TestIndexer testIndexer;
  private final IssueAuthorizationIndexer issueAuthorizationIndexer;
  private final IssueIndexer issueIndexer;
  private final UserIndexer userIndexer;
  private final ViewIndexer viewIndexer;
  private final ActivityIndexer activityIndexer;

  /**
   * Limitation - {@link org.sonar.server.es.BaseIndexer} are not injected through an array or a collection
   * because we need {@link org.sonar.server.issue.index.IssueAuthorizationIndexer} to be executed before
   * {@link org.sonar.server.issue.index.IssueIndexer}
   */
  public IndexSynchronizer(DbClient db, IndexClient index, SourceLineIndexer sourceLineIndexer,
                           TestIndexer testIndexer, IssueAuthorizationIndexer issueAuthorizationIndexer, IssueIndexer issueIndexer,
                           UserIndexer userIndexer, ViewIndexer viewIndexer, ActivityIndexer activityIndexer) {
    this.db = db;
    this.index = index;
    this.sourceLineIndexer = sourceLineIndexer;
    this.testIndexer = testIndexer;
    this.issueAuthorizationIndexer = issueAuthorizationIndexer;
    this.issueIndexer = issueIndexer;
    this.userIndexer = userIndexer;
    this.viewIndexer = viewIndexer;
    this.activityIndexer = activityIndexer;
  }

  public void executeDeprecated() {
    DbSession session = db.openSession(false);
    try {
      synchronize(session, db.ruleDao(), index.get(RuleIndex.class));
      synchronize(session, db.activeRuleDao(), index.get(ActiveRuleIndex.class));
      session.commit();
    } finally {
      session.close();
    }
  }

  public void execute() {
    LOG.info("Index activities");
    activityIndexer.setEnabled(true).index();

    LOG.info("Index issues");
    issueAuthorizationIndexer.setEnabled(true).index();
    issueIndexer.setEnabled(true).index();

    LOG.info("Index source lines");
    sourceLineIndexer.setEnabled(true).index();

    LOG.info("Index tests");
    testIndexer.setEnabled(true).index();

    LOG.info("Index users");
    userIndexer.setEnabled(true).index();

    LOG.info("Index views");
    viewIndexer.setEnabled(true).index();
  }

  void synchronize(DbSession session, Dao dao, Index index) {
    long count = index.getIndexStat().getDocumentCount();
    Date lastSynch = index.getLastSynchronization();
    LOG.info("Index {}s", index.getIndexType());
    if (count <= 0) {
      dao.synchronizeAfter(session);
    } else {
      dao.synchronizeAfter(session, lastSynch);
    }
  }
}
