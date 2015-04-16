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

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.core.UtcDateUtils;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.db.DbClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FeedQProfileDatesMigrationTest {

  @ClassRule
  public static DbTester db = new DbTester().schema(FeedQProfileDatesMigrationTest.class, "schema.sql");

  FeedQProfileDatesMigrationStep migration;

  @Before
  public void setUp() throws Exception {
    db.executeUpdateSql("truncate table active_rule_changes");
    db.executeUpdateSql("truncate table active_rule_param_changes");
    DbClient dbClient = new DbClient(db.database(), db.myBatis());
    System2 system = mock(System2.class);
    when(system.now()).thenReturn(UtcDateUtils.parseDateTime("2014-07-03T12:00:00+0000").getTime());
    migration = new FeedQProfileDatesMigrationStep(dbClient, system);
  }

  @Test
  public void feed_created_at_and_updated_at() throws Exception {
    db.prepareDbUnit(getClass(), "feed_created_at_and_updated_at.xml");

    migration.execute();

    Map<String, Object> columns = db.selectFirst("select created_at as \"createdAt\", updated_at as \"updatedAt\" from rules_profiles where id=10");
    assertThat(columns.get("createdAt").toString()).startsWith("2011-12-2");
    assertThat(columns.get("updatedAt").toString()).startsWith("2014-01-2");
  }

  @Test
  public void use_default_dates_when_no_changes() throws Exception {
    db.prepareDbUnit(getClass(), "use_default_dates_when_no_changes.xml");

    migration.execute();

    Map<String, Object> columns = db.selectFirst("select created_at as \"createdAt\", updated_at as \"updatedAt\" from rules_profiles where id=10");
    assertThat(columns.get("createdAt").toString()).startsWith("2014-07-0");
    assertThat(columns.get("updatedAt").toString()).startsWith("2014-07-0");
  }

}
