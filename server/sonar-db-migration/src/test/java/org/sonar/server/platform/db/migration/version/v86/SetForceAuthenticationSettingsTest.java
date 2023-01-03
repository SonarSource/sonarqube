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
package org.sonar.server.platform.db.migration.version.v86;

import java.sql.SQLException;
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.Settings;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SetForceAuthenticationSettingsTest {

  private static final long NOW = 100_000_000_000L;

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(SetForceAuthenticationSettingsTest.class, "schema.sql");

  private final System2 system2 = new TestSystem2().setNow(NOW);

  private final Settings settingsMock = mock(Settings.class);

  private final DataChange underTest = new SetForceAuthenticationSettings(db.database(), system2, UuidFactoryFast.getInstance(), settingsMock);

  @Test
  public void insert_force_auth_property_based_on_settings_when_false() throws SQLException {
    when(settingsMock.getRawString(any())).thenReturn(Optional.of("false"));
    underTest.execute();

    assertThatForceAuthenticationEquals("false");
  }

  @Test
  public void insert_force_auth_property_based_on_settings_when_true() throws SQLException {
    when(settingsMock.getRawString(any())).thenReturn(Optional.of("true"));
    underTest.execute();

    assertThatForceAuthenticationEquals("true");
  }

  @Test
  public void insert_force_auth_property_based_on_settings_when_empty() throws SQLException {
    when(settingsMock.getRawString(any())).thenReturn(Optional.empty());
    underTest.execute();

    assertThatForceAuthenticationEquals("false");
  }

  @Test
  public void insert_force_auth_property_if_row_not_exists() throws SQLException {
    underTest.execute();

    assertThatForceAuthenticationEquals("false");
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    underTest.execute();
    // re-entrant
    underTest.execute();

    assertThatForceAuthenticationEquals("false");
  }

  @Test
  public void do_nothing_if_force_auth_property_exists_with_value_false() throws SQLException {
    insertProperty("uuid-1", "sonar.forceAuthentication", "false");
    underTest.execute();

    assertThatForceAuthenticationEquals("false");
  }

  @Test
  public void do_nothing_if_force_auth_property_exists_with_value_true() throws SQLException {
    insertProperty("uuid-1", "sonar.forceAuthentication", "true");
    underTest.execute();

    assertThatForceAuthenticationEquals("true");
  }

  private void assertThatForceAuthenticationEquals(String s) {
    assertThat(db.selectFirst("select p.text_value from properties p where p.prop_key = 'sonar.forceAuthentication'"))
      .containsEntry("TEXT_VALUE", s);
  }

  private void insertProperty(String uuid, String key, String textValue) {
    db.executeInsert("properties",
      "uuid", uuid,
      "prop_key", key,
      "is_empty", false,
      "text_value", textValue,
      "created_at", NOW);
  }
}
