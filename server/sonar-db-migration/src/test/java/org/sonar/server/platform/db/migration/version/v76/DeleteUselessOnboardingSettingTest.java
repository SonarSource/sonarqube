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
package org.sonar.server.platform.db.migration.version.v76;

import com.google.common.collect.ImmutableMap;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.valueOf;
import static org.assertj.core.api.Assertions.assertThat;

public class DeleteUselessOnboardingSettingTest {

  private static final String TABLE_PROPERTIES = "properties";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(DeleteUselessOnboardingSettingTest.class, "properties.sql");

  private DeleteUselessOnboardingSetting underTest = new DeleteUselessOnboardingSetting(db.database());

  @Test
  public void migration_has_no_effect_on_empty_tables() throws SQLException {
    underTest.execute();

    assertThat(db.countRowsOfTable(TABLE_PROPERTIES)).isZero();
  }

  @Test
  public void migration_removes_onboarding_setting() throws SQLException {
    insertProperty("sonar.onboardingTutorial.showToNewUsers");

    underTest.execute();

    assertThat(db.countRowsOfTable(TABLE_PROPERTIES)).isZero();
  }

  private void insertProperty(String key) {
    Map<String, Object> values = new HashMap<>(ImmutableMap.of(
      "PROP_KEY", key,
      "IS_EMPTY", false,
      "CREATED_AT", 456789));
    db.executeInsert(TABLE_PROPERTIES, values);
  }

}
