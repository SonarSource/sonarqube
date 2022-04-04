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
package org.sonar.server.platform.db.migration.version.v89;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.assertj.core.groups.Tuple;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.core.util.Uuids;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.MigrationStep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class DropGithubEndpointOnProjectLevelSettingTest {
  private static final String DROPPED_PROPERTY = "sonar.pullrequest.github.endpoint";
  private static final String PROP_1_UUID = Uuids.createFast();
  private static final String PROP_2_UUID = Uuids.createFast();

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(DropGithubEndpointOnProjectLevelSettingTest.class, "schema.sql");

  private final MigrationStep underTest = new DropGithubEndpointOnProjectLevelSetting(db.database());

  @Test
  public void execute() throws SQLException {
    setupProperties();

    underTest.execute();

    verifyResult();
  }

  @Test
  public void is_reentrant() throws SQLException {
    setupProperties();

    underTest.execute();
    underTest.execute();

    verifyResult();
  }

  @Test
  public void is_successful_when_no_entries_to_drop() throws SQLException {
    underTest.execute();

    assertThat(db.select("SELECT UUID FROM PROPERTIES")).isEmpty();
  }

  private void setupProperties() {
    insertProperty(Uuids.createFast(), "some.prop", "1");
    insertProperty(Uuids.createFast(), "some.other.prop", null);
    insertProperty(PROP_1_UUID, DROPPED_PROPERTY, "1");
    insertProperty(PROP_2_UUID, DROPPED_PROPERTY, "2");
    insertProperty(Uuids.createFast(), DROPPED_PROPERTY, null);
  }

  private void verifyResult() {
    List<Map<String, Object>> result = db.select("SELECT UUID, PROP_KEY, COMPONENT_UUID FROM PROPERTIES");
    assertThat(result
      .stream()
      .map(e -> new Tuple(e.get("PROP_KEY"), e.get("COMPONENT_UUID")))
      .collect(Collectors.toList()))
        .containsExactlyInAnyOrder(
          tuple("some.prop", "1"),
          tuple("some.other.prop", null),
          tuple(DROPPED_PROPERTY, null));
    assertThat(result)
      .extracting(e -> e.get("UUID"))
      .doesNotContain(PROP_1_UUID, PROP_2_UUID);
  }

  private void insertProperty(String uuid, String key, @Nullable String projectUuid) {
    db.executeInsert("PROPERTIES",
      "UUID", uuid,
      "PROP_KEY", key,
      "COMPONENT_UUID", projectUuid,
      "USER_UUID", null,
      "IS_EMPTY", false,
      "TEXT_VALUE", "AnyValue",
      "CLOB_VALUE", null,
      "CREATED_AT", System2.INSTANCE.now());
  }
}
