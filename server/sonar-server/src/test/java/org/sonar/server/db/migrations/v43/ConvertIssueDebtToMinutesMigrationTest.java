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

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ConvertIssueDebtToMinutesMigrationTest {

  @ClassRule
  public static DbTester db = new DbTester().schema(ConvertIssueDebtToMinutesMigrationTest.class, "schema.sql");

  @Mock
  System2 system2;

  @Mock
  PropertiesDao propertiesDao;

  ConvertIssueDebtToMinutesMigration migration;

  @Before
  public void setUp() throws Exception {
    when(system2.now()).thenReturn(DateUtils.parseDateTime("2014-02-19T19:10:03+0100").getTime());
    when(propertiesDao.selectGlobalProperty(WorkDurationConvertor.HOURS_IN_DAY_PROPERTY)).thenReturn(new PropertyDto().setValue("8"));

    migration = new ConvertIssueDebtToMinutesMigration(db.database(), propertiesDao, system2);
  }

  @Test
  public void migrate_issues_debt() throws Exception {
    db.prepareDbUnit(getClass(), "migrate_issues_debt.xml");

    migration.execute();

    db.assertDbUnit(getClass(), "migrate_issues_debt_result.xml", "issues");
  }

}
