/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.CoreProperties;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.Version;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.property.PropertyDto;
import org.sonar.server.property.InternalProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.SonarQubeSide.COMPUTE_ENGINE;
import static org.sonar.api.SonarQubeSide.SERVER;

public class ServerIdManagerTest {

  private static final String A_SERVER_ID = "uuid";
  private static final String A_JDBC_URL = "jdbc:postgres:foo";
  private static final String A_VALID_CHECKSUM = ServerIdChecksum.of(A_SERVER_ID, A_JDBC_URL);

  @Rule
  public final DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private Configuration config = new MapSettings().setProperty("sonar.jdbc.url", A_JDBC_URL).asConfig();
  private DbClient dbClient = dbTester.getDbClient();
  private DbSession dbSession = dbTester.getSession();
  private WebServer webServer = mock(WebServer.class);
  private UuidFactory uuidFactory = mock(UuidFactory.class);
  private ServerIdManager underTest;

  @After
  public void tearDown() {
    if (underTest != null) {
      underTest.stop();
    }
  }

  @Test
  public void web_leader_persists_new_server_id_if_missing() {
    when(uuidFactory.create()).thenReturn(A_SERVER_ID);
    when(webServer.isStartupLeader()).thenReturn(true);

    test(SERVER);

    verifyDb(A_SERVER_ID, A_VALID_CHECKSUM);
  }

  @Test
  public void web_leader_persists_new_server_id_if_format_is_old_date() {
    insertServerId("20161123150657");
    when(uuidFactory.create()).thenReturn(A_SERVER_ID);
    when(webServer.isStartupLeader()).thenReturn(true);

    test(SERVER);

    verifyDb(A_SERVER_ID, A_VALID_CHECKSUM);
  }

  @Test
  public void web_leader_persists_new_server_id_if_value_is_empty() {
    insertServerId("");
    when(uuidFactory.create()).thenReturn(A_SERVER_ID);
    when(webServer.isStartupLeader()).thenReturn(true);

    test(SERVER);

    verifyDb(A_SERVER_ID, A_VALID_CHECKSUM);
  }

  @Test
  public void web_leader_keeps_existing_server_id_if_valid() {
    insertServerId(A_SERVER_ID);
    insertChecksum(A_VALID_CHECKSUM);
    when(webServer.isStartupLeader()).thenReturn(true);

    test(SERVER);

    verifyDb(A_SERVER_ID, A_VALID_CHECKSUM);
  }

  @Test
  public void web_leader_resets_server_id_if_invalid() {
    insertServerId("foo");
    insertChecksum("invalid");
    when(uuidFactory.create()).thenReturn(A_SERVER_ID);
    when(webServer.isStartupLeader()).thenReturn(true);

    test(SERVER);

    verifyDb(A_SERVER_ID, A_VALID_CHECKSUM);
  }

  @Test
  public void web_leader_generates_missing_checksum() {
    insertServerId(A_SERVER_ID);
    when(webServer.isStartupLeader()).thenReturn(true);

    test(SERVER);

    verifyDb(A_SERVER_ID, A_VALID_CHECKSUM);
  }

  @Test
  public void web_follower_does_not_fail_if_server_id_is_valid() {
    insertServerId(A_SERVER_ID);
    insertChecksum(A_VALID_CHECKSUM);
    when(webServer.isStartupLeader()).thenReturn(false);

    test(SERVER);

    // no changes
    verifyDb(A_SERVER_ID, A_VALID_CHECKSUM);
  }

  @Test
  public void web_follower_fails_if_server_id_is_missing() {
    when(webServer.isStartupLeader()).thenReturn(false);

    expectMissingServerIdException();

    test(SERVER);
  }

  @Test
  public void web_follower_fails_if_server_id_is_empty() {
    insertServerId("");
    when(webServer.isStartupLeader()).thenReturn(false);

    expectEmptyServerIdException();

    test(SERVER);
  }

  @Test
  public void web_follower_fails_if_server_id_is_invalid() {
    insertServerId(A_SERVER_ID);
    insertChecksum("boom");
    when(webServer.isStartupLeader()).thenReturn(false);

    expectInvalidServerIdException();

    test(SERVER);

    // no changes
    verifyDb(A_SERVER_ID, "boom");
  }

  @Test
  public void compute_engine_does_not_fail_if_server_id_is_valid() {
    insertServerId(A_SERVER_ID);
    insertChecksum(A_VALID_CHECKSUM);

    test(COMPUTE_ENGINE);

    // no changes
    verifyDb(A_SERVER_ID, A_VALID_CHECKSUM);
  }

  @Test
  public void compute_engine_fails_if_server_id_is_missing() {
    expectMissingServerIdException();

    test(COMPUTE_ENGINE);
  }

  @Test
  public void compute_engine_fails_if_server_id_is_empty() {
    insertServerId("");

    expectEmptyServerIdException();

    test(COMPUTE_ENGINE);
  }

  @Test
  public void compute_engine_fails_if_server_id_is_invalid() {
    insertServerId(A_SERVER_ID);
    insertChecksum("boom");

    expectInvalidServerIdException();

    test(COMPUTE_ENGINE);

    // no changes
    verifyDb(A_SERVER_ID, "boom");
  }

  private void expectEmptyServerIdException() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Property sonar.core.id is empty in database");
  }

  private void expectMissingServerIdException() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Property sonar.core.id is missing in database");
  }

  private void expectInvalidServerIdException() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Server ID is invalid");
  }

  private void verifyDb(String expectedServerId, String expectedChecksum) {
    assertThat(dbClient.propertiesDao().selectGlobalProperty(dbSession, CoreProperties.SERVER_ID))
      .extracting(PropertyDto::getValue)
      .containsExactly(expectedServerId);
    assertThat(dbClient.internalPropertiesDao().selectByKey(dbSession, InternalProperties.SERVER_ID_CHECKSUM))
      .hasValue(expectedChecksum);
  }

  private void insertServerId(String value) {
    dbClient.propertiesDao().saveProperty(dbSession, new PropertyDto().setKey(CoreProperties.SERVER_ID).setValue(value));
    dbSession.commit();
  }

  private void insertChecksum(String value) {
    dbClient.internalPropertiesDao().save(dbSession, InternalProperties.SERVER_ID_CHECKSUM, value);
    dbSession.commit();
  }

  private void test(SonarQubeSide side) {
    underTest = new ServerIdManager(config, dbClient, SonarRuntimeImpl.forSonarQube(Version.create(6, 7), side), webServer, uuidFactory);
    underTest.start();
  }
}
