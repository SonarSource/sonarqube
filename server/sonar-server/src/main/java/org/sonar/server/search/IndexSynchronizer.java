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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Settings;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.activity.index.ActivityIndex;
import org.sonar.server.db.Dao;
import org.sonar.server.db.DbClient;
import org.sonar.server.issue.index.IssueAuthorizationIndex;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.qualityprofile.index.ActiveRuleIndex;
import org.sonar.server.rule.index.RuleIndex;

import java.util.Date;

/**
 * @since 4.4
 */
public class IndexSynchronizer {

  private static final Logger LOG = LoggerFactory.getLogger(IndexSynchronizer.class);

  private final DbClient db;
  private final IndexClient index;
  private final Settings settings;

  public IndexSynchronizer(DbClient db, IndexClient index, Settings settings) {
    this.db = db;
    this.index = index;
    this.settings = settings;
  }

  public void execute() {
    /* synchronize all activeRules until we have mng tables in INDEX */
    DbSession session = db.openSession(true);
    LOG.info("Starting DB to Index synchronization");
    long start = System.currentTimeMillis();
    synchronize(session, db.ruleDao(), index.get(RuleIndex.class));

    // Switch Issue search
    if (settings.getString("sonar.issues.use_es_backend") != null) {
      synchronize(session, db.issueDao(), index.get(IssueIndex.class));
      synchronize(session, db.issueAuthorizationDao(), index.get(IssueAuthorizationIndex.class));
    }

    synchronize(session, db.activeRuleDao(), index.get(ActiveRuleIndex.class));
    synchronize(session, db.activityDao(), index.get(ActivityIndex.class));
    session.commit();
    LOG.info("Synchronization done in {}ms...", System.currentTimeMillis() - start);
    session.close();
  }

  void synchronize(DbSession session, Dao dao, Index index) {
    Long count = index.getIndexStat().getDocumentCount();
    Date lastSynch = index.getLastSynchronization();
    if (count <= 0) {
      LOG.info("Initial indexing of {} records", index.getIndexType());
    } else {
      LOG.info("Synchronizing {} records for updates after {}", index.getIndexType(), lastSynch);
    }
    dao.synchronizeAfter(session,
      index.getLastSynchronization());
    index.refresh();
  }
}
