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
package org.sonar.ce.platform;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Arrays;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.server.platform.db.migration.version.DatabaseVersion;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.server.platform.db.migration.version.DatabaseVersion.Status.FRESH_INSTALL;
import static org.sonar.server.platform.db.migration.version.DatabaseVersion.Status.UP_TO_DATE;

@RunWith(DataProviderRunner.class)
public class DatabaseCompatibilityTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DatabaseVersion databaseVersion = mock(DatabaseVersion.class);
  private DatabaseCompatibility underTest = new DatabaseCompatibility(databaseVersion);

  @Test
  @UseDataProvider("anyStatusButUpToDateOrFreshInstall")
  public void start_throws_ISE_if_status_is_not_UP_TO_DATE_nor_FRESH_INSTALL(DatabaseVersion.Status status) {
    when(databaseVersion.getStatus()).thenReturn(status);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Compute Engine can't start unless Database is up to date");

    underTest.start();
  }

  @DataProvider
  public static Object[][] anyStatusButUpToDateOrFreshInstall() {
    return Arrays.stream(DatabaseVersion.Status.values())
      .filter(t -> t != UP_TO_DATE && t != FRESH_INSTALL)
      .map(t -> new Object[] {t})
      .toArray(Object[][]::new);
  }

  @Test
  public void start_has_no_effect_if_status_is_UP_TO_DATE() {
    when(databaseVersion.getStatus()).thenReturn(UP_TO_DATE);

    underTest.start();

    verify(databaseVersion).getStatus();
    verifyNoMoreInteractions(databaseVersion);
  }

  @Test
  public void start_has_no_effect_if_status_is_FRESH_INSTALL() {
    when(databaseVersion.getStatus()).thenReturn(DatabaseVersion.Status.FRESH_INSTALL);

    underTest.start();

    verify(databaseVersion).getStatus();
    verifyNoMoreInteractions(databaseVersion);
  }

  @Test
  public void stop_has_no_effect() {
    underTest.stop();

    verifyZeroInteractions(databaseVersion);
  }
}
