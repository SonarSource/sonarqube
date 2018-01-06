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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.edition.EditionManagementState;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonar.test.JsonAssert;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.edition.EditionManagementState.PendingStatus.NONE;

public class StatusActionTest {
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private EditionManagementState editionManagementState = mock(EditionManagementState.class);
  private StatusAction underTest = new StatusAction(userSessionRule, editionManagementState);
  private WsActionTester actionTester = new WsActionTester(underTest);

  @Test
  public void verify_definition() {
    WebService.Action def = actionTester.getDef();
    assertThat(def.key()).isEqualTo("status");
    assertThat(def.since()).isEqualTo("6.7");
    assertThat(def.isPost()).isFalse();
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
    when(editionManagementState.getCurrentEditionKey()).thenReturn(empty());
    when(editionManagementState.getPendingEditionKey()).thenReturn(of("developer-edition"));
    when(editionManagementState.getPendingInstallationStatus()).thenReturn(EditionManagementState.PendingStatus.AUTOMATIC_READY);
    when(editionManagementState.getInstallErrorMessage()).thenReturn(empty());

    TestRequest request = actionTester.newRequest();

    JsonAssert.assertJson(request.execute().getInput()).isSimilarTo(actionTester.getDef().responseExampleAsString());
  }

  @Test
  public void response_contains_optional_fields_as_empty_string() {
    userSessionRule.logIn().setSystemAdministrator();
    when(editionManagementState.getCurrentEditionKey()).thenReturn(empty());
    when(editionManagementState.getPendingEditionKey()).thenReturn(empty());
    when(editionManagementState.getPendingInstallationStatus()).thenReturn(NONE);
    when(editionManagementState.getInstallErrorMessage()).thenReturn(empty());

    TestRequest request = actionTester.newRequest();

    JsonAssert.assertJson(request.execute().getInput())
      .isSimilarTo("{" +
        "  \"currentEditionKey\": \"\"," +
        "  \"installationStatus\": \"NONE\"," +
        "  \"nextEditionKey\": \"\"" +
        "}");
  }

  @Test
  public void response_contains_automaticInstallError_when_present() {
    userSessionRule.logIn().setSystemAdministrator();
    when(editionManagementState.getCurrentEditionKey()).thenReturn(empty());
    when(editionManagementState.getPendingEditionKey()).thenReturn(empty());
    when(editionManagementState.getPendingInstallationStatus()).thenReturn(NONE);
    String errorMessage = "an error! oh god, an error!";
    when(editionManagementState.getInstallErrorMessage()).thenReturn(of(errorMessage));
    TestRequest request = actionTester.newRequest();

    JsonAssert.assertJson(request.execute().getInput())
      .isSimilarTo("{" +
        "  \"currentEditionKey\": \"\"," +
        "  \"installationStatus\": \"NONE\"," +
        "  \"nextEditionKey\": \"\"," +
        "  \"installError\": \"" + errorMessage + "\"" +
        "}");
  }
}
