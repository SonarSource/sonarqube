/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.startup;

import java.util.Objects;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarRuntime;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.property.PropertyDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class PropertiesDBCleanerTest {
  @RegisterExtension
  public DbTester db = DbTester.create();
  private final DbClient dbClient = db.getDbClient();
  private final DbSession dbSession = db.getSession();
  private final SonarRuntime sonarRuntime = mock(SonarRuntime.class);
  private static final String SCA_FEATURE_ENABLED_PROPERTY = "sonar.sca.featureEnabled";
  private static final String SCA_LEGACY_ENABLED_PROPERTY = "sonar.sca.enabled";

  @ParameterizedTest
  @ValueSource(strings = { "true", "false" })
  void should_not_migrate_sca_settings_when_already_set(String setting) {
    when(sonarRuntime.getEdition()).thenReturn(SonarEdition.valueOf("ENTERPRISE"));
    PropertyDto prop = new PropertyDto()
      .setKey(SCA_FEATURE_ENABLED_PROPERTY)
      .setValue(setting);
    dbClient
      .propertiesDao()
      .saveProperty(dbSession, prop, null, null, null, null);
    dbSession.commit();

    new PropertiesDBCleaner(dbClient, sonarRuntime).start();
    assertThat(Objects.requireNonNull(dbClient.propertiesDao().selectGlobalProperty(SCA_FEATURE_ENABLED_PROPERTY)).getValue()).isEqualTo(setting);
  }

  @ParameterizedTest
  @ValueSource(strings = { "true", "false" })
  void should_migrate_sca_settings_when_not_already_set(String setting) {
    when(sonarRuntime.getEdition()).thenReturn(SonarEdition.valueOf("ENTERPRISE"));
    PropertyDto prop = new PropertyDto()
      .setKey(SCA_LEGACY_ENABLED_PROPERTY)
      .setValue(setting);
    dbClient
      .propertiesDao()
      .saveProperty(dbSession, prop, null, null, null, null);
    dbSession.commit();

    new PropertiesDBCleaner(dbClient, sonarRuntime).start();
    assertThat(Objects.requireNonNull(dbClient.propertiesDao().selectGlobalProperty(SCA_FEATURE_ENABLED_PROPERTY)).getValue()).isEqualTo(setting);
  }

  @Test
  void should_initialize_sca_settings_when_nothing_set() {
    when(sonarRuntime.getEdition()).thenReturn(SonarEdition.valueOf("ENTERPRISE"));
    new PropertiesDBCleaner(dbClient, sonarRuntime).start();
    assertThat(Objects.requireNonNull(dbClient.propertiesDao().selectGlobalProperty(SCA_FEATURE_ENABLED_PROPERTY)).getValue()).isEqualTo("false");
  }
}
