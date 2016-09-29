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
import java.net.URISyntaxException;
import javax.annotation.Nullable;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.MapSettings;
import org.sonar.api.config.Settings;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.Organizations.CreateWsResponse;
import org.sonarqube.ws.Organizations.Organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.core.config.CorePropertyDefinitions.ORGANIZATIONS_ANYONE_CAN_CREATE;
import static org.sonar.server.organization.ws.OrganizationsWsTestSupport.STRING_257_CHARS_LONG;
import static org.sonar.server.organization.ws.OrganizationsWsTestSupport.STRING_65_CHARS_LONG;
import static org.sonar.test.JsonAssert.assertJson;

public class CreateActionTest {
  private static final String SOME_UUID = "uuid";
  private static final long SOME_DATE = 1_200_000L;

  private System2 system2 = mock(System2.class);

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester dbTester = DbTester.create(system2);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private Settings settings = new MapSettings()
    .setProperty(ORGANIZATIONS_ANYONE_CAN_CREATE, false);
  private UuidFactory uuidFactory = mock(UuidFactory.class);
  private CreateAction underTest = new CreateAction(settings, userSession, dbTester.getDbClient(), uuidFactory, new OrganizationsWsSupport());
  private WsActionTester wsTester = new WsActionTester(underTest);

  @Test
  public void verify_define() {
    WebService.Action action = wsTester.getDef();
    assertThat(action.key()).isEqualTo("create");
    assertThat(action.isPost()).isTrue();
    assertThat(action.description()).isEqualTo("Create an organization.<br />" +
        "Requires 'Administer System' permission unless any logged in user is allowed to create an organization (see appropriate setting).");
    assertThat(action.isInternal()).isTrue();
    assertThat(action.since()).isEqualTo("6.2");
    assertThat(action.handler()).isEqualTo(underTest);
    assertThat(action.params()).hasSize(5);
    assertThat(action.responseExample()).isEqualTo(getClass().getResource("example-create.json"));
    assertThat(action.param("name"))
      .matches(WebService.Param::isRequired)
      .matches(param -> "Foo Company".equals(param.exampleValue()))
      .matches(param -> param.description() != null);
    assertThat(action.param("key"))
      .matches(param -> !param.isRequired())
      .matches(param -> "foo-company".equals(param.exampleValue()))
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
  public void verify_response_example() throws URISyntaxException, IOException {
    giveUserSystemAdminPermission();
    mockForSuccessfulInsert(Uuids.UUID_EXAMPLE_01, SOME_DATE);

    String response = executeJsonRequest("Foo Company", "foo-company", "The Foo company produces quality software for Bar.", "https://www.foo.com", "https://www.foo.com/foo.png");

    assertJson(response).isSimilarTo(IOUtils.toString(getClass().getResource("example-create.json")));
  }

  @Test
  public void request_fails_if_user_does_not_have_SYSTEM_ADMIN_permission_and_logged_in_user_can_not_create_organizations() {
    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    executeRequest("name");
  }

  @Test
  public void request_succeeds_if_user_has_SYSTEM_ADMIN_permission_and_logged_in_user_can_not_create_organizations() {
    giveUserSystemAdminPermission();
    mockForSuccessfulInsert(SOME_UUID, SOME_DATE);

    verifyResponseAndDb(executeRequest("foo"), SOME_UUID, "foo", "foo", SOME_DATE);
  }

  @Test
  public void request_fails_if_user_is_not_logged_in_and_logged_in_user_can_create_organizations() {
    settings.setProperty(ORGANIZATIONS_ANYONE_CAN_CREATE, true);

    expectedException.expect(UnauthorizedException.class);
    expectedException.expectMessage("Authentication is required");

    executeRequest("name");
  }

  @Test
  public void request_succeeds_if_user_is_logged_in_and_logged_in_user_can_create_organizations() {
    settings.setProperty(ORGANIZATIONS_ANYONE_CAN_CREATE, true);
    userSession.login();
    mockForSuccessfulInsert(SOME_UUID, SOME_DATE);

    verifyResponseAndDb(executeRequest("foo"), SOME_UUID, "foo", "foo", SOME_DATE);
  }

  @Test
  public void request_fails_if_name_param_is_missing() {
    giveUserSystemAdminPermission();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'name' parameter is missing");

    executeRequest(null);
  }

  @Test
  public void request_fails_if_name_is_one_char_long() {
    giveUserSystemAdminPermission();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Name 'a' must be at least 2 chars long");

    executeRequest("a");
  }

  @Test
  public void request_succeeds_if_name_is_two_chars_long() {
    giveUserSystemAdminPermission();
    mockForSuccessfulInsert(SOME_UUID, SOME_DATE);

    verifyResponseAndDb(executeRequest("ab"), SOME_UUID, "ab", "ab", SOME_DATE);
  }

  @Test
  public void request_fails_if_name_is_65_chars_long() {
    giveUserSystemAdminPermission();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Name '" + STRING_65_CHARS_LONG + "' must be at most 64 chars long");

    executeRequest(STRING_65_CHARS_LONG);
  }

  @Test
  public void request_succeeds_if_name_is_64_char_long() {
    giveUserSystemAdminPermission();
    mockForSuccessfulInsert(SOME_UUID, SOME_DATE);

    String name = STRING_65_CHARS_LONG.substring(0, 64);

    verifyResponseAndDb(executeRequest(name), SOME_UUID, name, name.substring(0, 32), SOME_DATE);
  }

  @Test
  public void request_fails_if_key_one_char_long() {
    giveUserSystemAdminPermission();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Key 'a' must be at least 2 chars long");

    executeRequest("foo", "a");
  }

  @Test
  public void request_fails_if_key_is_33_chars_long() {
    giveUserSystemAdminPermission();

    String key = STRING_65_CHARS_LONG.substring(0, 33);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Key '" + key + "' must be at most 32 chars long");

    executeRequest("foo", key);
  }

  @Test
  public void request_succeeds_if_key_is_2_chars_long() {
    giveUserSystemAdminPermission();
    mockForSuccessfulInsert(SOME_UUID, SOME_DATE);

    verifyResponseAndDb(executeRequest("foo", "ab"), SOME_UUID, "foo", "ab", SOME_DATE);
  }

  @Test
  public void requests_succeeds_if_key_is_32_chars_long() {
    giveUserSystemAdminPermission();
    mockForSuccessfulInsert(SOME_UUID, SOME_DATE);

    String key = STRING_65_CHARS_LONG.substring(0, 32);

    verifyResponseAndDb(executeRequest("foo", key), SOME_UUID, "foo", key, SOME_DATE);
  }

  @Test
  public void requests_fails_if_key_contains_non_ascii_chars_but_dash() {
    giveUserSystemAdminPermission();

    requestFailsWithInvalidCharInKey("ab@");
  }

  @Test
  public void request_fails_if_key_starts_with_a_dash() {
    giveUserSystemAdminPermission();

    requestFailsWithInvalidCharInKey("-ab");
  }

  @Test
  public void request_fails_if_key_ends_with_a_dash() {
    giveUserSystemAdminPermission();

    requestFailsWithInvalidCharInKey("ab-");
  }

  @Test
  public void request_fails_if_key_contains_space() {
    giveUserSystemAdminPermission();

    requestFailsWithInvalidCharInKey("a b");
  }

  private void requestFailsWithInvalidCharInKey(String key) {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Key '" + key + "' contains at least one invalid char");

    executeRequest("foo", key);
  }

  @Test
  public void request_fails_if_key_is_specified_and_already_exists_in_DB() {
    giveUserSystemAdminPermission();
    String key = "the-key";
    insertOrganization(key);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Key '" + key + "' is already used. Specify another one.");

    executeRequest("foo", key);
  }

  @Test
  public void request_fails_if_key_computed_from_name_already_exists_in_DB() {
    giveUserSystemAdminPermission();
    String key = STRING_65_CHARS_LONG.substring(0, 32);
    insertOrganization(key);

    String name = STRING_65_CHARS_LONG.substring(0, 64);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Key '" + key + "' generated from name '" + name + "' is already used. Specify one.");

    executeRequest(name);
  }

  @Test
  public void request_succeeds_if_description_url_and_avatar_are_not_specified() {
    giveUserSystemAdminPermission();
    mockForSuccessfulInsert(SOME_UUID, SOME_DATE);

    CreateWsResponse response = executeRequest("foo", "bar", null, null, null);
    verifyResponseAndDb(response, SOME_UUID, "foo", "bar", null, null, null, SOME_DATE);
  }

  @Test
  public void request_succeeds_if_description_url_and_avatar_are_specified() {
    giveUserSystemAdminPermission();
    mockForSuccessfulInsert(SOME_UUID, SOME_DATE);

    CreateWsResponse response = executeRequest("foo", "bar", "moo", "doo", "boo");
    verifyResponseAndDb(response, SOME_UUID, "foo", "bar", "moo", "doo", "boo", SOME_DATE);
  }

  @Test
  public void request_succeeds_to_generate_key_from_name_more_then_32_chars_long() {
    giveUserSystemAdminPermission();
    mockForSuccessfulInsert(SOME_UUID, SOME_DATE);

    String name = STRING_65_CHARS_LONG.substring(0, 33);

    CreateWsResponse response = executeRequest(name);
    verifyResponseAndDb(response, SOME_UUID, name, name.substring(0, 32), SOME_DATE);
  }

  @Test
  public void request_generates_key_ignoring_multiple_following_spaces() {
    giveUserSystemAdminPermission();
    mockForSuccessfulInsert(SOME_UUID, SOME_DATE);

    String name = "ab   cd";

    CreateWsResponse response = executeRequest(name);
    verifyResponseAndDb(response, SOME_UUID, name, "ab-cd", SOME_DATE);
  }

  @Test
  public void request_fails_if_description_is_257_chars_long() {
    giveUserSystemAdminPermission();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("description '" + STRING_257_CHARS_LONG + "' must be at most 256 chars long");

    executeRequest("foo", "bar", STRING_257_CHARS_LONG, null, null);
  }

  @Test
  public void request_succeeds_if_description_is_256_chars_long() {
    giveUserSystemAdminPermission();
    mockForSuccessfulInsert(SOME_UUID, SOME_DATE);
    String description = STRING_257_CHARS_LONG.substring(0, 256);

    CreateWsResponse response = executeRequest("foo", "bar", description, null, null);
    verifyResponseAndDb(response, SOME_UUID, "foo", "bar", description, null, null, SOME_DATE);
  }

  @Test
  public void request_fails_if_url_is_257_chars_long() {
    giveUserSystemAdminPermission();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("url '" + STRING_257_CHARS_LONG + "' must be at most 256 chars long");

    executeRequest("foo", "bar", null, STRING_257_CHARS_LONG, null);
  }

  @Test
  public void request_succeeds_if_url_is_256_chars_long() {
    giveUserSystemAdminPermission();
    mockForSuccessfulInsert(SOME_UUID, SOME_DATE);
    String url = STRING_257_CHARS_LONG.substring(0, 256);

    CreateWsResponse response = executeRequest("foo", "bar", null, url, null);
    verifyResponseAndDb(response, SOME_UUID, "foo", "bar", null, url, null, SOME_DATE);
  }

  @Test
  public void request_fails_if_avatar_is_257_chars_long() {
    giveUserSystemAdminPermission();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("avatar '" + STRING_257_CHARS_LONG + "' must be at most 256 chars long");

    executeRequest("foo", "bar", null, null, STRING_257_CHARS_LONG);
  }

  @Test
  public void request_succeeds_if_avatar_is_256_chars_long() {
    giveUserSystemAdminPermission();
    mockForSuccessfulInsert(SOME_UUID, SOME_DATE);
    String avatar = STRING_257_CHARS_LONG.substring(0, 256);

    CreateWsResponse response = executeRequest("foo", "bar", null, null, avatar);
    verifyResponseAndDb(response, SOME_UUID, "foo", "bar", null, null, avatar, SOME_DATE);
  }

  private void giveUserSystemAdminPermission() {
    userSession.setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
  }

  private void mockForSuccessfulInsert(String uuid, long now) {
    when(uuidFactory.create()).thenReturn(uuid);
    when(system2.now()).thenReturn(now);
  }

  private CreateWsResponse executeRequest(@Nullable String name, @Nullable String key) {
    return executeRequest(name, key, null, null, null);
  }

  private CreateWsResponse executeRequest(@Nullable String name) {
    return executeRequest(name, null, null, null, null);
  }

  private CreateWsResponse executeRequest(@Nullable String name, @Nullable String key, @Nullable String description, @Nullable String url, @Nullable String avatar) {
    TestRequest request = wsTester.newRequest()
      .setMediaType(MediaTypes.PROTOBUF);
    populateRequest(name, key, description, url, avatar, request);
    try {
      return CreateWsResponse.parseFrom(request.execute().getInputStream());
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private String executeJsonRequest(@Nullable String name, @Nullable String key, @Nullable String description, @Nullable String url, @Nullable String avatar) {
    TestRequest request = wsTester.newRequest()
      .setMediaType(MediaTypes.JSON);
    populateRequest(name, key, description, url, avatar, request);
    return request.execute().getInput();
  }

  private static void populateRequest(@Nullable String name, @Nullable String key, @Nullable String description, @Nullable String url, @Nullable String avatar,
    TestRequest request) {
    OrganizationsWsTestSupport.setParam(request, "name", name);
    OrganizationsWsTestSupport.setParam(request, "key", key);
    OrganizationsWsTestSupport.setParam(request, "description", description);
    OrganizationsWsTestSupport.setParam(request, "url", url);
    OrganizationsWsTestSupport.setParam(request, "avatar", avatar);
  }

  private void verifyResponseAndDb(CreateWsResponse response,
    String uuid, String name, String key,
    long createdAt) {
    verifyResponseAndDb(response, uuid, name, key, null, null, null, createdAt);
  }

  private void verifyResponseAndDb(CreateWsResponse response,
    String id, String name, String key,
    @Nullable String description, @Nullable String url, @Nullable String avatar,
    long createdAt) {
    Organization organization = response.getOrganization();
    assertThat(organization.getName()).isEqualTo(name);
    assertThat(organization.getKey()).isEqualTo(key);
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

    OrganizationDto dto = dbTester.getDbClient().organizationDao().selectByUuid(dbTester.getSession(), id).get();
    assertThat(dto.getUuid()).isEqualTo(id);
    assertThat(dto.getKey()).isEqualTo(key);
    assertThat(dto.getName()).isEqualTo(name);
    assertThat(dto.getDescription()).isEqualTo(description);
    assertThat(dto.getUrl()).isEqualTo(url);
    assertThat(dto.getAvatarUrl()).isEqualTo(avatar);
    assertThat(dto.getCreatedAt()).isEqualTo(createdAt);
    assertThat(dto.getUpdatedAt()).isEqualTo(createdAt);
  }

  private void insertOrganization(String key) {
    dbTester.getDbClient().organizationDao().insert(dbTester.getSession(), new OrganizationDto()
      .setUuid(key + "_uuid")
      .setKey(key)
      .setName(key + "_name")
      .setCreatedAt((long) key.hashCode())
      .setUpdatedAt((long) key.hashCode()));
    dbTester.commit();
  }
}
