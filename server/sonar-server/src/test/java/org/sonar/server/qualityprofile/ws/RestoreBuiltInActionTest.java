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
package org.sonar.server.qualityprofile.ws;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.qualityprofile.QProfileReset;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;

public class RestoreBuiltInActionTest {
  @Rule
  public DbTester db = DbTester.create();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private QProfileReset reset = mock(QProfileReset.class);
  private Languages languages = LanguageTesting.newLanguages("xoo");
  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
  private QProfileWsSupport wsSupport = new QProfileWsSupport(db.getDbClient(), userSession, defaultOrganizationProvider);
  private RestoreBuiltInAction underTest = new RestoreBuiltInAction(db.getDbClient(), reset, languages, userSession, wsSupport);
  private WsActionTester tester = new WsActionTester(underTest);

  @Test
  public void test_definition() {
    WebService.Action action = tester.getDef();

    assertThat(action.key()).isEqualTo("restore_built_in");
    assertThat(action.isPost()).isTrue();
    assertThat(action.responseExampleAsString()).isNull();

    // parameters
    assertThat(action.params()).hasSize(2);
    WebService.Param languageParam = action.param("language");
    assertThat(languageParam.isRequired()).isTrue();
    assertThat(languageParam.since()).isNull();//introduced at the same time than the web service

    WebService.Param organizationParam = action.param("organization");
    assertThat(organizationParam.isRequired()).isFalse();
    assertThat(organizationParam.since()).isEqualTo("6.4");
  }

  @Test
  public void restore_built_in_profiles_on_default_organization() {
    OrganizationDto organization = db.getDefaultOrganization();
    logInAsQProfileAdministrator(organization);
    TestResponse response = tester.newRequest().setParam("language", "xoo").execute();

    ArgumentCaptor<OrganizationDto> organizationArgument = ArgumentCaptor.forClass(OrganizationDto.class);
    verify(reset).resetLanguage(any(DbSession.class), organizationArgument.capture(), eq("xoo"));
    assertThat(organizationArgument.getValue().getUuid()).isEqualTo(organization.getUuid());
    assertThat(response.getStatus()).isEqualTo(204);
  }

  @Test
  public void restore_built_in_profiles_on_specified_organization() {
    OrganizationDto organization = db.organizations().insert();
    logInAsQProfileAdministrator(organization);
    TestResponse response = tester.newRequest()
      .setParam("language", "xoo")
      .setParam("organization", organization.getKey())
      .execute();

    ArgumentCaptor<OrganizationDto> organizationArgument = ArgumentCaptor.forClass(OrganizationDto.class);
    verify(reset).resetLanguage(any(DbSession.class), organizationArgument.capture(), eq("xoo"));
    assertThat(organizationArgument.getValue().getUuid()).isEqualTo(organization.getUuid());
    assertThat(response.getStatus()).isEqualTo(204);
  }

  @Test
  public void throw_IAE_if_language_does_not_exist() throws Exception {
    logInAsQProfileAdministrator(db.getDefaultOrganization());

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Value of parameter 'language' (unknown) must be one of: [xoo]");

    tester.newRequest().setParam("language", "unknown").execute();
  }

  @Test
  public void throw_NotFoundException_if_organization_does_not_exist() throws Exception {
    userSession.logIn();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("No organization with key 'does_not_exist'");

    tester.newRequest()
      .setParam("language", "unknown")
      .setParam("organization", "does_not_exist")
      .execute();
  }

  @Test
  public void throw_ForbiddenException_if_not_profile_administrator_of_default_organization() throws Exception {
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    tester.newRequest().setParam("language", "xoo").execute();
  }

  @Test
  public void throw_ForbiddenException_if_not_profile_administrator_of_specified_organization() throws Exception {
    OrganizationDto org = db.organizations().insert();
    userSession.logIn().addPermission(ADMINISTER_QUALITY_PROFILES, db.getDefaultOrganization());

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    tester.newRequest()
      .setParam("language", "xoo")
      .setParam("organization", org.getKey())
      .execute();
  }

  @Test
  public void throw_UnauthorizedException_if_not_logged_in() throws Exception {
    userSession.anonymous();

    expectedException.expect(UnauthorizedException.class);
    expectedException.expectMessage("Authentication is required");

    tester.newRequest().setParam("language", "xoo").execute();
  }

  private void logInAsQProfileAdministrator(OrganizationDto organization) {
    userSession
      .logIn()
      .addPermission(ADMINISTER_QUALITY_PROFILES, organization.getUuid());
  }
}
