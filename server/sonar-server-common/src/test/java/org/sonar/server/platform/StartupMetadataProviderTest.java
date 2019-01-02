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
import org.junit.rules.ExpectedException;
import org.sonar.api.CoreProperties;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.Version;
import org.sonar.db.DbTester;
import org.sonar.db.property.PropertyDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.api.utils.DateUtils.formatDateTime;

public class StartupMetadataProviderTest {

  private static final long A_DATE = 1_500_000_000_000L;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private StartupMetadataProvider underTest = new StartupMetadataProvider();
  private System2 system = mock(System2.class);
  private WebServer webServer = mock(WebServer.class);

  @Test
  public void generate_SERVER_STARTIME_but_do_not_persist_it_if_server_is_startup_leader() {
    when(system.now()).thenReturn(A_DATE);
    SonarRuntime runtime = SonarRuntimeImpl.forSonarQube(Version.create(6, 1), SonarQubeSide.SERVER);
    when(webServer.isStartupLeader()).thenReturn(true);

    StartupMetadata metadata = underTest.provide(system, runtime, webServer, dbTester.getDbClient());
    assertThat(metadata.getStartedAt()).isEqualTo(A_DATE);

    assertNotPersistedProperty(CoreProperties.SERVER_STARTTIME);

    // keep a cache
    StartupMetadata secondMetadata = underTest.provide(system, runtime, webServer, dbTester.getDbClient());
    assertThat(secondMetadata).isSameAs(metadata);
  }

  @Test
  public void load_from_database_if_server_is_startup_follower() {
    SonarRuntime runtime = SonarRuntimeImpl.forSonarQube(Version.create(6, 1), SonarQubeSide.SERVER);
    when(webServer.isStartupLeader()).thenReturn(false);

    testLoadingFromDatabase(runtime, false);
  }

  @Test
  public void load_from_database_if_compute_engine_of_startup_leader_server() {
    SonarRuntime runtime = SonarRuntimeImpl.forSonarQube(Version.create(6, 1), SonarQubeSide.COMPUTE_ENGINE);

    testLoadingFromDatabase(runtime, true);
  }

  @Test
  public void load_from_database_if_compute_engine_of_startup_follower_server() {
    SonarRuntime runtime = SonarRuntimeImpl.forSonarQube(Version.create(6, 1), SonarQubeSide.COMPUTE_ENGINE);

    testLoadingFromDatabase(runtime, false);
  }

  @Test
  public void fail_to_load_from_database_if_properties_are_not_persisted() {
    SonarRuntime runtime = SonarRuntimeImpl.forSonarQube(Version.create(6, 1), SonarQubeSide.COMPUTE_ENGINE);
    when(webServer.isStartupLeader()).thenReturn(false);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Property sonar.core.startTime is missing in database");
    underTest.provide(system, runtime, webServer, dbTester.getDbClient());
  }

  private void testLoadingFromDatabase(SonarRuntime runtime, boolean isStartupLeader) {
    dbTester.properties().insertProperty(new PropertyDto().setKey(CoreProperties.SERVER_STARTTIME).setValue(formatDateTime(A_DATE)));
    when(webServer.isStartupLeader()).thenReturn(isStartupLeader);

    StartupMetadata metadata = underTest.provide(system, runtime, webServer, dbTester.getDbClient());
    assertThat(metadata.getStartedAt()).isEqualTo(A_DATE);

    // still in database
    assertPersistedProperty(CoreProperties.SERVER_STARTTIME, formatDateTime(A_DATE));

    // keep a cache
    StartupMetadata secondMetadata = underTest.provide(system, runtime, webServer, dbTester.getDbClient());
    assertThat(secondMetadata).isSameAs(metadata);

    verifyZeroInteractions(system);
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
