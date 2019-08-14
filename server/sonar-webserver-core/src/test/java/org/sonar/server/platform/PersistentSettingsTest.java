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
package org.sonar.server.platform;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.server.setting.SettingsChangeNotifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class PersistentSettingsTest {
  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  private Settings delegate = new MapSettings();
  private SettingsChangeNotifier changeNotifier = mock(SettingsChangeNotifier.class);
  private PersistentSettings underTest = new PersistentSettings(delegate, dbTester.getDbClient(), changeNotifier);

  @Test
  public void insert_property_into_database_and_notify_extensions() {
    assertThat(underTest.getString("foo")).isNull();

    underTest.saveProperty("foo", "bar");

    assertThat(underTest.getString("foo")).isEqualTo("bar");
    assertThat(dbTester.getDbClient().propertiesDao().selectGlobalProperty("foo").getValue()).isEqualTo("bar");
    verify(changeNotifier).onGlobalPropertyChange("foo", "bar");
  }

  @Test
  public void delete_property_from_database_and_notify_extensions() {
    underTest.saveProperty("foo", "bar");
    underTest.saveProperty("foo", null);

    assertThat(underTest.getString("foo")).isNull();
    assertThat(dbTester.getDbClient().propertiesDao().selectGlobalProperty("foo")).isNull();
    verify(changeNotifier).onGlobalPropertyChange("foo", null);
  }

  @Test
  public void getSettings_returns_delegate() {
    assertThat(underTest.getSettings()).isSameAs(delegate);
  }
}
