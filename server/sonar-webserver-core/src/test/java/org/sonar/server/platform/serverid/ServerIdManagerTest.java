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
package org.sonar.server.platform.serverid;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.sonar.api.SonarEdition;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.api.CoreProperties;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.Version;
import org.sonar.core.platform.ServerId;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.property.PropertyDto;
import org.sonar.server.platform.WebServer;
import org.sonar.server.property.InternalProperties;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.api.SonarQubeSide.COMPUTE_ENGINE;
import static org.sonar.api.SonarQubeSide.SERVER;
import static org.sonar.core.platform.ServerId.DATABASE_ID_LENGTH;
import static org.sonar.core.platform.ServerId.NOT_UUID_DATASET_ID_LENGTH;
import static org.sonar.core.platform.ServerId.UUID_DATASET_ID_LENGTH;

@RunWith(DataProviderRunner.class)
public class ServerIdManagerTest {

  private static final ServerId OLD_FORMAT_SERVER_ID = ServerId.parse("20161123150657");
  private static final ServerId NO_DATABASE_ID_SERVER_ID = ServerId.parse(randomAlphanumeric(UUID_DATASET_ID_LENGTH));
  private static final ServerId WITH_DATABASE_ID_SERVER_ID = ServerId.of(randomAlphanumeric(DATABASE_ID_LENGTH), randomAlphanumeric(NOT_UUID_DATASET_ID_LENGTH));
  private static final String CHECKSUM_1 = randomAlphanumeric(12);

  @Rule
  public final DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private ServerIdChecksum serverIdChecksum = mock(ServerIdChecksum.class);
  private ServerIdFactory serverIdFactory = mock(ServerIdFactory.class);
  private DbClient dbClient = dbTester.getDbClient();
  private DbSession dbSession = dbTester.getSession();
  private WebServer webServer = mock(WebServer.class);
  private ServerIdManager underTest;

  @After
  public void tearDown() {
    if (underTest != null) {
      underTest.stop();
    }
  }

  @Test
  public void web_leader_persists_new_server_id_if_missing() {
    mockCreateNewServerId(WITH_DATABASE_ID_SERVER_ID);
    mockChecksumOf(WITH_DATABASE_ID_SERVER_ID, CHECKSUM_1);
    when(webServer.isStartupLeader()).thenReturn(true);

    test(SERVER);

    verifyDb(WITH_DATABASE_ID_SERVER_ID, CHECKSUM_1);
    verifyCreateNewServerIdFromScratch();
  }

  @Test
  public void web_leader_persists_new_server_id_if_format_is_old_date() {
    insertServerId(OLD_FORMAT_SERVER_ID);
    mockCreateNewServerId(WITH_DATABASE_ID_SERVER_ID);
    mockChecksumOf(WITH_DATABASE_ID_SERVER_ID, CHECKSUM_1);
    when(webServer.isStartupLeader()).thenReturn(true);

    test(SERVER);

    verifyDb(WITH_DATABASE_ID_SERVER_ID, CHECKSUM_1);
    verifyCreateNewServerIdFromScratch();
  }

  @Test
  public void web_leader_persists_new_server_id_if_value_is_empty() {
    insertServerId("");
    mockCreateNewServerId(WITH_DATABASE_ID_SERVER_ID);
    mockChecksumOf(WITH_DATABASE_ID_SERVER_ID, CHECKSUM_1);
    when(webServer.isStartupLeader()).thenReturn(true);

    test(SERVER);

    verifyDb(WITH_DATABASE_ID_SERVER_ID, CHECKSUM_1);
    verifyCreateNewServerIdFromScratch();
  }

  @Test
  public void web_leader_keeps_existing_server_id_if_valid() {
    insertServerId(WITH_DATABASE_ID_SERVER_ID);
    insertChecksum(CHECKSUM_1);
    mockChecksumOf(WITH_DATABASE_ID_SERVER_ID, CHECKSUM_1);
    when(webServer.isStartupLeader()).thenReturn(true);

    test(SERVER);

    verifyDb(WITH_DATABASE_ID_SERVER_ID, CHECKSUM_1);
  }

  @Test
  public void web_leader_creates_server_id_from_scratch_if_checksum_fails_for_serverId_in_deprecated_format() {
    ServerId currentServerId = OLD_FORMAT_SERVER_ID;
    insertServerId(currentServerId);
    insertChecksum("invalid");
    mockChecksumOf(currentServerId, "valid");
    mockCreateNewServerId(WITH_DATABASE_ID_SERVER_ID);
    mockChecksumOf(WITH_DATABASE_ID_SERVER_ID, CHECKSUM_1);
    when(webServer.isStartupLeader()).thenReturn(true);

    test(SERVER);

    verifyDb(WITH_DATABASE_ID_SERVER_ID, CHECKSUM_1);
    verifyCreateNewServerIdFromScratch();
  }

  @Test
  public void web_leader_creates_server_id_from_current_serverId_without_databaseId_if_checksum_fails() {
    ServerId currentServerId = ServerId.parse(randomAlphanumeric(UUID_DATASET_ID_LENGTH));
    insertServerId(currentServerId);
    insertChecksum("does_not_match_WITH_DATABASE_ID_SERVER_ID");
    mockChecksumOf(currentServerId, "matches_WITH_DATABASE_ID_SERVER_ID");
    mockCreateNewServerIdFrom(currentServerId, WITH_DATABASE_ID_SERVER_ID);
    mockChecksumOf(WITH_DATABASE_ID_SERVER_ID, CHECKSUM_1);
    when(webServer.isStartupLeader()).thenReturn(true);

    test(SERVER);

    verifyDb(WITH_DATABASE_ID_SERVER_ID, CHECKSUM_1);
    verifyCreateNewServerIdFrom(currentServerId);
  }

  @Test
  public void web_leader_creates_server_id_from_current_serverId_with_databaseId_if_checksum_fails() {
    ServerId currentServerId = ServerId.of(randomAlphanumeric(DATABASE_ID_LENGTH), randomAlphanumeric(UUID_DATASET_ID_LENGTH));
    insertServerId(currentServerId);
    insertChecksum("does_not_match_WITH_DATABASE_ID_SERVER_ID");
    mockChecksumOf(currentServerId, "matches_WITH_DATABASE_ID_SERVER_ID");
    mockCreateNewServerIdFrom(currentServerId, WITH_DATABASE_ID_SERVER_ID);
    mockChecksumOf(WITH_DATABASE_ID_SERVER_ID, CHECKSUM_1);
    when(webServer.isStartupLeader()).thenReturn(true);

    test(SERVER);

    verifyDb(WITH_DATABASE_ID_SERVER_ID, CHECKSUM_1);
    verifyCreateNewServerIdFrom(currentServerId);
  }

  @Test
  public void web_leader_generates_missing_checksum_for_current_serverId_with_databaseId() {
    insertServerId(WITH_DATABASE_ID_SERVER_ID);
    mockChecksumOf(WITH_DATABASE_ID_SERVER_ID, CHECKSUM_1);
    when(webServer.isStartupLeader()).thenReturn(true);

    test(SERVER);

    verifyDb(WITH_DATABASE_ID_SERVER_ID, CHECKSUM_1);
  }

  @Test
  @UseDataProvider("allFormatsOfServerId")
  public void web_follower_does_not_fail_if_server_id_matches_checksum(ServerId serverId) {
    insertServerId(serverId);
    insertChecksum(CHECKSUM_1);
    mockChecksumOf(serverId, CHECKSUM_1);
    when(webServer.isStartupLeader()).thenReturn(false);

    test(SERVER);

    // no changes
    verifyDb(serverId, CHECKSUM_1);
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
  @UseDataProvider("allFormatsOfServerId")
  public void web_follower_fails_if_checksum_does_not_match(ServerId serverId) {
    String dbChecksum = "boom";
    insertServerId(serverId);
    insertChecksum(dbChecksum);
    mockChecksumOf(serverId, CHECKSUM_1);
    when(webServer.isStartupLeader()).thenReturn(false);

    try {
      test(SERVER);
      fail("An ISE should have been raised");
    }
    catch (IllegalStateException e) {
      assertThat(e.getMessage()).isEqualTo("Server ID is invalid");
      // no changes
      verifyDb(serverId, dbChecksum);
    }
  }

  @Test
  @UseDataProvider("allFormatsOfServerId")
  public void compute_engine_does_not_fail_if_server_id_is_valid(ServerId serverId) {
    insertServerId(serverId);
    insertChecksum(CHECKSUM_1);
    mockChecksumOf(serverId, CHECKSUM_1);

    test(COMPUTE_ENGINE);

    // no changes
    verifyDb(serverId, CHECKSUM_1);
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
  @UseDataProvider("allFormatsOfServerId")
  public void compute_engine_fails_if_server_id_is_invalid(ServerId serverId) {
    String dbChecksum = "boom";
    insertServerId(serverId);
    insertChecksum(dbChecksum);
    mockChecksumOf(serverId, CHECKSUM_1);

    try {
      test(SERVER);
      fail("An ISE should have been raised");
    }
    catch (IllegalStateException e) {
      assertThat(e.getMessage()).isEqualTo("Server ID is invalid");
      // no changes
      verifyDb(serverId, dbChecksum);
    }
  }

  @DataProvider
  public static Object[][] allFormatsOfServerId() {
    return new Object[][] {
        {OLD_FORMAT_SERVER_ID},
        {NO_DATABASE_ID_SERVER_ID},
        {WITH_DATABASE_ID_SERVER_ID}
    };
  }

  private void expectEmptyServerIdException() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Property sonar.core.id is empty in database");
  }

  private void expectMissingServerIdException() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Property sonar.core.id is missing in database");
  }

  private void verifyDb(ServerId expectedServerId, String expectedChecksum) {
    assertThat(dbClient.propertiesDao().selectGlobalProperty(dbSession, CoreProperties.SERVER_ID))
      .extracting(PropertyDto::getValue)
      .isEqualTo(expectedServerId.toString());
    assertThat(dbClient.internalPropertiesDao().selectByKey(dbSession, InternalProperties.SERVER_ID_CHECKSUM))
      .hasValue(expectedChecksum);
  }

  private void mockCreateNewServerId(ServerId newServerId) {
    when(serverIdFactory.create()).thenReturn(newServerId);
    when(serverIdFactory.create(any())).thenThrow(new IllegalStateException("new ServerId should not be created from current server id"));
  }

  private void mockCreateNewServerIdFrom(ServerId currentServerId, ServerId newServerId) {
    when(serverIdFactory.create()).thenThrow(new IllegalStateException("new ServerId should be created from current server id"));
    when(serverIdFactory.create(eq(currentServerId))).thenReturn(newServerId);
  }

  private void verifyCreateNewServerIdFromScratch() {
    verify(serverIdFactory).create();
  }

  private void verifyCreateNewServerIdFrom(ServerId currentServerId) {
    verify(serverIdFactory).create(currentServerId);
  }

  private void mockChecksumOf(ServerId serverId, String checksum1) {
    when(serverIdChecksum.computeFor(serverId.toString())).thenReturn(checksum1);
  }

  private void insertServerId(ServerId serverId) {
    insertServerId(serverId.toString());
  }

  private void insertServerId(String serverId) {
    dbClient.propertiesDao().saveProperty(dbSession, new PropertyDto().setKey(CoreProperties.SERVER_ID).setValue(serverId.toString()));
    dbSession.commit();
  }

  private void insertChecksum(String value) {
    dbClient.internalPropertiesDao().save(dbSession, InternalProperties.SERVER_ID_CHECKSUM, value);
    dbSession.commit();
  }

  private void test(SonarQubeSide side) {
    underTest = new ServerIdManager(serverIdChecksum, serverIdFactory, dbClient, SonarRuntimeImpl
      .forSonarQube(Version.create(6, 7), side, SonarEdition.COMMUNITY), webServer);
    underTest.start();
  }
}
