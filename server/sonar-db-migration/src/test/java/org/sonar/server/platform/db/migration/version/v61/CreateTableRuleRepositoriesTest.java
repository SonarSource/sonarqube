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
package org.sonar.server.platform.db.migration.version.v61;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class CreateTableRuleRepositoriesTest {
  private static final String TABLE_RULE_REPOSITORIES = "rule_repositories";

  @Rule
  public final CoreDbTester dbTester = CoreDbTester.createForSchema(CreateTableRuleRepositoriesTest.class, "empty.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private CreateTableRuleRepositories underTest = new CreateTableRuleRepositories(dbTester.database());

  @Test
  public void creates_table_on_empty_db() throws SQLException {
    underTest.execute();

    assertThat(dbTester.countRowsOfTable(TABLE_RULE_REPOSITORIES)).isEqualTo(0);

    dbTester.assertColumnDefinition(TABLE_RULE_REPOSITORIES, "kee", Types.VARCHAR, 200, false);
    dbTester.assertColumnDefinition(TABLE_RULE_REPOSITORIES, "language", Types.VARCHAR, 20, false);
    dbTester.assertColumnDefinition(TABLE_RULE_REPOSITORIES, "name", Types.VARCHAR, 4000, false);
    dbTester.assertColumnDefinition(TABLE_RULE_REPOSITORIES, "created_at", Types.BIGINT, null, false);
    dbTester.assertPrimaryKey(TABLE_RULE_REPOSITORIES, "pk_" + TABLE_RULE_REPOSITORIES, "kee");
  }

  @Test
  public void migration_is_not_reentrant() throws SQLException {
    underTest.execute();

    expectedException.expect(IllegalStateException.class);

    underTest.execute();
  }
}
