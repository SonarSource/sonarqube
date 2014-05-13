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
package org.sonar.server.db;

import org.sonar.api.ServerComponent;
import org.sonar.core.persistence.Database;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.qualityprofile.db.QualityProfileDao;
import org.sonar.server.rule2.ActiveRuleDao;
import org.sonar.server.rule2.RuleDao;

/**
 * Facade for all db components
 */
public class DbClient implements ServerComponent {

  private final Database db;
  private final MyBatis myBatis;
  private final RuleDao ruleDao;
  private final ActiveRuleDao activeRuleDao;
  private final QualityProfileDao qProfileDao;

  public DbClient(Database db, MyBatis myBatis, RuleDao ruleDao, ActiveRuleDao activeRuleDao,
                  QualityProfileDao qProfileDao) {
    this.db = db;
    this.myBatis = myBatis;
    this.ruleDao = ruleDao;
    this.activeRuleDao = activeRuleDao;
    this.qProfileDao = qProfileDao;
  }

  public Database database() {
    return db;
  }

  public DbSession openSession(boolean batch) {
    return myBatis.openSession(batch);
  }

  public RuleDao ruleDao() {
    return ruleDao;
  }

  public ActiveRuleDao activeRuleDao() {
    return activeRuleDao;
  }

  public QualityProfileDao qualityProfileDao() {
    return qProfileDao;
  }
}
