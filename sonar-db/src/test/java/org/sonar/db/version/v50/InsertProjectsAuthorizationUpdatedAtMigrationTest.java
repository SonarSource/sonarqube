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

package org.sonar.db.version.v50;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.version.MigrationStep;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InsertProjectsAuthorizationUpdatedAtMigrationTest {

  @Rule
  public DbTester db = DbTester.createForSchema(System2.INSTANCE, InsertProjectsAuthorizationUpdatedAtMigrationTest.class, "schema.sql");

  MigrationStep migration;
  System2 system = mock(System2.class);

  @Before
  public void setUp() {
    db.executeUpdateSql("truncate table projects");
    migration = new InsertProjectsAuthorizationUpdatedAtMigrationStep(db.database(), system);
    when(system.now()).thenReturn(123456789L);
  }

  @Test
  public void execute() throws Exception {
    db.prepareDbUnit(getClass(), "before.xml");

    migration.execute();

    db.assertDbUnit(getClass(), "after.xml", "projects");
  }

}
