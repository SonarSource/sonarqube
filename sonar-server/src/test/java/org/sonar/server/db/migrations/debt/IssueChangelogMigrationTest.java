/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

package org.sonar.server.db.migrations.debt;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.persistence.TestDatabase;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IssueChangelogMigrationTest {

  @ClassRule
  public static TestDatabase db = new TestDatabase().schema(IssueChangelogMigrationTest.class, "schema.sql");

  @Mock
  System2 system2;

  Settings settings;

  IssueChangelogMigration migration;

  @Before
  public void setUp() throws Exception {
    when(system2.now()).thenReturn(DateUtils.parseDate("2014-02-19").getTime());
    settings = new Settings();
    settings.setProperty(DebtConvertor.HOURS_IN_DAY_PROPERTY, 8);

    migration = new IssueChangelogMigration(db.database(), settings, system2);
  }

  @Test
  public void migrate_issue_changelog_debt() throws Exception {
    db.prepareDbUnit(getClass(), "migrate_issue_changelog_debt.xml");

    migration.execute();

    db.assertDbUnit(getClass(), "migrate_issue_changelog_debt_result.xml", "issue_changes");
  }

  @Test
  public void convert_data_containing_only_debt_change() throws Exception {
    assertThat(migration.convertChangelog("technicalDebt=1|2")).isEqualTo("technicalDebt=60|120");
    assertThat(migration.convertChangelog("technicalDebt=100|200")).isEqualTo("technicalDebt=3600|7200");
    assertThat(migration.convertChangelog("technicalDebt=10000|20000")).isEqualTo("technicalDebt=28800|57600");

    assertThat(migration.convertChangelog("technicalDebt=|2")).isEqualTo("technicalDebt=|120");
    assertThat(migration.convertChangelog("technicalDebt=1|")).isEqualTo("technicalDebt=60|");
  }

  @Test
  public void convert_data_beginning_with_debt_change() throws Exception {
    assertThat(migration.convertChangelog("technicalDebt=100|200,status=RESOLVED|REOPENED")).isEqualTo("technicalDebt=3600|7200,status=RESOLVED|REOPENED");
    assertThat(migration.convertChangelog("technicalDebt=|200,status=RESOLVED|REOPENED")).isEqualTo("technicalDebt=|7200,status=RESOLVED|REOPENED");
    assertThat(migration.convertChangelog("technicalDebt=100|,status=RESOLVED|REOPENED")).isEqualTo("technicalDebt=3600|,status=RESOLVED|REOPENED");
  }

  @Test
  public void convert_data_finishing_with_debt_change() throws Exception {
    assertThat(migration.convertChangelog("status=RESOLVED|REOPENED,technicalDebt=100|200")).isEqualTo("status=RESOLVED|REOPENED,technicalDebt=3600|7200");
    assertThat(migration.convertChangelog("status=RESOLVED|REOPENED,technicalDebt=|200")).isEqualTo("status=RESOLVED|REOPENED,technicalDebt=|7200");
    assertThat(migration.convertChangelog("status=RESOLVED|REOPENED,technicalDebt=100|")).isEqualTo("status=RESOLVED|REOPENED,technicalDebt=3600|");
  }

  @Test
  public void convert_data_with_debt_change_in_the_middle() throws Exception {
    assertThat(migration.convertChangelog("status=RESOLVED|REOPENED,technicalDebt=100|200,resolution=")).isEqualTo("status=RESOLVED|REOPENED,technicalDebt=3600|7200,resolution=");
    assertThat(migration.convertChangelog("status=RESOLVED|REOPENED,technicalDebt=|200,resolution=")).isEqualTo("status=RESOLVED|REOPENED,technicalDebt=|7200,resolution=");
    assertThat(migration.convertChangelog("status=RESOLVED|REOPENED,technicalDebt=100|,resolution=")).isEqualTo("status=RESOLVED|REOPENED,technicalDebt=3600|,resolution=");
  }
}
