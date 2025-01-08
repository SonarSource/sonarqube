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
package org.sonar.server.almintegration.ws.github.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.alm.client.github.config.ConfigCheckResult;
import org.sonar.alm.client.github.config.ConfigCheckResult.ApplicationStatus;
import org.sonar.alm.client.github.config.GithubProvisioningConfigValidator;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.alm.client.github.config.ConfigCheckResult.ConfigStatus;
import static org.sonar.alm.client.github.config.ConfigCheckResult.InstallationStatus;
import static org.sonar.test.JsonAssert.assertJson;

public class CheckActionIT {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private GithubProvisioningConfigValidator configValidator = mock(GithubProvisioningConfigValidator.class);

  private final WsActionTester ws = new WsActionTester(new CheckAction(userSession, configValidator));

  @Test
  public void test_definition() {
    WebService.Action def = ws.getDef();
    assertThat(def.key()).isEqualTo("check");
    assertThat(def.since()).isEqualTo("10.1");
    assertThat(def.isInternal()).isTrue();
    assertThat(def.isPost()).isTrue();
    assertThat(def.params()).isEmpty();
    assertThat(def.responseExample()).isNotNull();
    assertThat(def.responseExample()).isEqualTo(getClass().getResource("check-example.json"));
  }

  @Test
  public void check_whenUserIsAdmin_shouldReturnCheckResult() throws IOException {
    userSession.logIn().setSystemAdministrator();
    ConfigCheckResult result = new ConfigCheckResult(
      new ApplicationStatus(
        ConfigStatus.SUCCESS,
        ConfigStatus.failed("App validation failed")),
      List.of(
        new InstallationStatus(
          "org1",
          ConfigStatus.SUCCESS,
          ConfigStatus.SUCCESS
        ),
        new InstallationStatus(
          "org2",
          ConfigStatus.SUCCESS,
          ConfigStatus.failed("Organization validation failed.")
        )
      ));

    when(configValidator.checkConfig()).thenReturn(result);
    TestResponse response = ws.newRequest().execute();

    assertThat(response.getStatus()).isEqualTo(200);
    assertJson(response.getInput()).isSimilarTo(readResponse("check-example.json"));
  }

  @Test
  public void check_whenNotAnAdmin_shouldThrow() {
    userSession.logIn("not-an-admin");

    TestRequest testRequest = ws.newRequest();
    assertThatThrownBy(testRequest::execute)
      .hasMessage("Insufficient privileges")
      .isInstanceOf(ForbiddenException.class);
  }

  private String readResponse(String file) throws IOException {
    return IOUtils.toString(getClass().getResource(file), StandardCharsets.UTF_8);
  }
}
