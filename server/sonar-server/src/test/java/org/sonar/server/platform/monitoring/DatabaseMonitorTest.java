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
package org.sonar.server.platform.monitoring;

import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.server.platform.db.migration.version.DatabaseVersion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DatabaseMonitorTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private DatabaseVersion databaseVersion = mock(DatabaseVersion.class);
  private DatabaseMonitor underTest = new DatabaseMonitor(databaseVersion, dbTester.getDbClient());

  @Before
  public void setUp() throws Exception {
    when(databaseVersion.getStatus()).thenReturn(DatabaseVersion.Status.UP_TO_DATE);
  }

  @Test
  public void name_is_not_empty() {
    assertThat(underTest.name()).isNotEmpty();
  }

  @Test
  public void db_info() {
    Map<String, Object> attributes = underTest.attributes();
    assertThat(attributes.get("Database")).isEqualTo("H2");
    assertThat(attributes.get("Database Version").toString()).startsWith("1.");
    assertThat(attributes.get("Username")).isEqualTo("SONAR");
    assertThat(attributes.get("Driver Version").toString()).startsWith("1.");
  }

  @Test
  public void pool_info() {
    Map<String, Object> attributes = underTest.attributes();
    assertThat((int) attributes.get("Pool Max Connections")).isGreaterThan(0);
  }
}
