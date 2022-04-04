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
import java.time.Instant;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.core.util.Uuids;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;

public class DropOrphanFavoritesFromPropertiesTest {

  private static final String PROPERTIES_TABLE_NAME = "properties";

  @Rule
  public CoreDbTester dbTester = CoreDbTester.createForSchema(DropOrphanFavoritesFromPropertiesTest.class, "schema.sql");

  private DataChange underTest = new DropOrphanFavoritesFromProperties(dbTester.database());

  private static final String COMPONENT_UUID_1 = Uuids.createFast();
  private static final String COMPONENT_UUID_2 = Uuids.createFast();
  private static final String COMPONENT_UUID_3 = Uuids.createFast();
  private static final String COMPONENT_UUID_4 = Uuids.createFast();

  private static final String USER_UUID_1 = "1";
  private static final String USER_UUID_2 = "2";

  @Before
  public void setup() {
    insertProperty("1", USER_UUID_1, COMPONENT_UUID_1, "test");
    insertProperty("2", USER_UUID_2, COMPONENT_UUID_2, "test2");
    insertProperty("3", USER_UUID_2, COMPONENT_UUID_3, "test3");
    insertProperty("4", USER_UUID_2, null, "test3");
  }

  @Test
  public void migrate() throws SQLException {
    insertProperty("5", USER_UUID_1, COMPONENT_UUID_4, "favourite");
    // properties to remove
    insertProperty("6", USER_UUID_2, null, "favourite");
    insertProperty("7", USER_UUID_2, null, "favourite");
    insertProperty("8", USER_UUID_2, null, "favourite");

    underTest.execute();

    assertThat(dbTester.countRowsOfTable(PROPERTIES_TABLE_NAME)).isEqualTo(5);
    assertThat(dbTester.select("select uuid from properties").stream().map(columns -> columns.get("UUID")))
      .containsOnly("1", "2", "3", "4", "5");
  }

  @Test
  public void does_not_fail_when_properties_table_empty() throws SQLException {
    dbTester.executeUpdateSql("delete from properties");

    underTest.execute();

    assertThat(dbTester.countRowsOfTable(PROPERTIES_TABLE_NAME)).isZero();
  }

  @Test
  public void does_not_remove_properties_with_key_other_than_favourite() throws SQLException {
    underTest.execute();

    assertThat(dbTester.countRowsOfTable(PROPERTIES_TABLE_NAME)).isEqualTo(4L);
  }

  @Test
  public void migration_is_re_entrant() throws SQLException {
    insertProperty("5", USER_UUID_1, COMPONENT_UUID_1, "favourite");
    // properties to remove
    insertProperty("6", USER_UUID_1, null, "favourite");
    insertProperty("7", USER_UUID_2, null, "favourite");

    underTest.execute();

    insertProperty("8", USER_UUID_2, null, "favourite");
    insertProperty("9", USER_UUID_2, null, "favourite");

    // re-entrant
    underTest.execute();

    assertThat(dbTester.countRowsOfTable(PROPERTIES_TABLE_NAME)).isEqualTo(5);
    assertThat(dbTester.select("select uuid from properties").stream().map(columns -> columns.get("UUID")))
      .containsOnly("1", "2", "3", "4", "5");
  }

  private void insertProperty(String uuid, String userUuid, @Nullable String componentUuid, String propKey) {
    dbTester.executeInsert(PROPERTIES_TABLE_NAME,
      "uuid", uuid,
      "user_uuid", userUuid,
      "component_uuid", componentUuid,
      "prop_key", propKey,
      "is_empty", true,
      "created_at", Instant.now().toEpochMilli());
  }
}
