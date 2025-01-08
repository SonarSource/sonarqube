/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v103;

import java.sql.SQLException;
import java.util.Date;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.core.util.Uuids;
import org.sonar.db.MigrationDbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class DeduplicatePropertiesTableIT {
  public static final String KEY = "key";
  public static final String ENTITY = "entity";
  public static final String USER = "user";
  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(DeduplicatePropertiesTable.class);
  private final DeduplicatePropertiesTable underTest = new DeduplicatePropertiesTable(db.database());


  public void createBaseProperties() {
    createProperty(KEY, null, null);
    createProperty(KEY, USER, null);
    createProperty(KEY, USER, ENTITY);
  }

  private void createProperty(String key, @Nullable String user, @Nullable String entity) {
    db.executeInsert("PROPERTIES",
      "UUID", Uuids.createFast(),
      "PROP_KEY", key,
      "TEXT_VALUE", "value",
      "ENTITY_UUID", entity,
      "USER_UUID", user,
      "IS_EMPTY", false,
      "CREATED_AT", new Date().getTime());
  }

  @Test
  void execute_shouldDeduplicateRows_WhenOnlyKeyIsSpecified() throws SQLException {
    createBaseProperties();
    createProperty(KEY, null, null);
    createProperty(KEY, null, null);
    underTest.execute();
    assertThat(db.select("select * from properties"))
      .hasSize(3)
      .extracting(str -> str.get("PROP_KEY"), str -> str.get("USER_UUID"), str -> str.get("ENTITY_UUID"))
      .containsExactlyInAnyOrder(tuple(KEY, null, null), tuple(KEY, USER, null), tuple(KEY, USER, ENTITY));
  }

  @Test
  void execute_shouldDeduplicateRows_WhenOnlyKeyAndUserAreSpecified() throws SQLException {
    createBaseProperties();
    createProperty(KEY, USER, null);
    createProperty(KEY, USER, null);
    underTest.execute();
    assertThat(db.select("select * from properties"))
      .hasSize(3)
      .extracting(str -> str.get("PROP_KEY"), str -> str.get("USER_UUID"), str -> str.get("ENTITY_UUID"))
      .containsExactlyInAnyOrder(tuple(KEY, null, null), tuple(KEY, USER, null), tuple(KEY, USER, ENTITY));
  }

  @Test
  void execute_shouldDeduplicateRows_WhenKeyUserAndEntityAreSpecified() throws SQLException {
    createBaseProperties();
    createProperty(KEY, USER, ENTITY);
    createProperty(KEY, USER, ENTITY);
    underTest.execute();
    assertThat(db.select("select * from properties"))
      .hasSize(3)
      .extracting(str -> str.get("PROP_KEY"), str -> str.get("USER_UUID"), str -> str.get("ENTITY_UUID"))
      .containsExactlyInAnyOrder(tuple(KEY, null, null), tuple(KEY, USER, null), tuple(KEY, USER, ENTITY));
  }

  @Test
  void execute_shouldBeReentrant() throws SQLException {
    createBaseProperties();
    createProperty(KEY, USER, ENTITY);

    underTest.execute();
    underTest.execute();

    assertThat(db.select("select * from properties"))
      .hasSize(3)
      .extracting(str -> str.get("PROP_KEY"), str -> str.get("USER_UUID"), str -> str.get("ENTITY_UUID"))
      .containsExactlyInAnyOrder(tuple(KEY, null, null), tuple(KEY, USER, null), tuple(KEY, USER, ENTITY));
  }

}
