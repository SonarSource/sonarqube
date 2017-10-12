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
package org.sonar.server.edition.ws;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Arrays;
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.edition.EditionManagementState;
import org.sonar.server.edition.MutableEditionManagementState;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonar.test.JsonAssert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.edition.EditionManagementState.PendingStatus.AUTOMATIC_IN_PROGRESS;
import static org.sonar.server.edition.EditionManagementState.PendingStatus.NONE;

@RunWith(DataProviderRunner.class)
public class ApplyLicenseActionTest {
  private static final String PARAM_LICENSE = "license";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private MutableEditionManagementState mutableEditionManagementState = mock(MutableEditionManagementState.class);
  private ApplyLicenseAction underTest = new ApplyLicenseAction(userSessionRule, mutableEditionManagementState);
  private WsActionTester actionTester = new WsActionTester(underTest);

  @Test
  public void verify_definition() {
    WebService.Action def = actionTester.getDef();

    assertThat(def.key()).isEqualTo("apply_license");
    assertThat(def.since()).isEqualTo("6.7");
    assertThat(def.isPost()).isTrue();
    assertThat(def.isInternal()).isFalse();
    assertThat(def.description()).isNotEmpty();
    assertThat(def.params()).hasSize(1);

    WebService.Param licenseParam = def.param("license");
    assertThat(licenseParam.isRequired()).isTrue();
    assertThat(licenseParam.description()).isNotEmpty();
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
  public void request_fails_if_license_param_is_not_provided() {
    userSessionRule.logIn().setSystemAdministrator();
    when(mutableEditionManagementState.getPendingInstallationStatus()).thenReturn(NONE);
    TestRequest request = actionTester.newRequest();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'license' parameter is missing");

    request.execute();
  }

  @Test
  @UseDataProvider("notNonePendingInstallationStatuses")
  public void request_fails_with_BadRequestException_is_pendingStatus_is_not_NONE(EditionManagementState.PendingStatus notNone) {
    userSessionRule.logIn().setSystemAdministrator();
    when(mutableEditionManagementState.getCurrentEditionKey()).thenReturn(Optional.empty());
    when(mutableEditionManagementState.getPendingEditionKey()).thenReturn(Optional.empty());
    when(mutableEditionManagementState.getPendingInstallationStatus()).thenReturn(notNone);
    TestRequest request = actionTester.newRequest()
      .setParam(PARAM_LICENSE, "foo");

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Can't apply a license when applying one is already in progress");

    request.execute();
  }

  @Test
  public void verify_example() {
    userSessionRule.logIn().setSystemAdministrator();
    when(mutableEditionManagementState.getCurrentEditionKey()).thenReturn(Optional.empty());
    when(mutableEditionManagementState.getPendingEditionKey()).thenReturn(Optional.of("developer-edition"));
    when(mutableEditionManagementState.getPendingInstallationStatus())
        .thenReturn(NONE)
        .thenReturn(AUTOMATIC_IN_PROGRESS);

    TestRequest request = actionTester.newRequest()
      .setParam(PARAM_LICENSE, "foo");

    JsonAssert.assertJson(request.execute().getInput()).isSimilarTo(actionTester.getDef().responseExampleAsString());
  }

  @DataProvider
  public static Object[][] notNonePendingInstallationStatuses() {
    return Arrays.stream(EditionManagementState.PendingStatus.values())
        .filter(s -> s != NONE)
        .map(s -> new Object[] {s})
        .toArray(Object[][]::new);
  }
}
