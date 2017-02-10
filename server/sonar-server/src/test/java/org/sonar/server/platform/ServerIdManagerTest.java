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
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.property.PropertyDto;
import org.sonar.server.platform.cluster.ClusterMock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.SonarQubeSide.COMPUTE_ENGINE;
import static org.sonar.api.SonarQubeSide.SERVER;

public class ServerIdManagerTest {
  private static final Version SOME_VERSION = Version.create(5, 6);
  private static final String SOME_UUID = "some uuid";

  @Rule
  public final DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DbClient dbClient = dbTester.getDbClient();
  private DbSession dbSession = dbTester.getSession();
  private ClusterMock cluster = new ClusterMock();
  private UuidFactory uuidFactory = mock(UuidFactory.class);

  private static SonarRuntime runtimeFor(SonarQubeSide side) {
    return SonarRuntimeImpl.forSonarQube(SOME_VERSION, side);
  }

  @Test
  public void start_persists_new_serverId_if_server_startupLeader_and_serverId_does_not_exist() {
    when(uuidFactory.create()).thenReturn(SOME_UUID);
    cluster.setStartupLeader(true);

    new ServerIdManager(dbClient, runtimeFor(SERVER), cluster, uuidFactory)
      .start();

    assertThat(dbClient.propertiesDao().selectGlobalProperty(dbSession, CoreProperties.SERVER_ID))
      .extracting(PropertyDto::getValue)
      .containsOnly(SOME_UUID);
  }

  @Test
  public void start_persists_new_serverId_if_server_startupLeader_and_serverId_is_an_old_date() {
    insertPropertyCoreId("20161123150657");
    when(uuidFactory.create()).thenReturn(SOME_UUID);
    cluster.setStartupLeader(true);

    new ServerIdManager(dbClient, runtimeFor(SERVER), cluster, uuidFactory)
        .start();

    assertThat(dbClient.propertiesDao().selectGlobalProperty(dbSession, CoreProperties.SERVER_ID))
        .extracting(PropertyDto::getValue)
        .containsOnly(SOME_UUID);
  }

  private void insertPropertyCoreId(String value) {
    dbClient.propertiesDao().saveProperty(dbSession, new PropertyDto().setKey(CoreProperties.SERVER_ID).setValue(value));
    dbSession.commit();
  }

  @Test
  public void start_persists_new_serverId_if_server_startupLeader_and_serverId_is_empty() {
    insertPropertyCoreId("");
    when(uuidFactory.create()).thenReturn(SOME_UUID);
    cluster.setStartupLeader(true);

    new ServerIdManager(dbClient, runtimeFor(SERVER), cluster, uuidFactory)
        .start();

    assertThat(dbClient.propertiesDao().selectGlobalProperty(dbSession, CoreProperties.SERVER_ID))
        .extracting(PropertyDto::getValue)
        .containsOnly(SOME_UUID);
  }

  @Test
  public void start_fails_with_ISE_if_serverId_is_null_and_server_is_not_startupLeader() {
    cluster.setStartupLeader(false);

    ServerIdManager underTest = new ServerIdManager(dbClient, runtimeFor(SERVER), cluster, uuidFactory);

    expectMissingCoreIdException();
    
    underTest.start();
  }

  @Test
  public void start_fails_with_ISE_if_serverId_is_empty_and_server_is_not_startupLeader() {
    insertPropertyCoreId("");
    cluster.setStartupLeader(false);

    ServerIdManager underTest = new ServerIdManager(dbClient, runtimeFor(SERVER), cluster, uuidFactory);

    expectEmptyCoreIdException();

    underTest.start();
  }

  @Test
  public void start_fails_with_ISE_if_serverId_is_null_and_not_server() {
    cluster.setStartupLeader(false);

    ServerIdManager underTest = new ServerIdManager(dbClient, runtimeFor(COMPUTE_ENGINE), cluster, uuidFactory);

    expectMissingCoreIdException();

    underTest.start();
  }

  @Test
  public void start_fails_with_ISE_if_serverId_is_empty_and_not_server() {
    insertPropertyCoreId("");

    ServerIdManager underTest = new ServerIdManager(dbClient, runtimeFor(COMPUTE_ENGINE), cluster, uuidFactory);

    expectEmptyCoreIdException();

    underTest.start();
  }

  @Test
  public void start_does_not_fail_if_serverId_exists_and_server_is_not_startupLeader() {
    insertPropertyCoreId(SOME_UUID);
    cluster.setStartupLeader(false);

    new ServerIdManager(dbClient, runtimeFor(SERVER), cluster, uuidFactory).start();
  }

  @Test
  public void start_does_not_fail_if_serverId_exists_and_not_server() {
    insertPropertyCoreId(SOME_UUID);

    new ServerIdManager(dbClient, runtimeFor(COMPUTE_ENGINE), cluster, uuidFactory).start();
  }

  private void expectEmptyCoreIdException() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Property sonar.core.id is set but empty in database");
  }

  private void expectMissingCoreIdException() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Property sonar.core.id is missing in database");
  }
}
