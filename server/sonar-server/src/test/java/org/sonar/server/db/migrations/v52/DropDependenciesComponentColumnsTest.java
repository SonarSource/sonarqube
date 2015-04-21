/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.db.migrations.v52;

import org.junit.Before;
import org.junit.Test;
import org.sonar.core.persistence.Database;
import org.sonar.core.persistence.dialect.PostgreSql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DropDependenciesComponentColumnsTest {

  DropDependenciesComponentColumns migration;

  Database database;

  @Before
  public void setUp() throws Exception {
    database = mock(Database.class);
    migration = new DropDependenciesComponentColumns(database);
  }

  @Test
  public void generate_sql_on_postgresql() throws Exception {
    when(database.getDialect()).thenReturn(new PostgreSql());
    assertThat(migration.generateSql()).isEqualTo(
      "ALTER TABLE dependencies DROP COLUMN from_resource_id, DROP COLUMN to_resource_id"
      );
  }

}
