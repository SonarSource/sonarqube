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
package org.sonar.server.platform.db.migration.version.v84.grouproles;

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

public class PopulateGroupRolesUuidTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateGroupRolesUuidTest.class, "schema.sql");

  private UuidFactory uuidFactory = UuidFactoryFast.getInstance();
  private DataChange underTest = new PopulateGroupRolesUuid(db.database(), uuidFactory);

  @Test
  public void populate_uuids() throws SQLException {
    insertGroupRoles(1L);
    insertGroupRoles(2L);
    insertGroupRoles(3L);

    underTest.execute();

    verifyUuidsAreNotNull();
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    insertGroupRoles(1L);
    insertGroupRoles(2L);
    insertGroupRoles(3L);

    underTest.execute();
    // re-entrant
    underTest.execute();

    verifyUuidsAreNotNull();
  }

  private void verifyUuidsAreNotNull() {
    assertThat(db.select("select uuid from group_roles")
      .stream()
      .map(row -> row.get("UUID"))
      .filter(Objects::isNull)
      .collect(Collectors.toList())).isEmpty();
  }

  private void insertGroupRoles(Long id) {
    db.executeInsert("group_roles",
      "id", id,
      "uuid", uuidFactory.create(),
      "organization_uuid", uuidFactory.create(),
      "group_id", id + 1,
      "role", id + 2,
      "component_uuid", uuidFactory.create());
  }

}
