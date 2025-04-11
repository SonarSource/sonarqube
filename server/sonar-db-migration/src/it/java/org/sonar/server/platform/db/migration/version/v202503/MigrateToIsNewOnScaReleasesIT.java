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

class MigrateToIsNewOnScaReleasesIT {
  private static final String COLUMN_NAME = "is_new";

  @RegisterExtension
  public final MigrationDbTester db = createForMigrationStep(MigrateToIsNewOnScaReleases.class);
  private final DataChange underTest = new MigrateToIsNewOnScaReleases(db.database());

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
    assertThat(db.countSql("select count(*) from sca_releases")).isZero();
  }

  @ParameterizedTest
  @MethodSource("provideParamsForExecute")
  void execute_updatesCorrectly(boolean newInPullRequest, boolean isNew, boolean expectedIsNew) throws SQLException {
    insertRelease("1", newInPullRequest, isNew);

    assertThat(db.selectFirst("select * from sca_releases where uuid = '%s'".formatted("scaReleaseUuid1"))).containsEntry(COLUMN_NAME, isNew);
    underTest.execute();
    assertThat(db.selectFirst("select * from sca_releases where uuid = '%s'".formatted("scaReleaseUuid1"))).containsEntry(COLUMN_NAME, expectedIsNew);
  }

  private void insertRelease(String suffix, boolean newInPullRequest, @Nullable Boolean isNew) {
    db.executeInsert("sca_releases",
      "uuid", "scaReleaseUuid" + suffix,
      "component_uuid", "componentUuid" + suffix,
      "package_url", "packageUrl",
      "package_manager", "MAVEN",
      "package_name", "packageName" + suffix,
      "version", "1.0.0",
      "license_expression", "MIT",
      "declared_license_expression", "MIT",
      "is_new", isNew,
      "new_in_pull_request", newInPullRequest,
      "known", true,
      "known_package", true,
      "updated_at", 1L,
      "created_at", 2L);
  }
}
