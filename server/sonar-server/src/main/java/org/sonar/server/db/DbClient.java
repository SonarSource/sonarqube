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

import java.util.Map;
import org.sonar.db.Dao;
import org.sonar.db.Database;
import org.sonar.db.MyBatis;
import org.sonar.server.qualityprofile.db.ActiveRuleDao;
import org.sonar.server.rule.db.RuleDao;

/**
 * Should be replaced by {@link org.sonar.db.DbClient}, but some DAOs
 * still depend on other sonar-server classes.
 * @deprecated since 5.2 - should be removed as soon as the remaining daos are moved to sonar-db
 */
@Deprecated
public class DbClient extends org.sonar.db.DbClient {

  private ActiveRuleDao activeRuleDao;
  private RuleDao ruleDao;

  public DbClient(Database database, MyBatis myBatis, Dao... daos) {
    super(database, myBatis, daos);
  }

  @Override
  protected void doOnLoad(Map<Class, Dao> daoByClass) {
    this.activeRuleDao = (ActiveRuleDao) daoByClass.get(ActiveRuleDao.class);
    this.ruleDao = (RuleDao) daoByClass.get(RuleDao.class);
  }

  public ActiveRuleDao activeRuleDao() {
    return activeRuleDao;
  }

  public RuleDao deprecatedRuleDao() {
    return ruleDao;
  }
}
