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
import java.sql.Types;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.assertj.core.api.Assertions.assertThat;

public class AddUuidColumnToPropertiesTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(AddUuidColumnToPropertiesTest.class, "schema.sql");

  private DdlChange underTest = new AddUuidColumnToProperties(db.database());

  @Before
  public void setup() {
    insertProperties(1L, "uuid1", "key1");
    insertProperties(2L, "uuid2", "key2");
    insertProperties(3L, "uuid3", "key3");
  }

  @Test
  public void add_uuid_to_properties() throws SQLException {
    underTest.execute();

    db.assertColumnDefinition("properties", "uuid", Types.VARCHAR, 40, true);

    assertThat(db.countSql("select count(id) from properties"))
      .isEqualTo(3);
  }

  private void insertProperties(Long id, String uuid, String propKey) {

    db.executeInsert("properties",
      "id", id,
      "prop_key", propKey,
      "is_empty", false,
      "created_at", 0);
  }
}
