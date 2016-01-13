/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.db.version.v50;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.version.MigrationStep;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReplaceIssueFiltersProjectKeyByUuidTest {

  @Rule
  public DbTester db = DbTester.createForSchema(System2.INSTANCE, ReplaceIssueFiltersProjectKeyByUuidTest.class, "schema.sql");

  MigrationStep migration;
  System2 system = mock(System2.class);

  @Before
  public void setUp() {
    db.executeUpdateSql("truncate table issue_filters");
    migration = new ReplaceIssueFiltersProjectKeyByUuid(db.database(), system);
    when(system.now()).thenReturn(DateUtils.parseDate("2014-10-29").getTime());
  }

  @Test
  public void execute() throws Exception {
    db.prepareDbUnit(getClass(), "execute.xml");

    migration.execute();

    db.assertDbUnit(getClass(), "execute-result.xml", "issue_filters");
  }

  @Test
  public void do_not_execute_if_already_migrated() throws Exception {
    db.prepareDbUnit(getClass(), "do_not_execute_if_already_migrated.xml");

    migration.execute();

    db.assertDbUnit(getClass(), "do_not_execute_if_already_migrated-result.xml", "issue_filters");
  }

}
