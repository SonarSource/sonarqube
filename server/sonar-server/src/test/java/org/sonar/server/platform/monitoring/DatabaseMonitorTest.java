/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.platform.monitoring;

import java.util.LinkedHashMap;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.version.DatabaseVersion;

import static org.assertj.core.api.Assertions.assertThat;

public class DatabaseMonitorTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  DatabaseMonitor underTest;

  @Before
  public void setUp() {
    DatabaseVersion dbVersion = new DatabaseVersion(dbTester.myBatis());
    underTest = new DatabaseMonitor(dbVersion, dbTester.getDbClient());
  }

  @Test
  public void db_info() {
    LinkedHashMap<String, Object> attributes = underTest.attributes();
    assertThat(attributes.get("Database")).isEqualTo("H2");
    assertThat(attributes.get("Database Version").toString()).startsWith("1.");
    assertThat(attributes.get("Username")).isEqualTo("SONAR");
    assertThat(attributes.get("Driver Version").toString()).startsWith("1.");
  }

  @Test
  public void pool_info() {
    LinkedHashMap<String, Object> attributes = underTest.attributes();
    assertThat((int) attributes.get("Pool Max Connections")).isGreaterThan(0);
  }
}
