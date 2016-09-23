/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

package org.sonar.server.serverid.ws;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.property.PropertyDbTester;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.platform.ServerIdGenerator;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonar.test.JsonAssert;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.ServerId.ShowWsResponse;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.db.property.PropertyTesting.newGlobalPropertyDto;
import static org.sonarqube.ws.MediaTypes.JSON;

public class ShowActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  DbClient dbClient = db.getDbClient();
  PropertyDbTester propertyDb = new PropertyDbTester(db);
  ServerIdGenerator generator = mock(ServerIdGenerator.class);

  WsActionTester ws = new WsActionTester(new ShowAction(userSession, generator, dbClient));

  @Test
  public void return_server_id_info() throws Exception {
    setUserAsSystemAdmin();
    when(generator.validate("home", "127.0.0.1", "1818a1eefb26f9g")).thenReturn(true);
    setAvailableIpAdresses("192.168.1.1", "127.0.0.1");
    insertConfiguration("1818a1eefb26f9g", "home", "127.0.0.1");

    ShowWsResponse response = executeRequest();

    assertThat(response.getServerId()).isEqualTo("1818a1eefb26f9g");
    assertThat(response.getOrganization()).isEqualTo("home");
    assertThat(response.getIp()).isEqualTo("127.0.0.1");
    assertThat(response.getValidIpAddressesList()).containsOnly("192.168.1.1", "127.0.0.1");
    assertThat(response.hasInvalidServerId()).isFalse();
  }

  @Test
  public void return_invalid_server_id() throws Exception {
    setUserAsSystemAdmin();
    when(generator.validate("home", "127.0.0.1", "1818a1eefb26f9g")).thenReturn(true);
    insertConfiguration("invalid", null, null);

    ShowWsResponse response = executeRequest();

    assertThat(response.hasInvalidServerId()).isTrue();
    assertThat(response.getServerId()).isEqualTo("invalid");
    assertThat(response.hasOrganization()).isFalse();
    assertThat(response.hasIp()).isFalse();
    assertThat(response.getValidIpAddressesList()).isEmpty();
  }

  @Test
  public void return_no_server_id_info_when_no_settings_and_no_available_ips() throws Exception {
    setUserAsSystemAdmin();

    ShowWsResponse response = executeRequest();

    assertThat(response.hasServerId()).isFalse();
    assertThat(response.hasOrganization()).isFalse();
    assertThat(response.hasIp()).isFalse();
    assertThat(response.getValidIpAddressesList()).isEmpty();
    assertThat(response.hasInvalidServerId()).isFalse();
  }

  @Test
  public void return_no_server_id_info_when_no_server_id_but_other_settings() throws Exception {
    setUserAsSystemAdmin();
    insertConfiguration(null, "home", "127.0.0.1");

    ShowWsResponse response = executeRequest();

    assertThat(response.hasServerId()).isFalse();
    assertThat(response.hasOrganization()).isFalse();
    assertThat(response.hasIp()).isFalse();
    assertThat(response.getValidIpAddressesList()).isEmpty();
    assertThat(response.hasInvalidServerId()).isFalse();
  }

  @Test
  public void return_available_ips_even_if_no_settings() throws Exception {
    setUserAsSystemAdmin();
    setAvailableIpAdresses("192.168.1.1", "127.0.0.1");

    ShowWsResponse response = executeRequest();

    assertThat(response.hasServerId()).isFalse();
    assertThat(response.hasOrganization()).isFalse();
    assertThat(response.hasIp()).isFalse();
    assertThat(response.getValidIpAddressesList()).containsOnly("192.168.1.1", "127.0.0.1");
    assertThat(response.hasInvalidServerId()).isFalse();
  }

  @Test
  public void fail_when_not_system_admin() throws Exception {
    userSession.login("not-admin").setGlobalPermissions(GlobalPermissions.QUALITY_GATE_ADMIN);

    expectedException.expect(ForbiddenException.class);

    executeRequest();
  }

  @Test
  public void test_ws_definition() {
    WebService.Action action = ws.getDef();
    assertThat(action).isNotNull();
    assertThat(action.isInternal()).isTrue();
    assertThat(action.isPost()).isFalse();
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.params()).isEmpty();
  }

  @Test
  public void test_example_json_response() {
    setUserAsSystemAdmin();
    when(generator.validate("home", "127.0.0.1", "1818a1eefb26f9g")).thenReturn(true);
    setAvailableIpAdresses("192.168.1.1", "127.0.0.1");
    insertConfiguration("1818a1eefb26f9g", "home", "127.0.0.1");

    String result = ws.newRequest()
      .setMediaType(JSON)
      .execute()
      .getInput();

    JsonAssert.assertJson(ws.getDef().responseExampleAsString()).isSimilarTo(result);
  }

  private void insertConfiguration(@Nullable String serverId, @Nullable String organisation, @Nullable String ip) {
    if (serverId != null) {
      propertyDb.insertProperties(newGlobalPropertyDto().setKey("sonar.server_id").setValue(serverId));
    }
    if (organisation != null) {
      propertyDb.insertProperties(newGlobalPropertyDto().setKey("sonar.organisation").setValue(organisation));
    }
    if (ip != null) {
      propertyDb.insertProperties(newGlobalPropertyDto().setKey("sonar.server_id.ip_address").setValue(ip));
    }
  }

  private void setAvailableIpAdresses(String... ips) {
    List<InetAddress> availableAddresses = new ArrayList<>();
    for (String ip : ips) {
      InetAddress inetAddress = mock(InetAddress.class);
      when(inetAddress.getHostAddress()).thenReturn(ip);
      availableAddresses.add(inetAddress);
    }
    when(generator.getAvailableAddresses()).thenReturn(availableAddresses);
  }

  private ShowWsResponse executeRequest() {
    TestRequest request = ws.newRequest()
      .setMediaType(MediaTypes.PROTOBUF);
    try {
      return ShowWsResponse.parseFrom(request.execute().getInputStream());
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private void setUserAsSystemAdmin() {
    userSession.login("admin").setGlobalPermissions(SYSTEM_ADMIN);
  }
}
