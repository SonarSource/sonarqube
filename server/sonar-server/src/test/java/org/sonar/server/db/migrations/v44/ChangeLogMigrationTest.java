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
package org.sonar.server.db.migrations.v44;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.activity.db.ActivityDao;
import org.sonar.server.db.DbClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ChangeLogMigrationTest {

  @ClassRule
  public static DbTester db = new DbTester().schema(ChangeLogMigrationTest.class, "schema.sql");

  System2 system2 = mock(System2.class);
  DbClient dbClient;
  ActivityDao dao;
  ChangeLogMigrationStep migration;
  DbSession session;

  @Before
  public void setUp() throws Exception {
    when(system2.now()).thenReturn(DateUtils.parseDate("2014-03-13").getTime());
    dao = new ActivityDao(db.myBatis(), system2);
    dbClient = new DbClient(db.database(), db.myBatis(), dao);
    migration = new ChangeLogMigrationStep(dao, dbClient);
    session = dbClient.openSession(false);
  }

  @After
  public void tearDown() {
    session.close();
  }

  @Test
  public void migrate() throws Exception {
    db.prepareDbUnit(getClass(), "active_rules_changes.xml");
    migration.execute();
    assertThat(db.countRowsOfTable("activities")).isEqualTo(5);

    int count = db.countSql("select count(*) from activities where data_field like '%param_PARAM1=TODO%'");
    assertThat(count).isGreaterThan(0);
  }

  @Test
  public void migrate_when_no_changelog() throws Exception {
    db.prepareDbUnit(getClass(), "migrate_when_no_changelog.xml");
    migration.execute();

    assertThat(db.countRowsOfTable("activities")).isEqualTo(0);
  }
}
