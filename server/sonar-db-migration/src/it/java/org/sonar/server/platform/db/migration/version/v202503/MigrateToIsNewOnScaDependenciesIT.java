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
package org.sonar.server.platform.db.migration.version.v202503;

import java.sql.SQLException;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.db.MigrationDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.MigrationDbTester.createForMigrationStep;

class MigrateToIsNewOnScaDependenciesIT {
  private static final String COLUMN_NAME = "is_new";

  @RegisterExtension
  public final MigrationDbTester db = createForMigrationStep(MigrateToIsNewOnScaDependencies.class);
  private final DataChange underTest = new MigrateToIsNewOnScaDependencies(db.database());

  private static Stream<Arguments> provideParamsForExecute() {
    return Stream.of(
      Arguments.of(false, false, false),
      Arguments.of(false, true, false),
      Arguments.of(true, false, true),
      Arguments.of(true, true, true));
  }

  @Test
  void execute_doesNotCreateRecords() throws SQLException {
    underTest.execute();
    assertThat(db.countSql("select count(*) from sca_dependencies")).isZero();
  }

  @ParameterizedTest
  @MethodSource("provideParamsForExecute")
  void execute_updatesCorrectly(boolean newInPullRequest, boolean isNew, boolean expectedIsNew) throws SQLException {
    insertDependency("1", newInPullRequest, isNew);

    assertThat(db.selectFirst("select * from sca_dependencies where uuid = '%s'".formatted("scaReleaseUuid1"))).containsEntry(COLUMN_NAME, isNew);
    underTest.execute();
    assertThat(db.selectFirst("select * from sca_dependencies where uuid = '%s'".formatted("scaReleaseUuid1"))).containsEntry(COLUMN_NAME, expectedIsNew);
  }

  private void insertDependency(String suffix, boolean newInPullRequest, @Nullable Boolean isNew) {
    db.executeInsert("sca_dependencies",
      "uuid", "scaReleaseUuid" + suffix,
      "sca_release_uuid", "componentUuid" + suffix,
      "direct", true,
      "scope", "development",
      "production_scope", true,
      "user_dependency_file_path", "Gemfile",
      "lockfile_dependency_file_path", "Gemfile.lock",
      "chains", "[]",
      "is_new", isNew,
      "new_in_pull_request", newInPullRequest,
      "updated_at", 1L,
      "created_at", 2L);
  }
}
