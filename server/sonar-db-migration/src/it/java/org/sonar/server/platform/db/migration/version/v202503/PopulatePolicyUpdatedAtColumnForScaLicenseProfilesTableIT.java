/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PopulatePolicyUpdatedAtColumnForScaLicenseProfilesTableIT {
  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(PopulatePolicyUpdatedAtColumnForScaLicenseProfilesTable.class);
  private final PopulatePolicyUpdatedAtColumnForScaLicenseProfilesTable underTest = new PopulatePolicyUpdatedAtColumnForScaLicenseProfilesTable(db.database());

  @Test
  void execute_shouldPopulatePolicyUpdatedAtWithUpdatedAt() throws SQLException {
    insertScaLicenseProfile(1);
    insertScaLicenseProfile(2);

    underTest.execute();

    assertThatPolicyUpdatedAtIsPopulated();
  }

  @Test
  void execute_whenAlreadyExecuted_shouldBeIdempotent() throws SQLException {
    insertScaLicenseProfile(1);

    underTest.execute();
    underTest.execute();

    assertThatPolicyUpdatedAtIsPopulated();
  }

  private void insertScaLicenseProfile(Integer index) {
    db.executeInsert("sca_license_profiles",
      "uuid", "uuid-" + index,
      "is_default_profile", false,
      "name", "licenseProfile-" + index,
      "created_at", 1L,
      "updated_at", 2L,
      "policy_updated_at", null);
  }

  private void assertThatPolicyUpdatedAtIsPopulated() {
    List<Map<String, Object>> rows = db.select("select policy_updated_at, updated_at from sca_license_profiles");
    assertThat(rows).isNotEmpty()
      .allSatisfy(row -> assertEquals(row.get("policy_updated_at"), row.get("updated_at")));
  }
}
