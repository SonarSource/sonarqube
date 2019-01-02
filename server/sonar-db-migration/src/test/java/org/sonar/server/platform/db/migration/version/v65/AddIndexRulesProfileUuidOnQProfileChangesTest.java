/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.sonar.db.CoreDbTester;

public class AddIndexRulesProfileUuidOnQProfileChangesTest {

  private static final String TABLE_NAME = "qprofile_changes";
  private static final String COLUMN_NAME = "rules_profile_uuid";
  private static final String INDEX_NAME = "qp_changes_rules_profile_uuid";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(AddIndexRulesProfileUuidOnQProfileChangesTest.class, "initial.sql");

  private AddIndexRulesProfileUuidOnQProfileChanges underTest = new AddIndexRulesProfileUuidOnQProfileChanges(db.database());

  @Test
  public void add_index_ON_RULES_PROFILE_UUID_of_QPROFILE_CHANGES() throws SQLException {
    underTest.execute();

    db.assertIndex(TABLE_NAME, INDEX_NAME,COLUMN_NAME);
  }
}
