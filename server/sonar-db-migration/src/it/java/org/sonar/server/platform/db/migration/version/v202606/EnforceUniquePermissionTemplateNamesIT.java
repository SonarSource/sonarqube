/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version.v202606;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;
import org.sonar.db.dialect.H2;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.sonar.server.platform.db.migration.version.v202606.EnforceUniquePermissionTemplateNames.TABLE_NAME;

class EnforceUniquePermissionTemplateNamesIT {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(EnforceUniquePermissionTemplateNames.class);

  private final DdlChange underTest = new EnforceUniquePermissionTemplateNames(db.database());

  @Test
  void execute_shouldCreateCaseInsensitiveUniqueIndex() throws SQLException {
    db.executeInsert(TABLE_NAME, "uuid", "first-template", "name", "Finance");

    underTest.execute();

    assertThat(select("SELECT name_upper FROM permission_templates WHERE uuid = 'first-template'"))
      .extracting(row -> row.get("NAME_UPPER"))
      .containsExactly("FINANCE");
    assertThatThrownBy(() -> db.executeInsert(TABLE_NAME, "uuid", "second-template", "name", "finance"))
      .isInstanceOf(RuntimeException.class);
  }

  @Test
  void execute_shouldBeReentrant() throws SQLException {
    underTest.execute();
    underTest.execute();

    db.executeInsert(TABLE_NAME, "uuid", "first-template", "name", "Finance");
    assertThatThrownBy(() -> db.executeInsert(TABLE_NAME, "uuid", "second-template", "name", "finance"))
      .isInstanceOf(RuntimeException.class);
  }

  @Test
  void execute_shouldAllowNamesWhoseUpperCaseValueIsLongerThanTheOriginalName() {
    assumeTrue(H2.ID.equals(db.database().getDialect().getId()), "H2 applies one-to-many uppercase mappings");
    db.executeInsert(TABLE_NAME, "uuid", "template", "name", "ß".repeat(51));

    assertThatCode(underTest::execute).doesNotThrowAnyException();
    assertThat(select("SELECT name_upper FROM permission_templates WHERE uuid = 'template'"))
      .extracting(row -> row.get("NAME_UPPER"))
      .containsExactly("SS".repeat(51));
  }

  private List<Map<String, Object>> select(String sql) {
    return db.select(sql);
  }
}
