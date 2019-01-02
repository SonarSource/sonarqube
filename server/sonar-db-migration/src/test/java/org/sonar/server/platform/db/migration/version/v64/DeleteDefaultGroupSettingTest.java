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
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class DeleteDefaultGroupSettingTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(DeleteDefaultGroupSettingTest.class, "initial.sql");

  private DeleteDefaultGroupSetting underTest = new DeleteDefaultGroupSetting(db.database());

  @Test
  public void delete_setting() throws SQLException {
    insertDefaultGroupProperty();

    underTest.execute();

    assertThat(db.countRowsOfTable("properties")).isZero();
  }

  @Test
  public void does_not_fail_when_setting_does_not_exist() throws Exception {
    underTest.execute();

    assertThat(db.countRowsOfTable("properties")).isZero();
  }

  @Test
  public void does_not_delete_other_setting() throws Exception {
    insertDefaultGroupProperty();
    insertProperty("sonar.prop", "a value");

    underTest.execute();

    assertThat(db.countRowsOfTable("properties")).isEqualTo(1);
  }

  @Test
  public void migration_is_reentrant() throws Exception {
    insertDefaultGroupProperty();

    underTest.execute();
    assertThat(db.countRowsOfTable("properties")).isZero();

    underTest.execute();
    assertThat(db.countRowsOfTable("properties")).isZero();
  }

  private void insertDefaultGroupProperty() {
    insertProperty("sonar.defaultGroup", "123");
  }

  private void insertProperty(String key, String value) {
    db.executeInsert(
      "PROPERTIES",
      "PROP_KEY", key,
      "TEXT_VALUE", value,
      "IS_EMPTY", "false",
      "CREATED_AT", "1000");
  }


}
