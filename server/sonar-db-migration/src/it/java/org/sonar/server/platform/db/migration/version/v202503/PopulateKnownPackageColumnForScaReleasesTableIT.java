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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.MigrationDbTester.createForMigrationStep;

class PopulateKnownPackageColumnForScaReleasesTableIT {
  private static final String COLUMN_NAME = "known_package";

  @RegisterExtension
  public final MigrationDbTester db = createForMigrationStep(PopulateKnownPackageColumnForScaReleasesTable.class);
  private final DataChange underTest = new PopulateKnownPackageColumnForScaReleasesTable(db.database());

  @Test
  void execute_doesNotCreateRecords() throws SQLException {
    underTest.execute();
    assertThat(db.countSql("select count(*) from sca_releases")).isZero();
  }

  @Test
  void execute_whenPackageKnownNull_updatesKnownTrueToPackageKnownTrue() throws SQLException {
    boolean known = true;
    Boolean packageKnown = null;
    insertRelease("1", known, packageKnown);

    assertThat(db.selectFirst("select * from sca_releases where uuid = '%s'".formatted("scaReleaseUuid1"))).containsEntry(COLUMN_NAME, packageKnown);
    underTest.execute();
    assertThat(db.selectFirst("select * from sca_releases where uuid = '%s'".formatted("scaReleaseUuid1"))).containsEntry(COLUMN_NAME, known);
  }

  @Test
  void execute_whenPackageKnownNull_updatesKnownFalseToPackageKnownFalse() throws SQLException {
    boolean known = false;
    Boolean packageKnown = null;
    insertRelease("1", known, packageKnown);

    assertThat(db.selectFirst("select * from sca_releases where uuid = '%s'".formatted("scaReleaseUuid1"))).containsEntry(COLUMN_NAME, packageKnown);
    underTest.execute();
    assertThat(db.selectFirst("select * from sca_releases where uuid = '%s'".formatted("scaReleaseUuid1"))).containsEntry(COLUMN_NAME, known);
  }

  @Test
  void execute_whenPackageKnownNotNull_doesNotUpdatePackageKnown() throws SQLException {
    boolean known = false;
    Boolean packageKnown = true;
    insertRelease("1", known, packageKnown);

    assertThat(db.selectFirst("select * from sca_releases where uuid = '%s'".formatted("scaReleaseUuid1"))).containsEntry(COLUMN_NAME, packageKnown);
    underTest.execute();
    assertThat(db.selectFirst("select * from sca_releases where uuid = '%s'".formatted("scaReleaseUuid1"))).containsEntry(COLUMN_NAME, packageKnown);
  }

  @Test
  void execute_resultsInNoNullPackageKnown() throws SQLException {
    insertRelease("1", true, null);
    insertRelease("2", false, null);
    insertRelease("3", false, false);

    underTest.execute();
    assertThat(db.countSql("select count(*) from sca_releases where known_package is null")).isZero();
  }

  private void insertRelease(String suffix, boolean known, Boolean knownPackage) {
    db.executeInsert("sca_releases",
      "uuid", "scaReleaseUuid" + suffix,
      "component_uuid", "componentUuid" + suffix,
      "package_url", "packageUrl",
      "package_manager", "MAVEN",
      "package_name", "packageName" + suffix,
      "version", "1.0.0",
      "license_expression", "MIT",
      "declared_license_expression", "MIT",
      "known", known,
      "new_in_pull_request", false,
      "known_package", knownPackage,
      "updated_at", 1L,
      "created_at", 2L);
  }
}
