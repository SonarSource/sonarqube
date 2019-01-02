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
package org.sonar.server.platform.db.migration.version.v62;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class CreateTableOrganizationsTest {
  private static final String TABLE_ORGANIZATIONS = "organizations";

  @Rule
  public final CoreDbTester dbTester = CoreDbTester.createForSchema(CreateTableOrganizationsTest.class, "empty.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private CreateTableOrganizations underTest = new CreateTableOrganizations(dbTester.database());

  @Test
  public void creates_table_on_empty_db() throws SQLException {
    underTest.execute();

    assertThat(dbTester.countRowsOfTable(TABLE_ORGANIZATIONS)).isEqualTo(0);

    dbTester.assertColumnDefinition(TABLE_ORGANIZATIONS, "uuid", Types.VARCHAR, 40, false);
    dbTester.assertColumnDefinition(TABLE_ORGANIZATIONS, "kee", Types.VARCHAR, 32, false);
    dbTester.assertColumnDefinition(TABLE_ORGANIZATIONS, "name", Types.VARCHAR, 64, false);
    dbTester.assertColumnDefinition(TABLE_ORGANIZATIONS, "description", Types.VARCHAR, 256, true);
    dbTester.assertColumnDefinition(TABLE_ORGANIZATIONS, "url", Types.VARCHAR, 256, true);
    dbTester.assertColumnDefinition(TABLE_ORGANIZATIONS, "avatar_url", Types.VARCHAR, 256, true);
    dbTester.assertColumnDefinition(TABLE_ORGANIZATIONS, "created_at", Types.BIGINT, null, false);
    dbTester.assertColumnDefinition(TABLE_ORGANIZATIONS, "updated_at", Types.BIGINT, null, false);
    dbTester.assertPrimaryKey(TABLE_ORGANIZATIONS, "pk_" + TABLE_ORGANIZATIONS, "uuid");
  }

  @Test
  public void migration_is_not_reentrant() throws SQLException {
    underTest.execute();

    expectedException.expect(IllegalStateException.class);

    underTest.execute();
  }

}
