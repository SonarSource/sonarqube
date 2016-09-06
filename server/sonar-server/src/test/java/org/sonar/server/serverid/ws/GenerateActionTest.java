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

import com.google.common.base.Throwables;
import java.io.IOException;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.property.PropertyDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.platform.ServerIdGenerator;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.ServerId.GenerateWsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.CoreProperties.ORGANISATION;
import static org.sonar.api.CoreProperties.PERMANENT_SERVER_ID;
import static org.sonar.api.CoreProperties.SERVER_ID_IP_ADDRESS;
import static org.sonar.server.serverid.ws.GenerateAction.PARAM_IP;
import static org.sonar.server.serverid.ws.GenerateAction.PARAM_ORGANIZATION;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.MediaTypes.PROTOBUF;

public class GenerateActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone().setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
  @Rule
  public LogTester log = new LogTester();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  DbClient dbClient = db.getDbClient();

  ServerIdGenerator generator = mock(ServerIdGenerator.class);

  GenerateAction underTest = new GenerateAction(userSession, generator, dbClient);

  WsActionTester ws = new WsActionTester(underTest);

  @Test
  public void persist_settings() {
    when(generator.generate("SonarSource", "10.51.42.255")).thenReturn("server_id");

    GenerateWsResponse result = call("SonarSource", "10.51.42.255");

    assertThat(result.getServerId()).isEqualTo("server_id");
    assertGlobalSetting(ORGANISATION, "SonarSource");
    assertGlobalSetting(SERVER_ID_IP_ADDRESS, "10.51.42.255");
    assertGlobalSetting(PERMANENT_SERVER_ID, "server_id");
  }

  @Test
  public void json_example() {
    when(generator.generate("SonarSource", "127.0.0.1")).thenReturn("1818a1eefb26f9g");

    String result = ws.newRequest()
      .setParam(PARAM_ORGANIZATION, "SonarSource")
      .setParam(PARAM_IP, "127.0.0.1")
      .execute().getInput();

    assertJson(result).isSimilarTo(ws.getDef().responseExampleAsString());
  }

  @Test
  public void log_message_when_id_generated() {
    when(generator.generate("SonarSource", "127.0.0.1")).thenReturn("server_id");

    call("SonarSource", "127.0.0.1");

    assertThat(log.logs(LoggerLevel.INFO)).contains("Generated new server ID=" + "server_id");
  }

  @Test
  public void definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.key()).isEqualTo("generate");
    assertThat(definition.since()).isEqualTo("6.1");
    assertThat(definition.isInternal()).isTrue();
    assertThat(definition.isPost()).isTrue();
    assertThat(definition.responseExampleAsString()).isNotEmpty();
    assertThat(definition.params()).hasSize(2);
  }

  @Test
  public void fail_if_insufficient_permission() {
    userSession.setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    expectedException.expect(ForbiddenException.class);

    call("SonarSource", "127.0.0.1");
  }

  @Test
  public void fail_if_no_organization() {
    expectedException.expect(IllegalArgumentException.class);

    call(null, "127.0.0.1");
  }

  @Test
  public void fail_if_empty_organization() {
    expectedException.expect(IllegalArgumentException.class);

    call("", "127.0.0.1");
  }

  @Test
  public void fail_if_no_ip() {
    expectedException.expect(IllegalArgumentException.class);

    call("SonarSource", null);
  }

  @Test
  public void fail_if_empty_ip() {
    expectedException.expect(IllegalArgumentException.class);

    call("SonarSource", "");
  }

  private void assertGlobalSetting(String key, String value) {
    PropertyDto result = dbClient.propertiesDao().selectGlobalProperty(key);

    assertThat(result)
      .extracting(PropertyDto::getKey, PropertyDto::getValue, PropertyDto::getResourceId)
      .containsExactly(key, value, null);
  }

  private GenerateWsResponse call(@Nullable String organization, @Nullable String ip) {
    TestRequest request = ws.newRequest()
      .setMethod("POST")
      .setMediaType(PROTOBUF);

    if (organization != null) {
      request.setParam(PARAM_ORGANIZATION, organization);
    }

    if (ip != null) {
      request.setParam(PARAM_IP, ip);
    }

    try {
      return GenerateWsResponse.parseFrom(request.execute().getInputStream());
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }
}
