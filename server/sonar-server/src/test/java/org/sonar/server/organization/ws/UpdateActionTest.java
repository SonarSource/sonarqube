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
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_03;
import static org.sonar.server.organization.ws.OrganizationsWsTestSupport.STRING_257_CHARS_LONG;
import static org.sonar.server.organization.ws.OrganizationsWsTestSupport.STRING_65_CHARS_LONG;
import static org.sonar.server.organization.ws.OrganizationsWsTestSupport.setParam;

public class UpdateActionTest {
  private static final String SOME_UUID = "uuid";
  private static final String SOME_KEY = "key";
  private static final long SOME_DATE = 1_200_000L;

  private System2 system2 = mock(System2.class);

  @Rule
  public DbTester dbTester = DbTester.create(system2);
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
      "The 'id' or 'key' must be provided.<br/>" +
      "Require 'Administer System' permission.");
    assertThat(action.isInternal()).isTrue();
    assertThat(action.since()).isEqualTo("6.2");
    assertThat(action.handler()).isEqualTo(underTest);
    assertThat(action.params()).hasSize(6);
    assertThat(action.responseExample()).isNull();

    assertThat(action.param("id"))
      .matches(param -> !param.isRequired())
      .matches(param -> UUID_EXAMPLE_03.equals(param.exampleValue()))
      .matches(param -> "Organization id".equals(param.description()));
    assertThat(action.param("key"))
      .matches(param -> !param.isRequired())
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

    executeIdRequest("uuid", "name");
  }

  @Test
  public void request_fails_if_both_uuid_and_key_are_missing() {
    giveUserSystemAdminPermission();

    expectUuidAndKeySetOrMissingIAE();

    executeRequest(null, null, "name", "description", "url", "avatar");
  }

  @Test
  public void request_fails_if_both_uuid_and_key_are_provided() {
    giveUserSystemAdminPermission();

    expectUuidAndKeySetOrMissingIAE();

    executeRequest("uuid", "key", "name", "description", "url", "avatar");
  }

  private void expectUuidAndKeySetOrMissingIAE() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Either 'id' or 'key' must be provided, not both");
  }

  @Test
  public void request_fails_if_name_param_is_missing_when_uuid_is_provided() {
    giveUserSystemAdminPermission();

    expectMissingNameIAE();

    executeIdRequest("uuid", null);
  }

  @Test
  public void request_fails_if_name_param_is_missing_when_key_is_provided() {
    giveUserSystemAdminPermission();

    expectMissingNameIAE();

    executeKeyRequest("key", null);
  }

  private void expectMissingNameIAE() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'name' parameter is missing");
  }

  @Test
  public void request_fails_if_name_is_one_char_long_when_uuid_is_provided() {
    giveUserSystemAdminPermission();

    expectNameTooShortIAE();

    executeIdRequest(SOME_UUID, "a");
  }

  @Test
  public void request_fails_if_name_is_one_char_long_when_key_is_provided() {
    giveUserSystemAdminPermission();

    expectNameTooShortIAE();

    executeKeyRequest(SOME_KEY, "a");
  }

  private void expectNameTooShortIAE() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Name 'a' must be at least 2 chars long");
  }

  @Test
  public void request_succeeds_if_name_is_two_chars_long_when_uuid_is_provided() {
    giveUserSystemAdminPermission();
    OrganizationDto dto = mockForSuccessfulUpdate(SOME_UUID, SOME_DATE);

    verifyResponseAndDb(executeIdRequest(SOME_UUID, "ab"), dto, "ab", SOME_DATE);
  }

  @Test
  public void request_succeeds_if_name_is_two_chars_long_when_key_is_provided() {
    giveUserSystemAdminPermission();
    OrganizationDto dto = mockForSuccessfulUpdate(SOME_UUID, SOME_DATE);

    verifyResponseAndDb(executeKeyRequest(dto.getKey(), "ab"), dto, "ab", SOME_DATE);
  }

  @Test
  public void request_fails_if_name_is_65_chars_long_when_uuid_is_provided() {
    giveUserSystemAdminPermission();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Name '" + STRING_65_CHARS_LONG + "' must be at most 64 chars long");

    executeIdRequest(SOME_UUID, STRING_65_CHARS_LONG);
  }

  @Test
  public void request_fails_if_name_is_65_chars_long_when_key_is_provided() {
    giveUserSystemAdminPermission();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Name '" + STRING_65_CHARS_LONG + "' must be at most 64 chars long");

    executeKeyRequest(SOME_KEY, STRING_65_CHARS_LONG);
  }

  @Test
  public void request_succeeds_if_name_is_64_char_long_when_uuid_is_provided() {
    giveUserSystemAdminPermission();
    OrganizationDto dto = mockForSuccessfulUpdate(SOME_UUID, SOME_DATE);

    String name = STRING_65_CHARS_LONG.substring(0, 64);

    verifyResponseAndDb(executeIdRequest(SOME_UUID, name), dto, name, SOME_DATE);
  }

  @Test
  public void request_succeeds_if_description_url_and_avatar_are_not_specified_when_uuid_is_specified() {
    giveUserSystemAdminPermission();
    OrganizationDto dto = mockForSuccessfulUpdate(SOME_UUID, SOME_DATE);

    Organizations.UpdateWsResponse response = executeIdRequest(SOME_UUID, "bar", null, null, null);
    verifyResponseAndDb(response, dto, "bar", null, null, null, SOME_DATE);
  }

  @Test
  public void request_succeeds_if_description_url_and_avatar_are_not_specified_when_key_is_specified() {
    giveUserSystemAdminPermission();
    OrganizationDto dto = mockForSuccessfulUpdate(SOME_UUID, SOME_DATE);

    Organizations.UpdateWsResponse response = executeKeyRequest(dto.getKey(), "bar", null, null, null);
    verifyResponseAndDb(response, dto, "bar", null, null, null, SOME_DATE);
  }

  @Test
  public void request_succeeds_if_description_url_and_avatar_are_specified_when_uuid_is_specified() {
    giveUserSystemAdminPermission();
    OrganizationDto dto = mockForSuccessfulUpdate(SOME_UUID, SOME_DATE);

    Organizations.UpdateWsResponse response = executeIdRequest(SOME_UUID, "bar", "moo", "doo", "boo");
    verifyResponseAndDb(response, dto, "bar", "moo", "doo", "boo", SOME_DATE);
  }

  @Test
  public void request_succeeds_if_description_url_and_avatar_are_specified_when_key_is_specified() {
    giveUserSystemAdminPermission();
    OrganizationDto dto = mockForSuccessfulUpdate(SOME_UUID, SOME_DATE);

    Organizations.UpdateWsResponse response = executeKeyRequest(dto.getKey(), "bar", "moo", "doo", "boo");
    verifyResponseAndDb(response, dto, "bar", "moo", "doo", "boo", SOME_DATE);
  }

  @Test
  public void request_fails_if_description_is_257_chars_long_when_uuid_is_specified() {
    giveUserSystemAdminPermission();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("description '" + STRING_257_CHARS_LONG + "' must be at most 256 chars long");

    executeIdRequest(SOME_UUID, "bar", STRING_257_CHARS_LONG, null, null);
  }

  @Test
  public void request_fails_if_description_is_257_chars_long_when_key_is_specified() {
    giveUserSystemAdminPermission();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("description '" + STRING_257_CHARS_LONG + "' must be at most 256 chars long");

    executeKeyRequest(SOME_KEY, "bar", STRING_257_CHARS_LONG, null, null);
  }

  @Test
  public void request_succeeds_if_description_is_256_chars_long_when_uuid_is_specified() {
    giveUserSystemAdminPermission();
    OrganizationDto dto = mockForSuccessfulUpdate(SOME_UUID, SOME_DATE);
    String description = STRING_257_CHARS_LONG.substring(0, 256);

    Organizations.UpdateWsResponse response = executeIdRequest(SOME_UUID, "bar", description, null, null);
    verifyResponseAndDb(response, dto, "bar", description, null, null, SOME_DATE);
  }

  @Test
  public void request_succeeds_if_description_is_256_chars_long_when_key_is_specified() {
    giveUserSystemAdminPermission();
    OrganizationDto dto = mockForSuccessfulUpdate(SOME_UUID, SOME_DATE);
    String description = STRING_257_CHARS_LONG.substring(0, 256);

    Organizations.UpdateWsResponse response = executeKeyRequest(dto.getKey(), "bar", description, null, null);
    verifyResponseAndDb(response, dto, "bar", description, null, null, SOME_DATE);
  }

  @Test
  public void request_fails_if_url_is_257_chars_long_when_uuid_is_specified() {
    giveUserSystemAdminPermission();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("url '" + STRING_257_CHARS_LONG + "' must be at most 256 chars long");

    executeIdRequest(SOME_UUID, "bar", null, STRING_257_CHARS_LONG, null);
  }

  @Test
  public void request_fails_if_url_is_257_chars_long_when_key_is_specified() {
    giveUserSystemAdminPermission();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("url '" + STRING_257_CHARS_LONG + "' must be at most 256 chars long");

    executeKeyRequest(SOME_KEY, "bar", null, STRING_257_CHARS_LONG, null);
  }

  @Test
  public void request_succeeds_if_url_is_256_chars_long_when_uuid_is_specified() {
    giveUserSystemAdminPermission();
    OrganizationDto dto = mockForSuccessfulUpdate(SOME_UUID, SOME_DATE);
    String url = STRING_257_CHARS_LONG.substring(0, 256);

    Organizations.UpdateWsResponse response = executeIdRequest(SOME_UUID, "bar", null, url, null);
    verifyResponseAndDb(response, dto, "bar", null, url, null, SOME_DATE);
  }

  @Test
  public void request_succeeds_if_url_is_256_chars_long_when_key_is_specified() {
    giveUserSystemAdminPermission();
    OrganizationDto dto = mockForSuccessfulUpdate(SOME_UUID, SOME_DATE);
    String url = STRING_257_CHARS_LONG.substring(0, 256);

    Organizations.UpdateWsResponse response = executeKeyRequest(dto.getKey(), "bar", null, url, null);
    verifyResponseAndDb(response, dto, "bar", null, url, null, SOME_DATE);
  }

  @Test
  public void request_fails_if_avatar_is_257_chars_long_when_uuid_is_specified() {
    giveUserSystemAdminPermission();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("avatar '" + STRING_257_CHARS_LONG + "' must be at most 256 chars long");

    executeIdRequest(SOME_UUID, "bar", null, null, STRING_257_CHARS_LONG);
  }

  @Test
  public void request_fails_if_avatar_is_257_chars_long_when_key_is_specified() {
    giveUserSystemAdminPermission();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("avatar '" + STRING_257_CHARS_LONG + "' must be at most 256 chars long");

    executeKeyRequest(SOME_KEY, "bar", null, null, STRING_257_CHARS_LONG);
  }

  @Test
  public void request_succeeds_if_avatar_is_256_chars_long_when_uuid_is_specified() {
    giveUserSystemAdminPermission();
    OrganizationDto dto = mockForSuccessfulUpdate(SOME_UUID, SOME_DATE);
    String avatar = STRING_257_CHARS_LONG.substring(0, 256);

    Organizations.UpdateWsResponse response = executeIdRequest(SOME_UUID, "bar", null, null, avatar);
    verifyResponseAndDb(response, dto, "bar", null, null, avatar, SOME_DATE);
  }

  @Test
  public void request_succeeds_if_avatar_is_256_chars_long_when_key_is_specified() {
    giveUserSystemAdminPermission();
    OrganizationDto dto = mockForSuccessfulUpdate(SOME_UUID, SOME_DATE);
    String avatar = STRING_257_CHARS_LONG.substring(0, 256);

    Organizations.UpdateWsResponse response = executeKeyRequest(dto.getKey(), "bar", null, null, avatar);
    verifyResponseAndDb(response, dto, "bar", null, null, avatar, SOME_DATE);
  }

  private void giveUserSystemAdminPermission() {
    userSession.setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
  }

  private OrganizationDto mockForSuccessfulUpdate(String uuid, long nextNow) {
    OrganizationDto dto = insertOrganization(uuid);
    when(system2.now()).thenReturn(nextNow);
    return dto;
  }

  private OrganizationDto insertOrganization(String uuid) {
    when(system2.now()).thenReturn((long) uuid.hashCode());
    OrganizationDto dto = new OrganizationDto()
      .setUuid(uuid)
      .setKey(uuid + "_key")
      .setName(uuid + "_name")
      .setDescription(uuid + "_description")
      .setUrl(uuid + "_url")
      .setAvatarUrl(uuid + "_avatar_url");
    dbTester.getDbClient().organizationDao().insert(dbTester.getSession(), dto);
    dbTester.commit();
    return dto;
  }

  private Organizations.UpdateWsResponse executeIdRequest(String uuid, @Nullable String name) {
    return executeRequest(uuid, null, name, null, null, null);
  }

  private Organizations.UpdateWsResponse executeIdRequest(String id, @Nullable String name,
    @Nullable String description, @Nullable String url, @Nullable String avatar) {
    return executeRequest(id, null, name, description, url, avatar);
  }

  private Organizations.UpdateWsResponse executeKeyRequest(String key, @Nullable String name) {
    return executeRequest(null, key, name, null, null, null);
  }

  private Organizations.UpdateWsResponse executeKeyRequest(String key, @Nullable String name,
    @Nullable String description, @Nullable String url, @Nullable String avatar) {
    return executeRequest(null, key, name, description, url, avatar);
  }

  private Organizations.UpdateWsResponse executeRequest(@Nullable String id, @Nullable String key,
    @Nullable String name, @Nullable String description, @Nullable String url, @Nullable String avatar) {
    TestRequest request = wsTester.newRequest()
      .setMediaType(MediaTypes.PROTOBUF);
    setParam(request, "id", id);
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
    assertThat(organization.getId()).isEqualTo(dto.getUuid());
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
