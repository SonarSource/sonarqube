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
import org.sonar.server.platform.db.migration.step.MigrationStep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.MigrationDbTester.createForMigrationStep;

class MigrateRemoveNonCanonicalScaEncounteredLicensesIT {
  @RegisterExtension
  public final MigrationDbTester db = createForMigrationStep(MigrateRemoveNonCanonicalScaEncounteredLicenses.class);
  private final MigrationStep underTest = new MigrateRemoveNonCanonicalScaEncounteredLicenses(db.database());

  @Test
  void test_removesNonCanonical() throws SQLException {
    // we should keep these
    insertEncounteredLicense("0", "GPL-2.0-only");
    insertEncounteredLicense("1", "LicenseRef-something");
    insertEncounteredLicense("2", "LicenseRef-something-with-something-else");

    // we should delete these
    insertEncounteredLicense("3", "GPL-2.0-with-classpath-exception");
    insertEncounteredLicense("4", "GPL-2.0-with-autoconf-exception");

    assertThat(db.countSql("select count(*) from sca_encountered_licenses")).isEqualTo(5);
    underTest.execute();

    assertThat(db.select("select uuid from sca_encountered_licenses")).map(row -> row.get("uuid"))
      .containsExactlyInAnyOrder("scaEncounteredLicenseUuid0", "scaEncounteredLicenseUuid1", "scaEncounteredLicenseUuid2");
  }

  @Test
  void test_canRunMultipleTimesOnEmptyTable() throws SQLException {
    assertThat(db.countSql("select count(*) from sca_encountered_licenses")).isZero();
    underTest.execute();
    underTest.execute();
    assertThat(db.countSql("select count(*) from sca_encountered_licenses")).isZero();
  }

  private void insertEncounteredLicense(String suffix, String licensePolicyId) {
    db.executeInsert("sca_encountered_licenses",
      "uuid", "scaEncounteredLicenseUuid" + suffix,
      "license_policy_id", licensePolicyId,
      "updated_at", 1L,
      "created_at", 1L);
  }
}
