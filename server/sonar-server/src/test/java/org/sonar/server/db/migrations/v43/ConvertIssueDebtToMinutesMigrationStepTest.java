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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ConvertIssueDebtToMinutesMigrationStepTest {

  @ClassRule
  public static DbTester db = new DbTester().schema(ConvertIssueDebtToMinutesMigrationStepTest.class, "schema.sql");

  @Mock
  System2 system2;

  @Mock
  PropertiesDao propertiesDao;

  ConvertIssueDebtToMinutesMigrationStep migration;

  @Before
  public void setUp() throws Exception {
    when(system2.now()).thenReturn(DateUtils.parseDateTime("2014-02-19T19:10:03+0100").getTime());
    when(propertiesDao.selectGlobalProperty(WorkDurationConvertor.HOURS_IN_DAY_PROPERTY)).thenReturn(new PropertyDto().setValue("8"));

    migration = new ConvertIssueDebtToMinutesMigrationStep(db.database(), propertiesDao, system2);
  }

  @Test
  public void migrate_issues_debt() throws Exception {
    db.prepareDbUnit(getClass(), "migrate_issues_debt.xml");

    migration.execute();

    db.assertDbUnit(getClass(), "migrate_issues_debt_result.xml", new String[] {"updated_at"}, "issues");

    // Check only updated_at columns values, but we could also remove db unit file and check all fields
    List<Map<String, Object>> results = db.select("select updated_at as \"updatedAt\" from issues");
    assertThat(results).hasSize(5);
    assertThat(results.get(0).get("updatedAt").toString()).startsWith("2014");
    assertThat(results.get(1).get("updatedAt").toString()).startsWith("2014");
    assertThat(results.get(2).get("updatedAt").toString()).startsWith("2014");
    assertThat(results.get(3).get("updatedAt").toString()).startsWith("2014");
    // Not updated because no debt
    assertThat(results.get(4).get("updatedAt").toString()).startsWith("2012");
  }

}
