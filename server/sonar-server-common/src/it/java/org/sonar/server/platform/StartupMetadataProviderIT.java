/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import org.sonar.api.CoreProperties;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.Version;
import org.sonar.db.DbTester;
import org.sonar.db.property.PropertyDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.api.utils.DateUtils.formatDateTime;

public class StartupMetadataProviderIT {
  private static final long A_DATE = 1_500_000_000_000L;

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private final StartupMetadataProvider underTest = new StartupMetadataProvider();
  private final System2 system = mock(System2.class);
  private final NodeInformation nodeInformation = mock(NodeInformation.class);

  @Test
  public void generate_SERVER_STARTIME_but_do_not_persist_it_if_server_is_startup_leader() {
    when(system.now()).thenReturn(A_DATE);
    SonarRuntime runtime = SonarRuntimeImpl.forSonarQube(Version.create(6, 1), SonarQubeSide.SERVER, SonarEdition.COMMUNITY);
    when(nodeInformation.isStartupLeader()).thenReturn(true);

    StartupMetadata metadata = underTest.provide(system, runtime, nodeInformation, dbTester.getDbClient());
    assertThat(metadata.getStartedAt()).isEqualTo(A_DATE);

    assertNotPersistedProperty(CoreProperties.SERVER_STARTTIME);
  }

  @Test
  public void load_from_database_if_server_is_startup_follower() {
    SonarRuntime runtime = SonarRuntimeImpl.forSonarQube(Version.create(6, 1), SonarQubeSide.SERVER, SonarEdition.COMMUNITY);
    when(nodeInformation.isStartupLeader()).thenReturn(false);

    testLoadingFromDatabase(runtime, false);
  }

  @Test
  public void load_from_database_if_compute_engine_of_startup_leader_server() {
    SonarRuntime runtime = SonarRuntimeImpl.forSonarQube(Version.create(6, 1), SonarQubeSide.COMPUTE_ENGINE, SonarEdition.COMMUNITY);

    testLoadingFromDatabase(runtime, true);
  }

  @Test
  public void load_from_database_if_compute_engine_of_startup_follower_server() {
    SonarRuntime runtime = SonarRuntimeImpl.forSonarQube(Version.create(6, 1), SonarQubeSide.COMPUTE_ENGINE, SonarEdition.COMMUNITY);

    testLoadingFromDatabase(runtime, false);
  }

  @Test
  public void fail_to_load_from_database_if_properties_are_not_persisted() {
    SonarRuntime runtime = SonarRuntimeImpl.forSonarQube(Version.create(6, 1), SonarQubeSide.COMPUTE_ENGINE, SonarEdition.COMMUNITY);
    when(nodeInformation.isStartupLeader()).thenReturn(false);

    assertThatThrownBy(() -> underTest.provide(system, runtime, nodeInformation, dbTester.getDbClient()))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Property sonar.core.startTime is missing in database");
  }

  private void testLoadingFromDatabase(SonarRuntime runtime, boolean isStartupLeader) {
    dbTester.properties().insertProperty(new PropertyDto().setKey(CoreProperties.SERVER_STARTTIME).setValue(formatDateTime(A_DATE)),
      null, null,null, null);
    when(nodeInformation.isStartupLeader()).thenReturn(isStartupLeader);

    StartupMetadata metadata = underTest.provide(system, runtime, nodeInformation, dbTester.getDbClient());
    assertThat(metadata.getStartedAt()).isEqualTo(A_DATE);

    // still in database
    assertPersistedProperty(CoreProperties.SERVER_STARTTIME, formatDateTime(A_DATE));

    verifyNoInteractions(system);
  }

  private void assertPersistedProperty(String propertyKey, String expectedValue) {
    PropertyDto prop = dbTester.getDbClient().propertiesDao().selectGlobalProperty(dbTester.getSession(), propertyKey);
    assertThat(prop.getValue()).isEqualTo(expectedValue);
  }

  private void assertNotPersistedProperty(String propertyKey) {
    PropertyDto prop = dbTester.getDbClient().propertiesDao().selectGlobalProperty(dbTester.getSession(), propertyKey);
    assertThat(prop).isNull();
  }
}
