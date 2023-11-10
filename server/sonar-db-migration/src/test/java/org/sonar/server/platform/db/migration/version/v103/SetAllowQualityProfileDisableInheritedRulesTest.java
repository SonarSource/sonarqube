/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v103;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.server.platform.db.migration.MigrationDbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class SetAllowQualityProfileDisableInheritedRulesTest {

  private static final long NOW = 1;

  @Rule
  public final MigrationDbTester dbTester = MigrationDbTester.createForMigrationStep(SetAllowQualityProfileDisableInheritedRules.class);
  private final System2 system2 = new TestSystem2().setNow(NOW);

  private final SetAllowQualityProfileDisableInheritedRules script = new SetAllowQualityProfileDisableInheritedRules(dbTester.database(), system2, UuidFactoryFast.getInstance());

  @Test
  public void execute_shouldInsertPropertyWithFalseValue() throws SQLException {
    script.execute();

    assertThatForceAuthenticationEquals("false");
  }

  @Test
  public void execute_shouldBeReentrant() throws SQLException {
    script.execute();
    // re-entrant
    script.execute();

    assertThatForceAuthenticationEquals("false");
  }

  @Test
  public void execute_shouldNotUpdateTheValueThatAlreadyExistsInTheDatabase() throws SQLException {
    insertPropertyWithValueAsTrue();
    script.execute();

    assertThatForceAuthenticationEquals("true");
  }

  private void assertThatForceAuthenticationEquals(String s) {
    assertThat(dbTester.selectFirst("select p.text_value from properties p where p.prop_key = 'sonar.qualityProfiles.allowDisableInheritedRules'"))
      .containsEntry("TEXT_VALUE", s);
  }

  private void insertPropertyWithValueAsTrue() {
    dbTester.executeInsert("properties",
      "uuid", "uuid-1",
      "prop_key", "sonar.qualityProfiles.allowDisableInheritedRules",
      "is_empty", false,
      "text_value", "true",
      "created_at", NOW);
  }
}
