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

import java.sql.Types;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;

public class RenameQProfileKeyToRulesProfileUuidOnQProfileChangesTest {

  private static final String TABLE_NAME = "qprofile_changes";
  private static final String OLD_COLUMN_NAME = "qprofile_key";
  private static final String NEW_COLUMN_NAME = "rules_profile_uuid";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(RenameQProfileKeyToRulesProfileUuidOnQProfileChangesTest.class, "initial.sql");

  private RenameQProfileKeyToRulesProfileUuidOnQProfileChanges underTest = new RenameQProfileKeyToRulesProfileUuidOnQProfileChanges(db.database());

  @Test
  public void change_column_name_from_QPROFILE_KEY_to_RULES_PROFILE_UUID() throws Exception {
    db.assertColumnDefinition(TABLE_NAME, OLD_COLUMN_NAME, Types.VARCHAR, 255);
    underTest.execute();
    db.assertColumnDefinition(TABLE_NAME, NEW_COLUMN_NAME, Types.VARCHAR, 255);
    db.assertColumnDoesNotExist(TABLE_NAME, OLD_COLUMN_NAME);
  }
}
