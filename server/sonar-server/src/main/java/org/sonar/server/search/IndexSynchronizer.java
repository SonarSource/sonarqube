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
import org.sonar.server.activity.index.ActivityIndex;
import org.sonar.server.db.Dao;
import org.sonar.server.db.DbClient;
import org.sonar.server.issue.index.IssueAuthorizationIndexer;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.qualityprofile.index.ActiveRuleIndex;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.source.index.SourceLineIndexer;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.view.index.ViewIndexer;

import java.util.Date;

public class IndexSynchronizer {

  private static final Logger LOG = Loggers.get(IndexSynchronizer.class);

  private final DbClient db;
  private final IndexClient index;
  private final SourceLineIndexer sourceLineIndexer;
  private final IssueAuthorizationIndexer issueAuthorizationIndexer;
  private final IssueIndexer issueIndexer;
  private final UserIndexer userIndexer;
  private final ViewIndexer viewIndexer;

  public IndexSynchronizer(DbClient db, IndexClient index, SourceLineIndexer sourceLineIndexer,
    IssueAuthorizationIndexer issueAuthorizationIndexer, IssueIndexer issueIndexer, UserIndexer userIndexer, ViewIndexer viewIndexer) {
    this.db = db;
    this.index = index;
    this.sourceLineIndexer = sourceLineIndexer;
    this.issueAuthorizationIndexer = issueAuthorizationIndexer;
    this.issueIndexer = issueIndexer;
    this.userIndexer = userIndexer;
    this.viewIndexer = viewIndexer;
  }

  public void execute() {
    DbSession session = db.openSession(false);
    try {
      synchronize(session, db.ruleDao(), index.get(RuleIndex.class));
      synchronize(session, db.activeRuleDao(), index.get(ActiveRuleIndex.class));
      synchronize(session, db.activityDao(), index.get(ActivityIndex.class));
      session.commit();
    } finally {
      session.close();
    }

    LOG.info("Index issues");
    issueAuthorizationIndexer.index();
    issueIndexer.index();

    LOG.info("Index source files");
    sourceLineIndexer.index();

    LOG.info("Index users");
    userIndexer.index();

    LOG.info("Index views");
    viewIndexer.index();
  }

  void synchronize(DbSession session, Dao dao, Index index) {
    long count = index.getIndexStat().getDocumentCount();
    Date lastSynch = index.getLastSynchronization();
    if (count <= 0) {
      LOG.info("Index {}s", index.getIndexType());
      dao.synchronizeAfter(session);
    } else {
      LOG.info("Index {}s for updates after {}", index.getIndexType(), lastSynch);
      dao.synchronizeAfter(session, lastSynch);
    }
  }
}
