/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.server.platform.db.migration.version.v65;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.CoreDbTester;

import static java.sql.Types.BOOLEAN;


public class AddBuiltInFlagToRulesProfilesTest {
  @Rule
  public final CoreDbTester dbTester = CoreDbTester.createForSchema(AddBuiltInFlagToRulesProfilesTest.class, "rules_profiles_6_4.sql");

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private AddBuiltInFlagToRulesProfiles underTest = new AddBuiltInFlagToRulesProfiles(dbTester.database());

  @Test
  public void column_is_added_to_table() throws SQLException {
    underTest.execute();

    dbTester.assertColumnDefinition("rules_profiles", "is_built_in", BOOLEAN, null, true);
  }

  @Test
  public void migration_is_not_reentrant() throws SQLException {
    underTest.execute();

    expectedException.expect(IllegalStateException.class);

    underTest.execute();
  }

}
