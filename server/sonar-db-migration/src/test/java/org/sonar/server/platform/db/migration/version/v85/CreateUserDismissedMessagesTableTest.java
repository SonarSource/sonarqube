/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v85;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;

import static java.sql.Types.BIGINT;
import static java.sql.Types.VARCHAR;

public class CreateUserDismissedMessagesTableTest {
  private static final String TABLE_NAME = "user_dismissed_messages";

  @Rule
  public CoreDbTester dbTester = CoreDbTester.createEmpty();

  private final CreateUserDismissedMessagesTable underTest = new CreateUserDismissedMessagesTable(dbTester.database());

  @Test
  public void table_has_been_created() throws SQLException {
    underTest.execute();

    dbTester.assertTableExists(TABLE_NAME);
    dbTester.assertPrimaryKey(TABLE_NAME, "pk_user_dismissed_messages", "uuid");
    dbTester.assertColumnDefinition(TABLE_NAME, "uuid", VARCHAR, 40, false);
    dbTester.assertColumnDefinition(TABLE_NAME, "user_uuid", VARCHAR, 255, false);
    dbTester.assertColumnDefinition(TABLE_NAME, "project_uuid", VARCHAR, 40, false);
    dbTester.assertColumnDefinition(TABLE_NAME, "message_type", VARCHAR, 255, false);
    dbTester.assertColumnDefinition(TABLE_NAME, "created_at", BIGINT, null, false);
    dbTester.assertUniqueIndex(TABLE_NAME, "uniq_user_dismissed_messages", "user_uuid",
      "project_uuid", "message_type");
    dbTester.assertIndex(TABLE_NAME, "udm_project_uuid", "project_uuid");
    dbTester.assertIndex(TABLE_NAME, "udm_message_type", "message_type");
  }

}
