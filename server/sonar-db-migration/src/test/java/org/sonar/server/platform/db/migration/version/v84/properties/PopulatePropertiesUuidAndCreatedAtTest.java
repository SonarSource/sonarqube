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
package org.sonar.server.platform.db.migration.version.v84.properties;

import java.sql.SQLException;
import java.util.Objects;
import java.util.stream.Collectors;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;

public class PopulatePropertiesUuidAndCreatedAtTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulatePropertiesUuidAndCreatedAtTest.class, "schema.sql");

  private UuidFactory uuidFactory = UuidFactoryFast.getInstance();
  private DataChange underTest = new PopulatePropertiesUuid(db.database(), uuidFactory);

  @Test
  public void populate_uuids() throws SQLException {
    insertProperties(1L, "uuid1", "key1");
    insertProperties(2L, "uuid2", "key2");
    insertProperties(3L, "uuid3", "key3");


    underTest.execute();

    verifyUuidsAreNotNull();
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    insertProperties(1L, "uuid1", "key1");
    insertProperties(2L, "uuid2", "key2");
    insertProperties(3L, "uuid3", "key3");

    underTest.execute();
    // re-entrant
    underTest.execute();

    verifyUuidsAreNotNull();
  }

  private void verifyUuidsAreNotNull() {
    assertThat(db.select("select uuid from properties")
      .stream()
      .map(row -> row.get("UUID"))
      .filter(Objects::isNull)
      .collect(Collectors.toList())).isEmpty();
  }

  private void insertProperties(Long id, String uuid, String propKey) {

    db.executeInsert("properties",
      "id", id,
      "prop_key", propKey,
      "uuid", uuid,
      "is_empty", false,
      "created_at", 0);
  }
}
