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
package org.sonar.server.organization.ws;

import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.organization.OrganizationValidationImpl;
import org.sonar.server.organization.TestOrganizationFlags;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Organizations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
import static org.sonar.server.organization.ws.OrganizationsWsTestSupport.STRING_257_CHARS_LONG;
import static org.sonar.server.organization.ws.OrganizationsWsTestSupport.STRING_65_CHARS_LONG;
import static org.sonar.server.organization.ws.OrganizationsWsTestSupport.setParam;

public class UpdateActionTest {
  private static final String SOME_KEY = "key";
  private static final long DATE_1 = 1_200_000L;
  private static final long DATE_2 = 5_600_000L;

  private System2 system2 = mock(System2.class);

  @Rule
  public DbTester dbTester = DbTester.create(system2);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private TestOrganizationFlags organizationFlags = TestOrganizationFlags.standalone().setEnabled(true);
  private UpdateAction underTest = new UpdateAction(userSession, new OrganizationsWsSupport(new OrganizationValidationImpl()), dbTester.getDbClient(), organizationFlags);
  private WsActionTester wsTester = new WsActionTester(underTest);

  @Test
  public void verify_define() {
    WebService.Action action = wsTester.getDef();
    assertThat(action.key()).isEqualTo("update");
    assertThat(action.isPost()).isTrue();
    assertThat(action.description()).isEqualTo("Update an organization.<br/>" +
      "Require 'Administer System' permission. Organization support must be enabled.");
    assertThat(action.isInternal()).isTrue();
    assertThat(action.since()).isEqualTo("6.2");
    assertThat(action.handler()).isEqualTo(underTest);
    assertThat(action.params()).hasSize(5);
    assertThat(action.responseExample()).isNull();

    assertThat(action.param("key"))
      .matches(param -> param.isRequired())
      .matches(param -> "foo-company".equals(param.exampleValue()))
      .matches(param -> "Organization key".equals(param.description()));
    assertThat(action.param("name"))
      .matches(param -> !param.isRequired())
      .matches(param -> "Foo Company".equals(param.exampleValue()))
      .matches(param -> param.description() != null);
    assertThat(action.param("description"))
      .matches(param -> !param.isRequired())
      .matches(param -> "The Foo company produces quality software for Bar.".equals(param.exampleValue()))
      .matches(param -> param.description() != null);
    assertThat(action.param("url"))
      .matches(param -> !param.isRequired())
      .matches(param -> "https://www.foo.com".equals(param.exampleValue()))
      .matches(param -> param.description() != null);
    assertThat(action.param("avatar"))
      .matches(param -> !param.isRequired())
      .matches(param -> "https://www.foo.com/foo.png".equals(param.exampleValue()))
      .matches(param -> param.description() != null);
  }

  @Test
  public void request_fails_with_IllegalStateException_if_organization_support_is_disabled() {
    organizationFlags.setEnabled(false);
    userSession.logIn();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Organization support is disabled");

    wsTester.newRequest().execute();
  }

  @Test
  public void request_succeeds_if_user_is_organization_administrator() {
    OrganizationDto dto = mockForSuccessfulUpdate(DATE_1, DATE_2);
    logInAsAdministrator(dto);

    verifyResponseAndDb(executeKeyRequest(dto.getKey(), "ab"), dto, "ab", DATE_2);
  }

  @Test
  public void request_succeeds_if_user_is_administrator_of_specified_organization() {
    OrganizationDto dto = mockForSuccessfulUpdate(DATE_1, DATE_2);
    logInAsAdministrator(dto);

    verifyResponseAndDb(executeKeyRequest(dto.getKey(), "ab"), dto, "ab", DATE_2);
  }

  @Test
  public void request_fails_with_UnauthorizedException_when_user_is_not_logged_in() {
    expectedException.expect(UnauthorizedException.class);
    expectedException.expectMessage("Authentication is required");

    wsTester.newRequest().execute();
  }

  @Test
  public void request_fails_if_user_is_not_system_administrator_and_is_not_organization_administrator() {
    OrganizationDto dto = mockForSuccessfulUpdate(DATE_1, DATE_2);
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    executeKeyRequest(dto.getKey(), "name");
  }

  @Test
  public void request_fails_if_user_is_administrator_of_another_organization() {
    OrganizationDto org = dbTester.organizations().insert();
    logInAsAdministrator(dbTester.getDefaultOrganization());

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    executeKeyRequest(org.getKey(), "name");
  }

  @Test
  public void request_fails_if_key_is_missing() {
    userSession.logIn();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'key' parameter is missing");

    executeRequest(null, "name", "description", "url", "avatar");
  }

  @Test
  public void request_with_only_key_param_succeeds_and_updates_only_updateAt_field() {
    OrganizationDto org = mockForSuccessfulUpdate(DATE_1, DATE_2);
    logInAsAdministrator(org);

    verifyResponseAndDb(executeKeyRequest(org.getKey(), null), org, org.getName(), DATE_2);
  }

  @Test
  public void request_fails_if_name_is_one_char_long() {
    userSession.logIn();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Name 'a' must be at least 2 chars long");

    executeKeyRequest(SOME_KEY, "a");
  }

  @Test
  public void request_succeeds_if_name_is_two_chars_long() {
    OrganizationDto org = mockForSuccessfulUpdate(DATE_1, DATE_2);
    logInAsAdministrator(org);

    verifyResponseAndDb(executeKeyRequest(org.getKey(), "ab"), org, "ab", DATE_2);
  }

  @Test
  public void request_fails_if_name_is_65_chars_long() {
    userSession.logIn();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("'name' length (65) is longer than the maximum authorized (64)");


    executeKeyRequest(SOME_KEY, STRING_65_CHARS_LONG);
  }

  @Test
  public void request_succeeds_if_name_is_64_char_long() {
    OrganizationDto org = mockForSuccessfulUpdate(DATE_1, DATE_2);
    logInAsAdministrator(org);

    String name = STRING_65_CHARS_LONG.substring(0, 64);

    verifyResponseAndDb(executeKeyRequest(org.getKey(), name), org, name, DATE_2);
  }

  @Test
  public void request_succeeds_if_description_url_and_avatar_are_not_specified() {
    OrganizationDto org = mockForSuccessfulUpdate(DATE_1, DATE_2);
    logInAsAdministrator(org);

    Organizations.UpdateWsResponse response = executeKeyRequest(org.getKey(), "bar", null, null, null);
    verifyResponseAndDb(response, org, "bar", DATE_2);
  }

  @Test
  public void request_succeeds_if_description_url_and_avatar_are_specified() {
    OrganizationDto org = mockForSuccessfulUpdate(DATE_1, DATE_2);
    logInAsAdministrator(org);

    Organizations.UpdateWsResponse response = executeKeyRequest(org.getKey(), "bar", "moo", "doo", "boo");
    verifyResponseAndDb(response, org, "bar", "moo", "doo", "boo", DATE_2);
  }

  @Test
  public void request_fails_if_description_is_257_chars_long() {
    userSession.logIn();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("'description' length (257) is longer than the maximum authorized (256)");

    executeKeyRequest(SOME_KEY, "bar", STRING_257_CHARS_LONG, null, null);
  }

  @Test
  public void request_succeeds_if_description_is_256_chars_long() {
    OrganizationDto org = mockForSuccessfulUpdate(DATE_1, DATE_2);
    String description = STRING_257_CHARS_LONG.substring(0, 256);
    logInAsAdministrator(org);

    Organizations.UpdateWsResponse response = executeKeyRequest(org.getKey(), "bar", description, null, null);
    verifyResponseAndDb(response, org, "bar", description, org.getUrl(), org.getAvatarUrl(), DATE_2);
  }

  @Test
  public void request_fails_if_url_is_257_chars_long() {
    userSession.logIn();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("'url' length (257) is longer than the maximum authorized (256)");

    executeKeyRequest(SOME_KEY, "bar", null, STRING_257_CHARS_LONG, null);
  }

  @Test
  public void request_succeeds_if_url_is_256_chars_long() {
    OrganizationDto org = mockForSuccessfulUpdate(DATE_1, DATE_2);
    String url = STRING_257_CHARS_LONG.substring(0, 256);
    logInAsAdministrator(org);

    Organizations.UpdateWsResponse response = executeKeyRequest(org.getKey(), "bar", null, url, null);
    verifyResponseAndDb(response, org, "bar", org.getDescription(), url, org.getAvatarUrl(), DATE_2);
  }

  @Test
  public void request_fails_if_avatar_is_257_chars_long() {
    userSession.logIn();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("'avatar' length (257) is longer than the maximum authorized (256)");

    executeKeyRequest(SOME_KEY, "bar", null, null, STRING_257_CHARS_LONG);
  }

  @Test
  public void request_succeeds_if_avatar_is_256_chars_long() {
    OrganizationDto org = mockForSuccessfulUpdate(DATE_1, DATE_2);
    String avatar = STRING_257_CHARS_LONG.substring(0, 256);
    logInAsAdministrator(org);

    Organizations.UpdateWsResponse response = executeKeyRequest(org.getKey(), "bar", null, null, avatar);
    verifyResponseAndDb(response, org, "bar", org.getDescription(), org.getUrl(), avatar, DATE_2);
  }

  @Test
  public void request_removes_optional_parameters_when_associated_parameter_are_empty() {
    OrganizationDto org = mockForSuccessfulUpdate(DATE_1, DATE_2);
    logInAsAdministrator(org);

    Organizations.UpdateWsResponse response = executeKeyRequest(org.getKey(), "bla", "", "", "");
    verifyResponseAndDb(response, org, "bla", null, null, null, DATE_2);
  }

  private OrganizationDto mockForSuccessfulUpdate(long createdAt, long nextNow) {
    OrganizationDto dto = insertOrganization(createdAt);
    when(system2.now()).thenReturn(nextNow);
    return dto;
  }

  private OrganizationDto insertOrganization(long createdAt) {
    when(system2.now()).thenReturn(createdAt);
    OrganizationDto dto = dbTester.organizations().insert();
    dbTester.commit();
    return dto;
  }

  private Organizations.UpdateWsResponse executeKeyRequest(String key, @Nullable String name) {
    return executeRequest(key, name, null, null, null);
  }

  private Organizations.UpdateWsResponse executeKeyRequest(String key, @Nullable String name,
    @Nullable String description, @Nullable String url, @Nullable String avatar) {
    return executeRequest(key, name, description, url, avatar);
  }

  private Organizations.UpdateWsResponse executeRequest(@Nullable String key,
    @Nullable String name, @Nullable String description, @Nullable String url, @Nullable String avatar) {
    TestRequest request = wsTester.newRequest();
    setParam(request, "key", key);
    setParam(request, "name", name);
    setParam(request, "description", description);
    setParam(request, "url", url);
    setParam(request, "avatar", avatar);
    return request.executeProtobuf(Organizations.UpdateWsResponse.class);
  }

  private void verifyResponseAndDb(Organizations.UpdateWsResponse response, OrganizationDto dto, String name, long updateAt) {
    verifyResponseAndDb(response, dto, name, dto.getDescription(), dto.getUrl(), dto.getAvatarUrl(), updateAt);
  }

  private void verifyResponseAndDb(Organizations.UpdateWsResponse response,
    OrganizationDto dto, String name,
    @Nullable String description, @Nullable String url, @Nullable String avatar,
    long updateAt) {
    Organizations.Organization organization = response.getOrganization();
    assertThat(organization.getName()).isEqualTo(name);
    assertThat(organization.getKey()).isEqualTo(dto.getKey());
    if (description == null) {
      assertThat(organization.hasDescription()).isFalse();
    } else {
      assertThat(organization.getDescription()).isEqualTo(description);
    }
    if (url == null) {
      assertThat(organization.hasUrl()).isFalse();
    } else {
      assertThat(organization.getUrl()).isEqualTo(url);
    }
    if (avatar == null) {
      assertThat(organization.hasAvatar()).isFalse();
    } else {
      assertThat(organization.getAvatar()).isEqualTo(avatar);
    }

    OrganizationDto newDto = dbTester.getDbClient().organizationDao().selectByUuid(dbTester.getSession(), dto.getUuid()).get();
    assertThat(newDto.getUuid()).isEqualTo(newDto.getUuid());
    assertThat(newDto.getKey()).isEqualTo(newDto.getKey());
    assertThat(newDto.getName()).isEqualTo(name);
    assertThat(newDto.getDescription()).isEqualTo(description);
    assertThat(newDto.getUrl()).isEqualTo(url);
    assertThat(newDto.getAvatarUrl()).isEqualTo(avatar);
    assertThat(newDto.getCreatedAt()).isEqualTo(newDto.getCreatedAt());
    assertThat(newDto.getUpdatedAt()).isEqualTo(updateAt);
  }

  private void logInAsAdministrator(OrganizationDto organization) {
    userSession.logIn().addPermission(ADMINISTER, organization);
  }
}
