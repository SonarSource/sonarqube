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

import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.activity.index.ActivityIndex;
import org.sonar.server.db.Dao;
import org.sonar.server.db.DbClient;
import org.sonar.server.issue.index.IssueAuthorizationIndexer;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueNormalizer;
import org.sonar.server.qualityprofile.index.ActiveRuleIndex;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.source.index.SourceLineIndexer;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @since 4.4
 */
public class IndexSynchronizer {

  private static final Logger LOG = LoggerFactory.getLogger(IndexSynchronizer.class);

  private final DbClient db;
  private final IndexClient index;
  private final SourceLineIndexer sourceLineIndexer;
  private final IssueAuthorizationIndexer issueAuthorizationIndexer;

  public IndexSynchronizer(DbClient db, IndexClient index, SourceLineIndexer sourceLineIndexer,
                           IssueAuthorizationIndexer issueAuthorizationIndexer) {
    this.db = db;
    this.index = index;
    this.sourceLineIndexer = sourceLineIndexer;
    this.issueAuthorizationIndexer = issueAuthorizationIndexer;
  }

  public void execute() {
    /* synchronize all activeRules until we have mng tables in INDEX */
    DbSession session = db.openSession(true);
    try {
      LOG.info("Starting DB to Index synchronization");
      long start = System.currentTimeMillis();
      List<String> projectUuids = db.componentDao().findProjectUuids(session);
      synchronize(session, db.ruleDao(), index.get(RuleIndex.class));
      issueAuthorizationIndexer.index();
      synchronizeByProject(session, db.issueDao(), index.get(IssueIndex.class),
        IssueNormalizer.IssueField.PROJECT.field(), projectUuids);
      synchronize(session, db.activeRuleDao(), index.get(ActiveRuleIndex.class));
      synchronize(session, db.activityDao(), index.get(ActivityIndex.class));

      LOG.info("Indexing of sourceLine records");
      sourceLineIndexer.index();

      session.commit();
      LOG.info("Synchronization done in {}ms...", System.currentTimeMillis() - start);
    } finally {
      session.close();
    }
  }

  void synchronize(DbSession session, Dao dao, Index index) {
    long count = index.getIndexStat().getDocumentCount();
    Date lastSynch = index.getLastSynchronization();
    if (count <= 0) {
      LOG.info("Initial indexing of {} records", index.getIndexType());
      dao.synchronizeAfter(session);
    } else {
      LOG.info("Synchronizing {} records for updates after {}", index.getIndexType(), lastSynch);
      dao.synchronizeAfter(session, lastSynch);
    }
  }

  void synchronizeByProject(DbSession session, Dao dao, Index index, String projectField, List<String> projectUuids) {
    Long count = index.getIndexStat().getDocumentCount();
    if (count <= 0) {
      LOG.info("Initial indexing of {} records", index.getIndexType());
      dao.synchronizeAfter(session);
    } else {
      LOG.info("Synchronizing {} records for updates", index.getIndexType());
      for (String projectUuid : projectUuids) {
        Map<String, String> params = ImmutableMap.of(projectField, projectUuid);
        dao.synchronizeAfter(session, index.getLastSynchronization(params), params);
      }
    }
  }
}
