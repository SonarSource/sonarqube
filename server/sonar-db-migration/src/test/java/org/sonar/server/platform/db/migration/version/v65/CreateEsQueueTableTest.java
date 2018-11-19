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

import static org.assertj.core.api.Assertions.assertThat;

public class CreateEsQueueTableTest {

  private static final String TABLE = "es_queue";

  @Rule
  public final CoreDbTester db = CoreDbTester.createForSchema(CreateEsQueueTableTest.class, "empty.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private CreateEsQueueTable underTest = new CreateEsQueueTable(db.database());

  @Test
  public void creates_table_on_empty_db() throws SQLException {
    underTest.execute();

    assertThat(db.countRowsOfTable(TABLE)).isEqualTo(0);

    db.assertColumnDefinition(TABLE, "uuid", Types.VARCHAR, 40, false);
    db.assertColumnDefinition(TABLE, "doc_type", Types.VARCHAR, 40, false);
    db.assertColumnDefinition(TABLE, "doc_id", Types.VARCHAR, 4000, false);
    db.assertColumnDefinition(TABLE, "doc_id_type", Types.VARCHAR, 20, true);
    db.assertColumnDefinition(TABLE, "doc_routing", Types.VARCHAR, 4000, true);
    db.assertColumnDefinition(TABLE, "created_at", Types.BIGINT, null, false);
  }
}
