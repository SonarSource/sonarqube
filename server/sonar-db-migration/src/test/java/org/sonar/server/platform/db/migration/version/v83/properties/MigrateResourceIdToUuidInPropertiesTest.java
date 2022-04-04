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
package org.sonar.server.platform.db.migration.version.v83.properties;

import java.sql.SQLException;
import java.util.Date;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.assertj.core.groups.Tuple;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class MigrateResourceIdToUuidInPropertiesTest {
  private static final String TABLE_NAME = "properties";

  @Rule
  public CoreDbTester dbTester = CoreDbTester.createForSchema(MigrateResourceIdToUuidInPropertiesTest.class, "schema.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private MigrateResourceIdToUuidInProperties underTest = new MigrateResourceIdToUuidInProperties(dbTester.database());
  private int id = 1;

  @Test
  public void data_has_been_migrated() throws SQLException {
    insertComponent(1, "uuid1");
    insertProperty(1);
    insertProperty(2);
    insertProperty(null);

    underTest.execute();
    assertThat(dbTester.select("select ID, RESOURCE_ID, COMPONENT_UUID, PROP_KEY, TEXT_VALUE from " + TABLE_NAME).stream()
      .map(e -> new Tuple(e.get("ID"), e.get("RESOURCE_ID"), e.get("COMPONENT_UUID"), e.get("PROP_KEY"), e.get("TEXT_VALUE")))
      .collect(Collectors.toList())).containsExactlyInAnyOrder(
      new Tuple(1L, 1L, "uuid1", "key", "value"),
      new Tuple(3L, null, null, "key", "value"));

    // reentrant
    underTest.execute();
  }

  private void insertProperty(@Nullable Integer resourceId) {
    dbTester.executeInsert(TABLE_NAME,
      "id", id++,
      "prop_key", "key",
      "resource_id", resourceId,
      "is_empty", false,
      "text_value", "value",
      "created_at", 1000L);
  }

  private void insertComponent(int id, String uuid) {
    dbTester.executeInsert("COMPONENTS",
      "ID", id,
      "NAME", uuid + "-name",
      "DESCRIPTION", uuid + "-description",
      "ORGANIZATION_UUID", "default",
      "CREATED_AT", new Date(1000L),
      "KEE", uuid + "-key",
      "UUID", uuid,
      "PROJECT_UUID", uuid,
      "MAIN_BRANCH_PROJECT_UUID", "project_uuid",
      "UUID_PATH", ".",
      "ROOT_UUID", uuid,
      "PRIVATE", Boolean.toString(false),
      "SCOPE", "TRK",
      "QUALIFIER", "PRJ");
  }

}
