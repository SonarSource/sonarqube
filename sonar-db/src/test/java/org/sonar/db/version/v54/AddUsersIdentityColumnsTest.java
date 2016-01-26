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
package org.sonar.db.version.v54;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.version.MigrationStep;

import static java.sql.Types.VARCHAR;

public class AddUsersIdentityColumnsTest {

  @Rule
  public DbTester db = DbTester.createForSchema(System2.INSTANCE, AddUsersIdentityColumnsTest.class, "schema.sql");

  MigrationStep migration;

  @Before
  public void setUp() {
    migration = new AddUsersIdentityColumns(db.database());
  }

  @Test
  public void update_columns() throws Exception {
    migration.execute();

    db.assertColumnDefinition("users", "external_identity_provider", VARCHAR, 100);
    db.assertColumnDefinition("users", "external_identity", VARCHAR, 255);
  }

}
