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
package org.sonar.server.edition.ws;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import java.io.IOException;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.api.platform.Server;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.edition.EditionManagementState;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.measure.index.ProjectMeasuresIndex;
import org.sonar.server.measure.index.ProjectMeasuresStatistics;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonar.test.JsonAssert;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.Editions.FormDataResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.edition.EditionManagementState.PendingStatus.NONE;

@RunWith(DataProviderRunner.class)
public class FormDataActionTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private Server server = mock(Server.class);
  private ProjectMeasuresStatistics stats = mock(ProjectMeasuresStatistics.class);
  private ProjectMeasuresIndex measuresIndex = mock(ProjectMeasuresIndex.class);
  private FormDataAction underTest = new FormDataAction(userSessionRule, server, measuresIndex);
  private WsActionTester actionTester = new WsActionTester(underTest);

  @Before
  public void setUp() {
    when(measuresIndex.searchTelemetryStatistics()).thenReturn(stats);
  }

  @Test
  public void verify_definition() {
    WebService.Action def = actionTester.getDef();

    assertThat(def.key()).isEqualTo("form_data");
    assertThat(def.since()).isEqualTo("6.7");
    assertThat(def.isPost()).isFalse();
    assertThat(def.isInternal()).isTrue();
    assertThat(def.description()).isNotEmpty();
    assertThat(def.params()).isEmpty();
  }

  @Test
  public void request_fails_if_user_not_logged_in() {
    userSessionRule.anonymous();
    TestRequest request = actionTester.newRequest();

    expectedException.expect(UnauthorizedException.class);
    expectedException.expectMessage("Authentication is required");

    request.execute();
  }

  @Test
  public void request_fails_if_user_is_not_system_administer() {
    userSessionRule.logIn();
    TestRequest request = actionTester.newRequest();

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    request.execute();
  }

  @Test
  public void verify_example() {
    userSessionRule.logIn().setSystemAdministrator();
    when(server.getId()).thenReturn("uuid");
    when(stats.getNcloc()).thenReturn(12345L);

    TestRequest request = actionTester.newRequest();

    JsonAssert.assertJson(request.execute().getInput()).isSimilarTo(actionTester.getDef().responseExampleAsString());
  }

  @Test
  public void returns_server_id_and_nloc() throws IOException {
    userSessionRule.logIn().setSystemAdministrator();
    when(server.getId()).thenReturn("myserver");
    when(stats.getNcloc()).thenReturn(1000L);

    FormDataResponse expectedResponse = FormDataResponse.newBuilder()
      .setServerId("myserver")
      .setNcloc(1000L)
      .build();

    TestRequest request = actionTester.newRequest().setMediaType(MediaTypes.PROTOBUF);

    assertThat(FormDataResponse.parseFrom(request.execute().getInputStream())).isEqualTo(expectedResponse);
  }

  @DataProvider
  public static Object[][] notNonePendingInstallationStatuses() {
    return Arrays.stream(EditionManagementState.PendingStatus.values())
      .filter(s -> s != NONE)
      .map(s -> new Object[] {s})
      .toArray(Object[][]::new);
  }
}
