/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v70;

import java.sql.SQLException;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.assertj.core.groups.Tuple;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class DeleteGlobalSonarQualityGateSettingTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(DeleteGlobalSonarQualityGateSettingTest.class, "properties.sql");

  private DataChange underTest = new DeleteGlobalSonarQualityGateSetting(db.database());

  @Test
  public void delete_sonar_quality_gate_setting() throws SQLException {
    insertSetting("sonar.qualitygate", null);
    insertSetting("sonar.qualitygate", 1L);
    insertSetting("other", null);
    insertSetting("other", 2L);

    underTest.execute();

    assertSettings(
      tuple("sonar.qualitygate", 1L),
      tuple("other", null),
      tuple("other", 2L));
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    insertSetting("sonar.qualitygate", null);
    insertSetting("sonar.qualitygate", 1L);

    underTest.execute();
    assertSettings(tuple("sonar.qualitygate", 1L));

    underTest.execute();
    assertSettings(tuple("sonar.qualitygate", 1L));
  }

  @Test
  public void does_nothing_when_no_sonar_quality_gate_setting() throws SQLException {
    insertSetting("other", null);

    underTest.execute();

    assertSettings(tuple("other", null));
  }

  @Test
  public void does_nothing_on_empty_table() throws SQLException {
    underTest.execute();

    assertSettings();
  }

  private void assertSettings(Tuple... expectedTuples) {
    assertThat(db.select("SELECT PROP_KEY, RESOURCE_ID FROM PROPERTIES")
      .stream()
      .map(map -> new Tuple(map.get("PROP_KEY"), map.get("RESOURCE_ID")))
      .collect(Collectors.toList()))
      .containsExactlyInAnyOrder(expectedTuples);
  }

  private void insertSetting(String key, @Nullable Long componentId) {
    db.executeInsert(
      "properties",
      "PROP_KEY", key,
      "RESOURCE_ID", componentId,
      "IS_EMPTY", false,
      "CREATED_AT", 1000);
  }
}
