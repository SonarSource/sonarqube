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
package org.sonar.server.platform.db.migration.version.v108;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.utils.System2;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.MigrationDbTester;
import org.sonar.server.platform.db.migration.history.MigrationHistory;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.core.config.MQRModeConstants.MULTI_QUALITY_MODE_ENABLED;

class EnableSpecificMqrModeIT {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(EnableSpecificMqrMode.class);

  private final UuidFactory uuidFactory = new SequenceUuidFactory();
  private final MigrationHistory migrationHistory = mock(MigrationHistory.class);
  private final System2 system2 = new TestSystem2().setNow(2_000_000_000L);

  private final DataChange underTest = new EnableSpecificMqrMode(db.database(), migrationHistory, uuidFactory, system2);

  @ParameterizedTest
  @MethodSource("versions")
  void execute_correctlyInsertsProperty(long version, String expectedResult) throws SQLException {
    when(migrationHistory.getInitialDbVersion()).thenReturn(version);
    underTest.execute();

    assertThat(getPropertyFromDB()).hasSize(1);
    assertThat(getPropertyFromDB().get(0))
      .containsEntry("text_value", expectedResult)
      .containsEntry("is_empty", false)
      .containsEntry("created_at", 2_000_000_000L)
      .containsEntry("uuid", "00000000-0000-0000-0000-000000000001");
  }

  @ParameterizedTest
  @MethodSource("versions")
  void execute_doesNothingIfPropertyAlreadyExists(long version) throws SQLException {
    when(migrationHistory.getInitialDbVersion()).thenReturn(version);

    String uuid = uuidFactory.create();
    db.executeInsert("properties",
      "prop_key", MULTI_QUALITY_MODE_ENABLED,
      "text_value", "false",
      "is_empty", false,
      "created_at", 1_000_000_000L,
      "uuid", uuid);
    underTest.execute();

    assertThat(getPropertyFromDB()).hasSize(1);
    assertThat(getPropertyFromDB().get(0))
      .containsEntry("text_value", "false")
      .containsEntry("is_empty", false)
      .containsEntry("created_at", 1_000_000_000L)
      .containsEntry("uuid", uuid);
  }

  @Test
  void execute_shouldBeReentrant() throws SQLException {
    when(migrationHistory.getInitialDbVersion()).thenReturn(102_000L);
    underTest.execute();
    underTest.execute();

    assertThat(getPropertyFromDB()).hasSize(1);
    assertThat(getPropertyFromDB().get(0))
      .containsEntry("text_value", "true")
      .containsEntry("is_empty", false)
      .containsEntry("created_at", 2_000_000_000L)
      .containsEntry("uuid", "00000000-0000-0000-0000-000000000001");
  }

  private List<Map<String, Object>> getPropertyFromDB() {
    String sql = "SELECT text_value, is_empty, created_at, uuid FROM properties WHERE prop_key = '" + MULTI_QUALITY_MODE_ENABLED + "'";
    return db.select(sql);
  }

  private static Stream<Arguments> versions() {
    return Stream.of(
      Arguments.of(102_000L, "true"),
      Arguments.of(-1L, "true"),
      Arguments.of(101_990L, "false"));
  }
}
