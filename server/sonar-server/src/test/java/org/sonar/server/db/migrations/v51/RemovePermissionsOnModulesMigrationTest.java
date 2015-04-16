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
package org.sonar.server.db.migrations.v51;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.db.migrations.MigrationStep;

public class RemovePermissionsOnModulesMigrationTest {

  @ClassRule
  public static DbTester db = new DbTester().schema(RemovePermissionsOnModulesMigrationTest.class, "schema.sql");

  MigrationStep migration;

  @Before
  public void setUp() throws Exception {
    migration = new RemovePermissionsOnModulesMigrationStep(db.database());
  }

  @Test
  public void execute() throws Exception {
    db.prepareDbUnit(getClass(), "migrate.xml");

    migration.execute();

    db.assertDbUnit(getClass(), "migrate-result.xml", "user_roles", "group_roles");
  }

  @Test
  public void nothing_to_do_when_already_migrated() throws Exception {
    db.prepareDbUnit(getClass(), "nothing_to_do_when_already_migrated.xml");

    migration.execute();

    db.assertDbUnit(getClass(), "nothing_to_do_when_already_migrated.xml", "user_roles", "group_roles");
  }

}
