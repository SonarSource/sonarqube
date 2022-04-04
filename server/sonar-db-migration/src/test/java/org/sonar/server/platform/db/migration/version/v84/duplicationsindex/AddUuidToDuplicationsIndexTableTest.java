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
package org.sonar.server.platform.db.migration.version.v84.duplicationsindex;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.assertj.core.api.Assertions.assertThat;

public class AddUuidToDuplicationsIndexTableTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(AddUuidToDuplicationsIndexTableTest.class, "schema.sql");

  private DdlChange underTest = new AddUuidToDuplicationsIndexTable(db.database());

  private UuidFactoryFast uuidFactory = UuidFactoryFast.getInstance();

  @Before
  public void setup() {
    insertDuplicationsIndex(1L);
    insertDuplicationsIndex(2L);
    insertDuplicationsIndex(3L);
  }

  @Test
  public void add_uuid_column_to_duplications_index() throws SQLException {
    underTest.execute();

    db.assertColumnDefinition("duplications_index", "uuid", Types.VARCHAR, 40, true);

    assertThat(db.countRowsOfTable("duplications_index"))
      .isEqualTo(3);
  }

  private void insertDuplicationsIndex(Long id) {
    db.executeInsert("duplications_index",
      "id", id,
      "hash", uuidFactory.create(),
      "index_in_file", id + 1,
      "start_line", id + 2,
      "end_line", id + 3,
      "component_uuid", uuidFactory.create(),
      "analysis_uuid", uuidFactory.create());
  }

}
