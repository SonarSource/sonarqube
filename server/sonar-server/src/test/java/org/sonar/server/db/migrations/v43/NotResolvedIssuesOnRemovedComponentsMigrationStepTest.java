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

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NotResolvedIssuesOnRemovedComponentsMigrationStepTest {

  @ClassRule
  public static DbTester db = new DbTester().schema(NotResolvedIssuesOnRemovedComponentsMigrationStepTest.class, "schema.sql");

  @Mock
  System2 system2;

  NotResolvedIssuesOnRemovedComponentsMigrationStep migration;

  @Before
  public void setUp() throws Exception {
    when(system2.now()).thenReturn(DateUtils.parseDate("2014-04-09").getTime());

    migration = new NotResolvedIssuesOnRemovedComponentsMigrationStep(db.database(), system2);
  }

  @Test
  public void migrate_issues() throws Exception {
    db.prepareDbUnit(getClass(), "migrate_issues.xml");

    migration.execute();

    db.assertDbUnit(getClass(), "migrate_issues_result.xml", "issues");
  }

}
