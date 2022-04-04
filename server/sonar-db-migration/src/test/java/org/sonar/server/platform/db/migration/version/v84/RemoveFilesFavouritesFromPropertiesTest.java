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
package org.sonar.server.platform.db.migration.version.v84;

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
import static org.sonar.api.resources.Qualifiers.APP;
import static org.sonar.api.resources.Qualifiers.DIRECTORY;
import static org.sonar.api.resources.Qualifiers.FILE;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.api.resources.Qualifiers.UNIT_TEST_FILE;

public class RemoveFilesFavouritesFromPropertiesTest {

  private static final String PROPERTIES_TABLE_NAME = "properties";

  @Rule
  public CoreDbTester dbTester = CoreDbTester.createForSchema(RemoveFilesFavouritesFromPropertiesTest.class, "schema.sql");

  private final DataChange underTest = new RemoveFilesFavouritesFromProperties(dbTester.database());

  private static final String APPLICATION_UUID_1 = Uuids.createFast();
  private static final String PROJECT_UUID_2 = Uuids.createFast();
  private static final String FILE_UUID_3 = Uuids.createFast();
  private static final String DIRECTORY_UUID_4 = Uuids.createFast();
  private static final String UNIT_TEST_FILE_UUID_5 = Uuids.createFast();

  private static final String USER_UUID_1 = "1";
  private static final String USER_UUID_2 = "2";

  @Before
  public void setup() {
    insertComponent(APPLICATION_UUID_1, APP);
    insertComponent(PROJECT_UUID_2, PROJECT);
    insertComponent(FILE_UUID_3, FILE);
    insertComponent(DIRECTORY_UUID_4, DIRECTORY);
    insertComponent(UNIT_TEST_FILE_UUID_5, UNIT_TEST_FILE);

    insertProperty("1", USER_UUID_1, APPLICATION_UUID_1, "test");
    insertProperty("2", USER_UUID_2, PROJECT_UUID_2, "test2");
    insertProperty("3", USER_UUID_2, null, "test3");
  }

  @Test
  public void migrate() throws SQLException {
    insertProperty("4", USER_UUID_1, APPLICATION_UUID_1, "favourite");
    // properties to remove
    insertProperty("5", USER_UUID_1, FILE_UUID_3, "favourite");
    insertProperty("6", USER_UUID_2, FILE_UUID_3, "favourite");
    insertProperty("7", USER_UUID_2, DIRECTORY_UUID_4, "favourite");
    insertProperty("8", USER_UUID_2, UNIT_TEST_FILE_UUID_5, "favourite");

    underTest.execute();

    assertThat(dbTester.countRowsOfTable(PROPERTIES_TABLE_NAME)).isEqualTo(4);
    assertThat(dbTester.select("select uuid from properties").stream().map(columns -> columns.get("UUID")))
      .containsOnly("1", "2", "3", "4");
  }

  @Test
  public void properties_table_empty() throws SQLException {
    dbTester.executeUpdateSql("delete from properties");

    underTest.execute();

    assertThat(dbTester.countRowsOfTable(PROPERTIES_TABLE_NAME)).isZero();
  }

  @Test
  public void does_not_remove_properties_with_key_other_than_favourite() throws SQLException {
    underTest.execute();

    assertThat(dbTester.countRowsOfTable(PROPERTIES_TABLE_NAME)).isEqualTo(3L);
  }

  @Test
  public void migration_is_re_entrant() throws SQLException {
    insertProperty("4", USER_UUID_1, APPLICATION_UUID_1, "favourite");
    // properties to remove
    insertProperty("5", USER_UUID_1, FILE_UUID_3, "favourite");
    insertProperty("6", USER_UUID_2, FILE_UUID_3, "favourite");

    underTest.execute();

    insertProperty("7", USER_UUID_2, DIRECTORY_UUID_4, "favourite");
    insertProperty("8", USER_UUID_2, UNIT_TEST_FILE_UUID_5, "favourite");

    // re-entrant
    underTest.execute();

    assertThat(dbTester.countRowsOfTable(PROPERTIES_TABLE_NAME)).isEqualTo(4);
    assertThat(dbTester.select("select uuid from properties").stream().map(columns -> columns.get("UUID")))
      .containsOnly("1", "2", "3", "4");
  }

  private void insertComponent(String uuid, String qualifier) {
    dbTester.executeInsert("COMPONENTS",
      "UUID", uuid,
      "NAME", uuid + "-name",
      "DESCRIPTION", uuid + "-description",
      "ORGANIZATION_UUID", "default",
      "KEE", uuid + "-key",
      "PROJECT_UUID", uuid,
      "MAIN_BRANCH_PROJECT_UUID", "project_uuid",
      "UUID_PATH", ".",
      "ROOT_UUID", uuid,
      "PRIVATE", Boolean.toString(false),
      "SCOPE", "TRK",
      "QUALIFIER", qualifier);
  }

  private void insertProperty(String id, String userUuid, @Nullable String componentUuid, String propKey) {
    dbTester.executeInsert(PROPERTIES_TABLE_NAME,
      "uuid", id,
      "user_uuid", userUuid,
      "component_uuid", componentUuid,
      "prop_key", propKey,
      "is_empty", true,
      "created_at", Instant.now().toEpochMilli());
  }
}
