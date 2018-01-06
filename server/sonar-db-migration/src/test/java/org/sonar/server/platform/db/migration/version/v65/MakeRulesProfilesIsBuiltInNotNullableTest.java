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
import java.sql.Types;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.CoreDbTester;


public class MakeRulesProfilesIsBuiltInNotNullableTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(MakeRulesProfilesIsBuiltInNotNullableTest.class, "initial.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private MakeRulesProfilesIsBuiltInNotNullable underTest = new MakeRulesProfilesIsBuiltInNotNullable(db.database());

  @Test
  public void execute_makes_column_not_null() throws SQLException {
    db.assertColumnDefinition("rules_profiles", "is_built_in", Types.BOOLEAN, null, true);
    insertRow(1);
    insertRow(2);

    underTest.execute();

    db.assertColumnDefinition("rules_profiles", "is_built_in", Types.BOOLEAN, null, false);
  }

  private void insertRow(int id) {
    db.executeInsert(
      "RULES_PROFILES",
      "NAME", "name_" + id,
      "ORGANIZATION_UUID", "org_" + id,
      "KEE", "kee" + id,
      "IS_DEFAULT", false,
      "IS_BUILT_IN", false);
  }

}
