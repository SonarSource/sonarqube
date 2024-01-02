/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v98;

import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;

public class UpsertSonarUsersDescriptionTest {

  private final UuidFactory uuidFactory = UuidFactoryFast.getInstance();
  public static final String OLD_DESCRIPTION = "Any new users created will automatically join this group";
  public static final String NEW_DESCRIPTION = "Every authenticated user automatically belongs to this group";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(UpsertSonarUsersDescriptionTest.class, "schema.sql");

  private final DataChange underTest = new UpsertSonarUsersDescription(db.database());

  @Test
  public void migration_populates_sonar_users_group_description() throws SQLException {
    String uuid = insertSonarUsersGroupWithOldDescription();
    underTest.execute();
    assertSonarUsersGroupDescriptionIsUpsertedCorrectly(uuid);
  }

  @Test
  public void migration_should_be_reentrant() throws SQLException {
    String userUuid1 = insertSonarUsersGroupWithOldDescription();
    underTest.execute();
    // re-entrant
    underTest.execute();
    assertSonarUsersGroupDescriptionIsUpsertedCorrectly(userUuid1);
  }

  private void assertSonarUsersGroupDescriptionIsUpsertedCorrectly(String userUuid) {
    String selectSql = String.format("select description from groups where uuid='%s'", userUuid);
    assertThat(db.select(selectSql).stream().map(row -> row.get("DESCRIPTION")).collect(Collectors.toList()))
      .containsExactlyInAnyOrder(UpsertSonarUsersDescriptionTest.NEW_DESCRIPTION);
  }

  private String insertSonarUsersGroupWithOldDescription() {
    Map<String, Object> map = new HashMap<>();
    String uuid = uuidFactory.create();
    map.put("UUID", uuid);
    map.put("NAME", "sonar-users");
    map.put("DESCRIPTION", OLD_DESCRIPTION);
    map.put("CREATED_AT", new Date());
    db.executeInsert("groups", map);
    return uuid;
  }
}
