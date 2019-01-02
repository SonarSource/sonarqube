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

public class DropOrgColumnsFromRulesProfilesTest {

  private static final String TABLE_NAME = "rules_profiles";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(DropOrgColumnsFromRulesProfilesTest.class, "initial.sql");

  private DropOrgColumnsFromRulesProfiles underTest = new DropOrgColumnsFromRulesProfiles(db.database());

  @Test
  public void columns_are_dropped() throws SQLException {
    underTest.execute();

    db.assertColumnDoesNotExist(TABLE_NAME, "organization_uuid");
    db.assertColumnDoesNotExist(TABLE_NAME, "parent_kee");
    db.assertColumnDoesNotExist(TABLE_NAME, "last_used");
    db.assertColumnDoesNotExist(TABLE_NAME, "user_updated_at");
  }
}
