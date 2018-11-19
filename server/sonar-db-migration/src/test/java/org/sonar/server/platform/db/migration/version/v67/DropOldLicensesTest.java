/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v67;
/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import java.sql.SQLException;
import java.util.stream.Collectors;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;

public class DropOldLicensesTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(DropOldLicensesTest.class, "properties.sql");

  private DropOldLicenses underTest = new DropOldLicenses(db.database());

  @Test
  public void remove_old_licenses() throws SQLException {
    insertProperty("sonar.cpp.license.secured");
    insertProperty("sonar.cpp.licenseHash.secured");
    insertProperty("sonar.objc.license.secured");
    insertProperty("sonar.objc.licenseHash.secured");

    underTest.execute();

    assertPropertiesIsEmpty();
  }

  @Test
  public void ignore_existing_none_related_licenses_settings() throws SQLException {
    insertProperty("my.property");
    insertProperty("custom.license");

    underTest.execute();

    assertProperties("my.property", "custom.license");
  }

  private void assertPropertiesIsEmpty() {
    assertProperties();
  }

  private void assertProperties(String... expectedSettingKeys) {
    assertThat(db.select("SELECT PROP_KEY FROM PROPERTIES")
      .stream()
      .map(map -> map.get("PROP_KEY"))
      .collect(Collectors.toList()))
        .containsExactlyInAnyOrder(expectedSettingKeys);
  }

  public void insertProperty(String propertyKey) {
    db.executeInsert(
      "properties",
      "prop_key", propertyKey,
      "is_empty", "false",
      "text_value", randomAlphabetic(2));
  }

}
