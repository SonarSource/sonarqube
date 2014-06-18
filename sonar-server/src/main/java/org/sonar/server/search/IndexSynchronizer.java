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
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.DbClient;

/**
 * @since 4.4
 */
public class IndexSynchronizer {

  private static final Logger LOG = LoggerFactory.getLogger(IndexSynchronizer.class);

  private final DbClient db;
  private final IndexClient index;

  public IndexSynchronizer(DbClient db, IndexClient index) {
    this.db = db;
    this.index = index;
  }

  public void execute() {
    /* synchronize all activeRules until we have mng tables in INDEX */
    DbSession session = db.openSession(true);
    LOG.info("Starting DB to Index synchronization");
    long start = System.currentTimeMillis();
    db.ruleDao().synchronizeAfter(session, 0);
    db.activeRuleDao().synchronizeAfter(session, 0);
    db.activityDao().synchronizeAfter(session, 0);
    session.commit();
    LOG.info("Synchronization done in {}ms...", System.currentTimeMillis()-start);
    session.close();
  }
}
