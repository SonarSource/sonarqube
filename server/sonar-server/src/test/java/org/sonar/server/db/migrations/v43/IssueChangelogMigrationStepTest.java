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

package org.sonar.server.db.migrations.v43;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.persistence.DbTester;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.core.properties.PropertyDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IssueChangelogMigrationStepTest {

  @ClassRule
  public static DbTester db = new DbTester().schema(IssueChangelogMigrationStepTest.class, "schema.sql");

  @Mock
  System2 system2;

  @Mock
  PropertiesDao propertiesDao;

  IssueChangelogMigrationStep migration;

  @Before
  public void setUp() throws Exception {
    when(system2.now()).thenReturn(DateUtils.parseDateTime("2014-02-19T19:10:03+0100").getTime());
    when(propertiesDao.selectGlobalProperty(WorkDurationConvertor.HOURS_IN_DAY_PROPERTY)).thenReturn(new PropertyDto().setValue("8"));

    WorkDurationConvertor convertor = new WorkDurationConvertor(propertiesDao);
    convertor.init();
    migration = new IssueChangelogMigrationStep(db.database(), system2, convertor);
  }

  @Test
  public void migrate_issue_changelog_debt() throws Exception {
    db.prepareDbUnit(getClass(), "migrate_issue_changelog_debt.xml");

    migration.execute();

    db.assertDbUnit(getClass(), "migrate_issue_changelog_debt_result.xml", new String[]{"updated_at"}, "issue_changes");
  }

  @Test
  public void convert_data_containing_only_debt_change() throws Exception {
    assertThat(migration.convertChangelog("technicalDebt=1|2")).isEqualTo("technicalDebt=1|2");
    assertThat(migration.convertChangelog("technicalDebt=100|200")).isEqualTo("technicalDebt=60|120");
    assertThat(migration.convertChangelog("technicalDebt=10000|20000")).isEqualTo("technicalDebt=480|960");

    assertThat(migration.convertChangelog("technicalDebt=|2")).isEqualTo("technicalDebt=|2");
    assertThat(migration.convertChangelog("technicalDebt=1|")).isEqualTo("technicalDebt=1|");
  }

  @Test
  public void convert_data_beginning_with_debt_change() throws Exception {
    assertThat(migration.convertChangelog("technicalDebt=100|200,status=RESOLVED|REOPENED")).isEqualTo("technicalDebt=60|120,status=RESOLVED|REOPENED");
    assertThat(migration.convertChangelog("technicalDebt=|200,status=RESOLVED|REOPENED")).isEqualTo("technicalDebt=|120,status=RESOLVED|REOPENED");
    assertThat(migration.convertChangelog("technicalDebt=100|,status=RESOLVED|REOPENED")).isEqualTo("technicalDebt=60|,status=RESOLVED|REOPENED");
  }

  @Test
  public void convert_data_finishing_with_debt_change() throws Exception {
    assertThat(migration.convertChangelog("status=RESOLVED|REOPENED,technicalDebt=100|200")).isEqualTo("status=RESOLVED|REOPENED,technicalDebt=60|120");
    assertThat(migration.convertChangelog("status=RESOLVED|REOPENED,technicalDebt=|200")).isEqualTo("status=RESOLVED|REOPENED,technicalDebt=|120");
    assertThat(migration.convertChangelog("status=RESOLVED|REOPENED,technicalDebt=100|")).isEqualTo("status=RESOLVED|REOPENED,technicalDebt=60|");
  }

  @Test
  public void convert_data_with_debt_change_in_the_middle() throws Exception {
    assertThat(migration.convertChangelog("status=RESOLVED|REOPENED,technicalDebt=100|200,resolution=")).isEqualTo("status=RESOLVED|REOPENED,technicalDebt=60|120,resolution=");
    assertThat(migration.convertChangelog("status=RESOLVED|REOPENED,technicalDebt=|200,resolution=")).isEqualTo("status=RESOLVED|REOPENED,technicalDebt=|120,resolution=");
    assertThat(migration.convertChangelog("status=RESOLVED|REOPENED,technicalDebt=100|,resolution=")).isEqualTo("status=RESOLVED|REOPENED,technicalDebt=60|,resolution=");
  }
}
