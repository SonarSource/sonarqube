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
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.core.persistence.DbTester;

@RunWith(MockitoJUnitRunner.class)
public class MeasureDataMigrationTest {

  @ClassRule
  public static DbTester db = new DbTester().schema(MeasureDataMigrationTest.class, "schema.sql");

  MeasureDataMigrationStep migration;

  @Before
  public void setUp() throws Exception {
    migration = new MeasureDataMigrationStep(db.database());
  }

  @Test
  public void migrate_issues_action_plan_key() throws Exception {
    db.prepareDbUnit(getClass(), "migrate_measure_data.xml");

    migration.execute();

    db.assertDbUnit(getClass(), "migrate_measure_data-result.xml", "project_measures");
  }

}
