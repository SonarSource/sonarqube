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

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.utils.System2;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.DbTester;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.qualityprofile.db.QualityProfileDao;
import org.sonar.server.qualityprofile.db.ActiveRuleDao;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;

@Category(DbTests.class)
public class DbClientTest {

  @Rule
  public DbTester db = new DbTester();

  @Test
  public void facade() throws Exception {
    MyBatis myBatis = db.myBatis();
    RuleDao ruleDao = new RuleDao(System2.INSTANCE);
    QualityProfileDao qualityProfileDao = new QualityProfileDao(myBatis, System2.INSTANCE);
    ActiveRuleDao activeRuleDao = new ActiveRuleDao(qualityProfileDao, ruleDao, System2.INSTANCE);

    DbClient client = new DbClient(db.database(), myBatis, ruleDao, activeRuleDao, qualityProfileDao);

    assertThat(client.database()).isSameAs(db.database());
    DbSession dbSession = client.openSession(true);
    assertThat(dbSession).isNotNull();
    assertThat(dbSession.getConnection().isClosed()).isFalse();
    dbSession.close();

    // DAO
    assertThat(client.qualityProfileDao()).isSameAs(qualityProfileDao);
    assertThat(client.activeRuleDao()).isSameAs(activeRuleDao);
    assertThat(client.ruleDao()).isSameAs(ruleDao);
  }
}
