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

import org.sonar.api.DaoComponent;
import org.sonar.api.ServerComponent;
import org.sonar.core.persistence.Database;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.qualityprofile.db.QualityProfileDao;
import org.sonar.server.qualityprofile.persistence.ActiveRuleDao;
import org.sonar.server.rule2.persistence.RuleDao;

import java.util.HashMap;
import java.util.Map;

/**
 * Facade for all db components
 */
public class DbClient implements ServerComponent {

  private final Database db;
  private final MyBatis myBatis;
  private final Map<Class<?>, DaoComponent> daoComponents;

  public DbClient(Database db, MyBatis myBatis, DaoComponent... daoComponents) {
    this.db = db;
    this.myBatis = myBatis;
    this.daoComponents = new HashMap<Class<?>, DaoComponent>();

    for(DaoComponent daoComponent : daoComponents){
      this.daoComponents.put(daoComponent.getClass(), daoComponent);
    }
  }

  public Database database() {
    return db;
  }

  public DbSession openSession(boolean batch) {
    return myBatis.openSession(batch);
  }

  public <K> K getDao(Class<K> clazz){
    return (K) this.daoComponents.get(clazz);
  }

  public RuleDao ruleDao() {
    return this.getDao(RuleDao.class);
  }

  public ActiveRuleDao activeRuleDao() {
    return this.getDao(ActiveRuleDao.class);
  }

  public QualityProfileDao qualityProfileDao() {
    return this.getDao(QualityProfileDao.class);
  }
}
