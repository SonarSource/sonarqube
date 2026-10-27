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
package org.sonar.db;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.config.internal.Settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.OrchestratorSettingsUtils.DEDICATED_DATABASE_PROPERTY;

class OrchestratorSettingsUtilsTest {

  @ParameterizedTest
  @CsvSource(delimiter = '|', value = {
    "postgresql | jdbc:postgresql://localhost/sonarqube                                                       | jdbc:postgresql://localhost/server_sonar_db_dao_test",
    "postgresql | jdbc:postgresql://localhost:5432/sonarqube?currentSchema=public                             | jdbc:postgresql://localhost:5432/server_sonar_db_dao_test?currentSchema=public",
    "mssql      | jdbc:sqlserver://127.0.0.1;databaseName=sonar;SelectMethod=Cursor;trustServerCertificate=true | jdbc:sqlserver://127.0.0.1;databaseName=server_sonar_db_dao_test;SelectMethod=Cursor;trustServerCertificate=true",
    "oracle     | jdbc:oracle:thin:@127.0.0.1:1521/FREEPDB1                                                   | jdbc:oracle:thin:@127.0.0.1:1521/FREEPDB1",
  })
  void useDedicatedDatabase_switches_login_and_database(String dialect, String url, String expectedUrl) {
    Settings settings = new MapSettings()
      .setProperty("sonar.jdbc.dialect", dialect)
      .setProperty("sonar.jdbc.url", url)
      .setProperty("sonar.jdbc.username", "sonarqube")
      .setProperty(DEDICATED_DATABASE_PROPERTY, "server_sonar_db_dao_test");

    OrchestratorSettingsUtils.useDedicatedDatabase(settings);

    assertThat(settings.getString("sonar.jdbc.url")).isEqualTo(expectedUrl);
    assertThat(settings.getString("sonar.jdbc.username")).isEqualTo("server_sonar_db_dao_test");
  }

  @Test
  void useDedicatedDatabase_keeps_settings_when_no_dedicated_database() {
    Settings settings = new MapSettings()
      .setProperty("sonar.jdbc.dialect", "postgresql")
      .setProperty("sonar.jdbc.url", "jdbc:postgresql://localhost/sonarqube")
      .setProperty("sonar.jdbc.username", "sonarqube");

    OrchestratorSettingsUtils.useDedicatedDatabase(settings);

    assertThat(settings.getString("sonar.jdbc.url")).isEqualTo("jdbc:postgresql://localhost/sonarqube");
    assertThat(settings.getString("sonar.jdbc.username")).isEqualTo("sonarqube");
  }
}
