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
package org.sonar.server.platform.db.migration.version.v64;

import java.sql.SQLException;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class PopulateColumnProjectsPrivateTest {
  private static final Boolean NO_PRIVATE_FLAG = null;

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateColumnProjectsPrivateTest.class, "projects_with_nullable_private_column.sql");

  private PopulateColumnProjectsPrivate underTest = new PopulateColumnProjectsPrivate(db.database());

  @Test
  public void execute_has_no_effect_if_table_PROJECTS_is_empty() throws SQLException {
    underTest.execute();

    assertThat(db.countRowsOfTable("PROJECTS")).isEqualTo(0);
  }

  @Test
  public void execute_updates_rows_with_null_private_column_to_false() throws SQLException {
    insertRow(1, NO_PRIVATE_FLAG);
    insertRow(2, NO_PRIVATE_FLAG);
    insertRow(3, NO_PRIVATE_FLAG);

    assertThat(countRowsWithPrivateFlag(null)).isEqualTo(3);
    assertThat(countRowsWithPrivateFlag(true)).isEqualTo(0);
    assertThat(countRowsWithPrivateFlag(false)).isEqualTo(0);

    underTest.execute();

    assertThat(countRowsWithPrivateFlag(null)).isEqualTo(0);
    assertThat(countRowsWithPrivateFlag(true)).isEqualTo(0);
    assertThat(countRowsWithPrivateFlag(false)).isEqualTo(3);
  }

  @Test
  public void execute_does_not_change_rows_with_non_null_private_column() throws SQLException {
    insertRow(2, true);
    insertRow(3, false);

    underTest.execute();

    assertThat(countRowsWithPrivateFlag(null)).isEqualTo(0);
    assertThat(countRowsWithPrivateFlag(true)).isEqualTo(1);
    assertThat(countRowsWithPrivateFlag(false)).isEqualTo(1);
  }

  @Test
  public void execute_is_reentreant() throws SQLException {
    insertRow(1, true);
    insertRow(2, false);
    insertRow(3, NO_PRIVATE_FLAG);

    underTest.execute();

    underTest.execute();
  }

  private int countRowsWithPrivateFlag(@Nullable Boolean privateFlag) {
    if (privateFlag == null) {
      return db.countSql("select count(1) from projects where private is null");
    }
    return db.countSql("select count(1) from projects where private=" + privateFlag);
  }

  private void insertRow(int id, @Nullable Boolean privateFlag) {
    db.executeInsert(
      "PROJECTS",
      "ORGANIZATION_UUID", "org_" + id,
      "UUID", "uuid_" + id,
      "UUID_PATH", "uuid_path_" + id,
      "ROOT_UUID", "root_uuid_" + id,
      "PRIVATE", privateFlag == null ? null : privateFlag.toString());
  }
}
