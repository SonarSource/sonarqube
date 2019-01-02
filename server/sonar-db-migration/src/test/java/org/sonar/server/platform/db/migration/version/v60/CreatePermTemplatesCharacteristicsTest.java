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
package org.sonar.server.platform.db.migration.version.v60;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class CreatePermTemplatesCharacteristicsTest {

  private static final String TABLE_NAME = "perm_tpl_characteristics";

  @Rule
  public final CoreDbTester dbTester = CoreDbTester.createForSchema(CreatePermTemplatesCharacteristicsTest.class, "empty.sql");

  private CreatePermTemplatesCharacteristics underTest = new CreatePermTemplatesCharacteristics(dbTester.database());

  @Test
  public void creates_table_and_index() throws SQLException {
    underTest.execute();

    assertThat(dbTester.countRowsOfTable(TABLE_NAME)).isEqualTo(0);
    dbTester.assertColumnDefinition(TABLE_NAME, "id", Types.INTEGER, null, false);
    dbTester.assertPrimaryKey(TABLE_NAME, "pk_" + TABLE_NAME, "id");
    dbTester.assertColumnDefinition(TABLE_NAME, "template_id", Types.INTEGER, null);
    dbTester.assertColumnDefinition(TABLE_NAME, "permission_key", Types.VARCHAR, 64, false);
    dbTester.assertColumnDefinition(TABLE_NAME, "with_project_creator", Types.BOOLEAN, null, false);
    dbTester.assertColumnDefinition(TABLE_NAME, "created_at", Types.BIGINT, null, false);
    dbTester.assertColumnDefinition(TABLE_NAME, "updated_at", Types.BIGINT, null, false);

    dbTester.assertUniqueIndex(TABLE_NAME, "uniq_perm_tpl_charac", "template_id", "permission_key");
  }

}
