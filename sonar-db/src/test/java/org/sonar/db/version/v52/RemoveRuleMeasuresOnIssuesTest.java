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

package org.sonar.db.version.v52;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.version.MigrationStep;

public class RemoveRuleMeasuresOnIssuesTest {

  @ClassRule
  public static DbTester db = DbTester.createForSchema(System2.INSTANCE, RemoveRuleMeasuresOnIssuesTest.class, "schema.sql");

  MigrationStep migration;

  @Before
  public void setUp() throws Exception {
    db.executeUpdateSql("TRUNCATE TABLE metrics");
    db.executeUpdateSql("TRUNCATE TABLE project_measures");
    migration = new RemoveRuleMeasuresOnIssues(db.database());
  }

  @Test
  public void execute() throws Exception {
    db.prepareDbUnit(getClass(), "execute.xml");

    migration.execute();

    db.assertDbUnit(getClass(), "execute-result.xml", "project_measures");
  }

  @Test
  public void execute_nothing() throws Exception {
    db.prepareDbUnit(getClass(), "execute_nothing.xml");

    migration.execute();

    db.assertDbUnit(getClass(), "execute_nothing.xml", "project_measures");
  }

}
