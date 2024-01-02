/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v92;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;

public class CreateProjectBadgeTokenTableTest {

  private static final String TABLE_NAME = "project_badge_token";

  @Rule
  public final CoreDbTester db = CoreDbTester.createForSchema(CreateProjectBadgeTokenTableTest.class, "schema.sql");

  private final DdlChange underTest = new CreateProjectBadgeTokenTable(db.database());

  @Test
  public void migration_should_create_a_table_with_index() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();

    db.assertTableExists(TABLE_NAME);
    db.assertPrimaryKey(TABLE_NAME, "pk_project_badge_token", "uuid");
    db.assertColumnDefinition(TABLE_NAME, "uuid", Types.VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, "token", Types.VARCHAR, 255, false);
    db.assertColumnDefinition(TABLE_NAME, "project_uuid", Types.VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, "updated_at", Types.BIGINT, 20, false);
    db.assertColumnDefinition(TABLE_NAME, "created_at", Types.BIGINT, 20, false);
  }

  @Test
  public void migration_should_be_reentrant() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();
    //re-entrant
    underTest.execute();

    db.assertTableExists(TABLE_NAME);
  }
}
