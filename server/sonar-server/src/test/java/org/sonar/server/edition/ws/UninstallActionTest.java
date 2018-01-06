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
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Arrays;
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
import org.sonar.server.plugins.edition.EditionInstaller;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.server.edition.EditionManagementState.PendingStatus.NONE;
import static org.sonar.server.edition.EditionManagementState.PendingStatus.UNINSTALL_IN_PROGRESS;

@RunWith(DataProviderRunner.class)
public class UninstallActionTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private EditionInstaller editionInstaller = mock(EditionInstaller.class);
  private MutableEditionManagementState mutableEditionManagementState = mock(MutableEditionManagementState.class);
  private UninstallAction action = new UninstallAction(userSessionRule, mutableEditionManagementState, editionInstaller);
  private WsActionTester actionTester = new WsActionTester(action);

  @Test
  public void check_definition() {
    WebService.Action def = actionTester.getDef();

    assertThat(def.key()).isEqualTo("uninstall");
    assertThat(def.since()).isEqualTo("6.7");
    assertThat(def.isPost()).isTrue();
    assertThat(def.isInternal()).isFalse();
    assertThat(def.responseExampleAsString()).isNull();
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
  @UseDataProvider("notNoneOrUninstallStatuses")
  public void request_fails_if_current_status_is_not_none_nor_uninstall(EditionManagementState.PendingStatus notNoneOrUninstall) {
    userSessionRule.logIn().setSystemAdministrator();
    when(mutableEditionManagementState.getPendingInstallationStatus()).thenReturn(notNoneOrUninstall);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Uninstall of the current edition is not allowed when install of an edition");
    actionTester.newRequest().execute();
  }

  @Test
  public void successful_edition_uninstall() {
    userSessionRule.logIn().setSystemAdministrator();
    when(mutableEditionManagementState.getPendingInstallationStatus()).thenReturn(NONE);

    TestResponse execute = actionTester.newRequest().execute();
    assertThat(execute.getStatus()).isEqualTo(204);
    verify(editionInstaller).uninstall();
    verify(mutableEditionManagementState).uninstall();
  }

  @Test
  public void successful_edition_uninstall_when_state_is_already_uninstall_in_progress() {
    userSessionRule.logIn().setSystemAdministrator();
    when(mutableEditionManagementState.getPendingInstallationStatus()).thenReturn(UNINSTALL_IN_PROGRESS);

    TestResponse execute = actionTester.newRequest().execute();
    assertThat(execute.getStatus()).isEqualTo(204);
    verify(editionInstaller).uninstall();
    verify(mutableEditionManagementState).uninstall();
  }

  @DataProvider
  public static Object[][] notNoneOrUninstallStatuses() {
    return Arrays.stream(EditionManagementState.PendingStatus.values())
      .filter(s -> s != NONE)
      .filter(s -> s != UNINSTALL_IN_PROGRESS)
      .map(s -> new Object[] {s})
      .toArray(Object[][]::new);
  }
}
