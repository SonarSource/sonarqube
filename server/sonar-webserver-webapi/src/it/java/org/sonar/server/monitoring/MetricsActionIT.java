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
package org.sonar.server.monitoring;

import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbTester;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.BearerPasscode;
import org.sonar.server.user.SystemPasscode;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.tester.UserSessionRule.standalone;

public class MetricsActionIT {

  @Rule
  public UserSessionRule userSession = standalone();

  @Rule
  public DbTester db = DbTester.create();

  private final BearerPasscode bearerPasscode = mock(BearerPasscode.class);
  private final SystemPasscode systemPasscode = mock(SystemPasscode.class);

  private final MetricsAction underTest = new MetricsAction(systemPasscode, bearerPasscode, userSession);
  private final WsActionTester ws = new WsActionTester(underTest);

  @Test
  public void test_definition() {
    WebService.Action definition = ws.getDef();
    assertThat(definition.isInternal()).isFalse();
    assertThat(definition.isPost()).isFalse();
    assertThat(definition.responseExampleAsString()).isNotEmpty();
    assertThat(definition.params()).isEmpty();
  }

  @Test
  public void no_authentication_throw_insufficient_privileges_error() {
    TestRequest request = ws.newRequest();
    Assertions.assertThatThrownBy(request::execute)
      .hasMessage("Insufficient privileges")
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void authenticated_non_global_admin_is_forbidden() {
    userSession.logIn();

    TestRequest testRequest = ws.newRequest();
    Assertions.assertThatThrownBy(testRequest::execute)
      .hasMessage("Insufficient privileges")
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void authentication_passcode_is_allowed() {
    when(systemPasscode.isValid(any())).thenReturn(true);

    TestResponse response = ws.newRequest().execute();
    String content = response.getInput();
    assertThat(content)
      .contains("# HELP sonarqube_health_web_status Tells whether Web process is up or down. 1 for up, 0 for down")
      .contains("# TYPE sonarqube_health_web_status gauge")
      .contains("sonarqube_health_web_status 1.0");
  }

  @Test
  public void authentication_bearer_passcode_is_allowed() {
    when(bearerPasscode.isValid(any())).thenReturn(true);

    TestResponse response = ws.newRequest().execute();
    String content = response.getInput();
    assertThat(content)
      .contains("# HELP sonarqube_health_web_status Tells whether Web process is up or down. 1 for up, 0 for down")
      .contains("# TYPE sonarqube_health_web_status gauge")
      .contains("sonarqube_health_web_status 1.0");
  }

  @Test
  public void authenticated_global_admin_is_allowed() {
    userSession.logIn().setSystemAdministrator();

    TestResponse response = ws.newRequest().execute();
    String content = response.getInput();
    assertThat(content)
      .contains("# HELP sonarqube_health_web_status Tells whether Web process is up or down. 1 for up, 0 for down")
      .contains("# TYPE sonarqube_health_web_status gauge")
      .contains("sonarqube_health_web_status 1.0");
  }

}
