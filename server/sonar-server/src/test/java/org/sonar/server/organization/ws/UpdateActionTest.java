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
package org.sonar.server.organization.ws;

import java.io.IOException;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.Organizations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.organization.ws.OrganizationsWsTestSupport.STRING_257_CHARS_LONG;
import static org.sonar.server.organization.ws.OrganizationsWsTestSupport.STRING_65_CHARS_LONG;
import static org.sonar.server.organization.ws.OrganizationsWsTestSupport.setParam;

public class UpdateActionTest {
  private static final String SOME_KEY = "key";
  private static final long SOME_DATE = 1_200_000L;

  private System2 system2 = mock(System2.class);

  @Rule
  public DbTester dbTester = DbTester.create(system2).setDisableDefaultOrganization(true);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private UpdateAction underTest = new UpdateAction(userSession, new OrganizationsWsSupport(), dbTester.getDbClient());
  private WsActionTester wsTester = new WsActionTester(underTest);

  @Test
  public void verify_define() {
    WebService.Action action = wsTester.getDef();
    assertThat(action.key()).isEqualTo("update");
    assertThat(action.isPost()).isTrue();
    assertThat(action.description()).isEqualTo("Update an organization.<br/>" +
      "Require 'Administer System' permission.");
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
      .matches(WebService.Param::isRequired)
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
  public void request_fails_if_user_does_not_have_SYSTEM_ADMIN_permission() {
    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    executeKeyRequest("key", "name");
  }

  @Test
  public void request_fails_if_key_is_missing() {
    giveUserSystemAdminPermission();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'key' parameter is missing");

    executeRequest(null, "name", "description", "url", "avatar");
  }

  @Test
  public void request_fails_if_name_param_is_missing() {
    giveUserSystemAdminPermission();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'name' parameter is missing");

    executeKeyRequest("key", null);
  }

  @Test
  public void request_fails_if_name_is_one_char_long() {
    giveUserSystemAdminPermission();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Name 'a' must be at least 2 chars long");

    executeKeyRequest(SOME_KEY, "a");
  }

  @Test
  public void request_succeeds_if_name_is_two_chars_long() {
    giveUserSystemAdminPermission();
    OrganizationDto dto = mockForSuccessfulUpdate(SOME_KEY, SOME_DATE);

    verifyResponseAndDb(executeKeyRequest(SOME_KEY, "ab"), dto, "ab", SOME_DATE);
  }

  @Test
  public void request_fails_if_name_is_65_chars_long() {
    giveUserSystemAdminPermission();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Name '" + STRING_65_CHARS_LONG + "' must be at most 64 chars long");

    executeKeyRequest(SOME_KEY, STRING_65_CHARS_LONG);
  }

  @Test
  public void request_succeeds_if_name_is_64_char_long() {
    giveUserSystemAdminPermission();
    OrganizationDto dto = mockForSuccessfulUpdate(SOME_KEY, SOME_DATE);

    String name = STRING_65_CHARS_LONG.substring(0, 64);

    verifyResponseAndDb(executeKeyRequest(SOME_KEY, name), dto, name, SOME_DATE);
  }

  @Test
  public void request_succeeds_if_description_url_and_avatar_are_not_specified() {
    giveUserSystemAdminPermission();
    OrganizationDto dto = mockForSuccessfulUpdate(SOME_KEY, SOME_DATE);

    Organizations.UpdateWsResponse response = executeKeyRequest(SOME_KEY, "bar", null, null, null);
    verifyResponseAndDb(response, dto, "bar", null, null, null, SOME_DATE);
  }

  @Test
  public void request_succeeds_if_description_url_and_avatar_are_specified() {
    giveUserSystemAdminPermission();
    OrganizationDto dto = mockForSuccessfulUpdate(SOME_KEY, SOME_DATE);

    Organizations.UpdateWsResponse response = executeKeyRequest(SOME_KEY, "bar", "moo", "doo", "boo");
    verifyResponseAndDb(response, dto, "bar", "moo", "doo", "boo", SOME_DATE);
  }

  @Test
  public void request_fails_if_description_is_257_chars_long() {
    giveUserSystemAdminPermission();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("description '" + STRING_257_CHARS_LONG + "' must be at most 256 chars long");

    executeKeyRequest(SOME_KEY, "bar", STRING_257_CHARS_LONG, null, null);
  }

  @Test
  public void request_succeeds_if_description_is_256_chars_long() {
    giveUserSystemAdminPermission();
    OrganizationDto dto = mockForSuccessfulUpdate(SOME_KEY, SOME_DATE);
    String description = STRING_257_CHARS_LONG.substring(0, 256);

    Organizations.UpdateWsResponse response = executeKeyRequest(SOME_KEY, "bar", description, null, null);
    verifyResponseAndDb(response, dto, "bar", description, null, null, SOME_DATE);
  }

  @Test
  public void request_fails_if_url_is_257_chars_long_when() {
    giveUserSystemAdminPermission();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("url '" + STRING_257_CHARS_LONG + "' must be at most 256 chars long");

    executeKeyRequest(SOME_KEY, "bar", null, STRING_257_CHARS_LONG, null);
  }

  @Test
  public void request_succeeds_if_url_is_256_chars_long() {
    giveUserSystemAdminPermission();
    OrganizationDto dto = mockForSuccessfulUpdate(SOME_KEY, SOME_DATE);
    String url = STRING_257_CHARS_LONG.substring(0, 256);

    Organizations.UpdateWsResponse response = executeKeyRequest(SOME_KEY, "bar", null, url, null);
    verifyResponseAndDb(response, dto, "bar", null, url, null, SOME_DATE);
  }

  @Test
  public void request_fails_if_avatar_is_257_chars_long() {
    giveUserSystemAdminPermission();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("avatar '" + STRING_257_CHARS_LONG + "' must be at most 256 chars long");

    executeKeyRequest(SOME_KEY, "bar", null, null, STRING_257_CHARS_LONG);
  }

  @Test
  public void request_succeeds_if_avatar_is_256_chars_long() {
    giveUserSystemAdminPermission();
    OrganizationDto dto = mockForSuccessfulUpdate(SOME_KEY, SOME_DATE);
    String avatar = STRING_257_CHARS_LONG.substring(0, 256);

    Organizations.UpdateWsResponse response = executeKeyRequest(SOME_KEY, "bar", null, null, avatar);
    verifyResponseAndDb(response, dto, "bar", null, null, avatar, SOME_DATE);
  }

  private void giveUserSystemAdminPermission() {
    userSession.setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
  }

  private OrganizationDto mockForSuccessfulUpdate(String key, long nextNow) {
    OrganizationDto dto = insertOrganization(key);
    when(system2.now()).thenReturn(nextNow);
    return dto;
  }

  private OrganizationDto insertOrganization(String key) {
    when(system2.now()).thenReturn((long) key.hashCode());
    OrganizationDto dto = new OrganizationDto()
      .setUuid(key + "_uuid")
      .setKey(key)
      .setName(key + "_name")
      .setDescription(key + "_description")
      .setUrl(key + "_url")
      .setAvatarUrl(key + "_avatar_url");
    dbTester.getDbClient().organizationDao().insert(dbTester.getSession(), dto);
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
    TestRequest request = wsTester.newRequest()
      .setMediaType(MediaTypes.PROTOBUF);
    setParam(request, "key", key);
    setParam(request, "name", name);
    setParam(request, "description", description);
    setParam(request, "url", url);
    setParam(request, "avatar", avatar);
    try {
      return Organizations.UpdateWsResponse.parseFrom(request.execute().getInputStream());
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private void verifyResponseAndDb(Organizations.UpdateWsResponse response, OrganizationDto dto, String name, long updateAt) {
    verifyResponseAndDb(response, dto, name, null, null, null, updateAt);
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

}
