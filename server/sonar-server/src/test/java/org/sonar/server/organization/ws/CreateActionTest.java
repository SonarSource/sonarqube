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

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.organization.DefaultTemplates;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.permission.template.PermissionTemplateGroupDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserMembershipDto;
import org.sonar.db.user.UserMembershipQuery;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.organization.OrganizationCreation;
import org.sonar.server.organization.OrganizationCreationImpl;
import org.sonar.server.organization.OrganizationValidation;
import org.sonar.server.organization.OrganizationValidationImpl;
import org.sonar.server.organization.TestOrganizationFlags;
import org.sonar.server.qualityprofile.BuiltInQProfileRepository;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.index.UserIndex;
import org.sonar.server.user.index.UserIndexDefinition;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.usergroups.DefaultGroupCreatorImpl;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.Organizations.CreateWsResponse;
import org.sonarqube.ws.Organizations.Organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
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
  public DbTester dbTester = DbTester.create(system2).setDisableDefaultOrganization(true);
  @Rule
  public EsTester es = new EsTester(new UserIndexDefinition(new MapSettings().asConfig()));
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DbClient dbClient = dbTester.getDbClient();
  private DbSession dbSession = dbTester.getSession();
  private MapSettings settings = new MapSettings()
    .setProperty(ORGANIZATIONS_ANYONE_CAN_CREATE, false);
  private UuidFactory uuidFactory = mock(UuidFactory.class);
  private OrganizationValidation organizationValidation = new OrganizationValidationImpl();
  private UserIndexer userIndexer = new UserIndexer(dbClient, es.client());
  private UserIndex userIndex = new UserIndex(es.client(), System2.INSTANCE);
  private OrganizationCreation organizationCreation = new OrganizationCreationImpl(dbClient, system2, uuidFactory, organizationValidation, settings.asConfig(), userIndexer,
    mock(BuiltInQProfileRepository.class), new DefaultGroupCreatorImpl(dbClient));
  private TestOrganizationFlags organizationFlags = TestOrganizationFlags.standalone().setEnabled(true);

  private UserDto user;

  private CreateAction underTest = new CreateAction(settings.asConfig(), userSession, dbClient, new OrganizationsWsSupport(organizationValidation), organizationValidation,
    organizationCreation, organizationFlags);

  private WsActionTester wsTester = new WsActionTester(underTest);

  @Before
  public void setUp() {
    user = dbTester.users().insertUser();
    userIndexer.indexOnStartup(new HashSet<>());
    userSession.logIn(user);
  }

  @Test
  public void verify_define() {
    WebService.Action action = wsTester.getDef();
    assertThat(action.key()).isEqualTo("create");
    assertThat(action.isPost()).isTrue();
    assertThat(action.description()).isEqualTo("Create an organization.<br />" +
      "Requires 'Administer System' permission unless any logged in user is allowed to create an organization (see appropriate setting). Organization support must be enabled.");
    assertThat(action.isInternal()).isTrue();
    assertThat(action.since()).isEqualTo("6.2");
    assertThat(action.handler()).isEqualTo(underTest);
    assertThat(action.params()).hasSize(5);
    assertThat(action.responseExample()).isEqualTo(getClass().getResource("create-example.json"));
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
  public void verify_response_example() {
    logInAsSystemAdministrator();
    mockForSuccessfulInsert(Uuids.UUID_EXAMPLE_01, SOME_DATE);
    dbTester.qualityGates().insertBuiltInQualityGate();

    String response = executeJsonRequest("Foo Company", "foo-company", "The Foo company produces quality software for Bar.", "https://www.foo.com", "https://www.foo.com/foo.png");

    assertJson(response).isSimilarTo(wsTester.getDef().responseExampleAsString());
  }

  @Test
  public void request_fails_if_user_is_not_logged_in_and_logged_in_users_cannot_create_organizations() {
    userSession.anonymous();

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    executeRequest("name");
  }

  @Test
  public void request_fails_if_user_is_not_logged_in_and_logged_in_users_can_create_organizations() {
    userSession.anonymous();
    settings.setProperty(ORGANIZATIONS_ANYONE_CAN_CREATE, true);

    expectedException.expect(UnauthorizedException.class);
    expectedException.expectMessage("Authentication is required");

    executeRequest("name");
  }

  @Test
  public void request_fails_if_user_is_not_system_administrator_and_logged_in_users_cannot_create_organizations() {
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    executeRequest("name");
  }

  @Test
  public void request_succeeds_if_user_is_system_administrator_and_logged_in_users_cannot_create_organizations() {
    logInAsSystemAdministrator();
    mockForSuccessfulInsert(SOME_UUID, SOME_DATE);
    dbTester.qualityGates().insertBuiltInQualityGate();

    verifyResponseAndDb(executeRequest("foo"), SOME_UUID, "foo", "foo", SOME_DATE);
  }

  @Test
  public void request_succeeds_if_user_is_system_administrator_and_logged_in_users_can_create_organizations() {
    logInAsSystemAdministrator();
    settings.setProperty(ORGANIZATIONS_ANYONE_CAN_CREATE, true);
    mockForSuccessfulInsert(SOME_UUID, SOME_DATE);
    dbTester.qualityGates().insertBuiltInQualityGate();

    verifyResponseAndDb(executeRequest("foo"), SOME_UUID, "foo", "foo", SOME_DATE);
  }

  @Test
  public void request_succeeds_if_user_is_not_system_administrator_and_logged_in_users_can_create_organizations() {
    userSession.logIn(user);
    settings.setProperty(ORGANIZATIONS_ANYONE_CAN_CREATE, true);
    mockForSuccessfulInsert(SOME_UUID, SOME_DATE);
    dbTester.qualityGates().insertBuiltInQualityGate();

    verifyResponseAndDb(executeRequest("foo"), SOME_UUID, "foo", "foo", SOME_DATE);
  }

  @Test
  public void request_fails_if_name_param_is_missing() {
    logInAsSystemAdministrator();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'name' parameter is missing");

    executeRequest(null);
  }

  @Test
  public void request_fails_if_name_is_one_char_long() {
    logInAsSystemAdministrator();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Name 'a' must be at least 2 chars long");

    executeRequest("a");
  }

  @Test
  public void request_succeeds_if_name_is_two_chars_long() {
    logInAsSystemAdministrator();
    mockForSuccessfulInsert(SOME_UUID, SOME_DATE);
    dbTester.qualityGates().insertBuiltInQualityGate();

    verifyResponseAndDb(executeRequest("ab"), SOME_UUID, "ab", "ab", SOME_DATE);
  }

  @Test
  public void request_fails_if_name_is_65_chars_long() {
    logInAsSystemAdministrator();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("'name' length (65) is longer than the maximum authorized (64)");

    executeRequest(STRING_65_CHARS_LONG);
  }

  @Test
  public void request_succeeds_if_name_is_64_char_long() {
    logInAsSystemAdministrator();
    mockForSuccessfulInsert(SOME_UUID, SOME_DATE);
    dbTester.qualityGates().insertBuiltInQualityGate();

    String name = STRING_65_CHARS_LONG.substring(0, 64);

    verifyResponseAndDb(executeRequest(name), SOME_UUID, name, name.substring(0, 32), SOME_DATE);
  }

  @Test
  public void request_fails_if_key_one_char_long() {
    logInAsSystemAdministrator();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Key 'a' must be at least 2 chars long");

    executeRequest("foo", "a");
  }

  @Test
  public void request_fails_if_key_is_33_chars_long() {
    logInAsSystemAdministrator();

    String key = STRING_65_CHARS_LONG.substring(0, 33);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("'key' length (33) is longer than the maximum authorized (32)");

    executeRequest("foo", key);
  }

  @Test
  public void request_succeeds_if_key_is_2_chars_long() {
    logInAsSystemAdministrator();
    mockForSuccessfulInsert(SOME_UUID, SOME_DATE);
    dbTester.qualityGates().insertBuiltInQualityGate();

    verifyResponseAndDb(executeRequest("foo", "ab"), SOME_UUID, "foo", "ab", SOME_DATE);
  }

  @Test
  public void requests_succeeds_if_key_is_32_chars_long() {
    logInAsSystemAdministrator();
    mockForSuccessfulInsert(SOME_UUID, SOME_DATE);
    dbTester.qualityGates().insertBuiltInQualityGate();

    String key = STRING_65_CHARS_LONG.substring(0, 32);

    verifyResponseAndDb(executeRequest("foo", key), SOME_UUID, "foo", key, SOME_DATE);
  }

  @Test
  public void requests_fails_if_key_contains_non_ascii_chars_but_dash() {
    logInAsSystemAdministrator();

    requestFailsWithInvalidCharInKey("ab@");
  }

  @Test
  public void request_fails_if_key_starts_with_a_dash() {
    logInAsSystemAdministrator();

    requestFailsWithInvalidCharInKey("-ab");
  }

  @Test
  public void request_fails_if_key_ends_with_a_dash() {
    logInAsSystemAdministrator();

    requestFailsWithInvalidCharInKey("ab-");
  }

  @Test
  public void request_fails_if_key_contains_space() {
    logInAsSystemAdministrator();

    requestFailsWithInvalidCharInKey("a b");
  }

  private void requestFailsWithInvalidCharInKey(String key) {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Key '" + key + "' contains at least one invalid char");

    executeRequest("foo", key);
  }

  @Test
  public void request_fails_if_key_is_specified_and_already_exists_in_DB() {
    logInAsSystemAdministrator();
    OrganizationDto org = dbTester.organizations().insert(o -> o.setKey("the-key"));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Key '" + org.getKey() + "' is already used. Specify another one.");

    executeRequest("foo", org.getKey());
  }

  @Test
  public void request_fails_if_key_computed_from_name_already_exists_in_DB() {
    logInAsSystemAdministrator();
    String key = STRING_65_CHARS_LONG.substring(0, 32);
    dbTester.organizations().insert(o -> o.setKey(key));

    String name = STRING_65_CHARS_LONG.substring(0, 64);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Key '" + key + "' generated from name '" + name + "' is already used. Specify one.");

    executeRequest(name);
  }

  @Test
  public void request_succeeds_if_description_url_and_avatar_are_not_specified() {
    logInAsSystemAdministrator();
    mockForSuccessfulInsert(SOME_UUID, SOME_DATE);
    dbTester.qualityGates().insertBuiltInQualityGate();

    CreateWsResponse response = executeRequest("foo", "bar", null, null, null);
    verifyResponseAndDb(response, SOME_UUID, "foo", "bar", null, null, null, SOME_DATE);
  }

  @Test
  public void request_succeeds_if_description_url_and_avatar_are_specified() {
    logInAsSystemAdministrator();
    mockForSuccessfulInsert(SOME_UUID, SOME_DATE);
    dbTester.qualityGates().insertBuiltInQualityGate();

    CreateWsResponse response = executeRequest("foo", "bar", "moo", "doo", "boo");
    verifyResponseAndDb(response, SOME_UUID, "foo", "bar", "moo", "doo", "boo", SOME_DATE);
  }

  @Test
  public void request_succeeds_to_generate_key_from_name_more_then_32_chars_long() {
    logInAsSystemAdministrator();
    mockForSuccessfulInsert(SOME_UUID, SOME_DATE);
    dbTester.qualityGates().insertBuiltInQualityGate();

    String name = STRING_65_CHARS_LONG.substring(0, 33);

    CreateWsResponse response = executeRequest(name);
    verifyResponseAndDb(response, SOME_UUID, name, name.substring(0, 32), SOME_DATE);
  }

  @Test
  public void request_generates_key_ignoring_multiple_following_spaces() {
    logInAsSystemAdministrator();
    mockForSuccessfulInsert(SOME_UUID, SOME_DATE);
    dbTester.qualityGates().insertBuiltInQualityGate();

    String name = "ab   cd";

    CreateWsResponse response = executeRequest(name);
    verifyResponseAndDb(response, SOME_UUID, name, "ab-cd", SOME_DATE);
  }

  @Test
  public void request_fails_if_description_is_257_chars_long() {
    logInAsSystemAdministrator();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("'description' length (257) is longer than the maximum authorized (256)");

    executeRequest("foo", "bar", STRING_257_CHARS_LONG, null, null);
  }

  @Test
  public void request_succeeds_if_description_is_256_chars_long() {
    logInAsSystemAdministrator();
    mockForSuccessfulInsert(SOME_UUID, SOME_DATE);
    String description = STRING_257_CHARS_LONG.substring(0, 256);
    dbTester.qualityGates().insertBuiltInQualityGate();

    CreateWsResponse response = executeRequest("foo", "bar", description, null, null);
    verifyResponseAndDb(response, SOME_UUID, "foo", "bar", description, null, null, SOME_DATE);
  }

  @Test
  public void request_fails_if_url_is_257_chars_long() {
    logInAsSystemAdministrator();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("'url' length (257) is longer than the maximum authorized (256)");

    executeRequest("foo", "bar", null, STRING_257_CHARS_LONG, null);
  }

  @Test
  public void request_succeeds_if_url_is_256_chars_long() {
    logInAsSystemAdministrator();
    mockForSuccessfulInsert(SOME_UUID, SOME_DATE);
    String url = STRING_257_CHARS_LONG.substring(0, 256);
    dbTester.qualityGates().insertBuiltInQualityGate();

    CreateWsResponse response = executeRequest("foo", "bar", null, url, null);
    verifyResponseAndDb(response, SOME_UUID, "foo", "bar", null, url, null, SOME_DATE);
  }

  @Test
  public void request_fails_if_avatar_is_257_chars_long() {
    logInAsSystemAdministrator();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("'avatar' length (257) is longer than the maximum authorized (256)");

    executeRequest("foo", "bar", null, null, STRING_257_CHARS_LONG);
  }

  @Test
  public void request_succeeds_if_avatar_is_256_chars_long() {
    logInAsSystemAdministrator();
    mockForSuccessfulInsert(SOME_UUID, SOME_DATE);
    String avatar = STRING_257_CHARS_LONG.substring(0, 256);
    dbTester.qualityGates().insertBuiltInQualityGate();

    CreateWsResponse response = executeRequest("foo", "bar", null, null, avatar);
    verifyResponseAndDb(response, SOME_UUID, "foo", "bar", null, null, avatar, SOME_DATE);
  }

  @Test
  public void request_creates_owners_group_with_all_permissions_for_new_organization_and_add_current_user_to_it() {
    mockForSuccessfulInsert(SOME_UUID, SOME_DATE);
    UserDto user = dbTester.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
    dbTester.qualityGates().insertBuiltInQualityGate();

    executeRequest("orgFoo");

    DbSession dbSession = dbTester.getSession();
    OrganizationDto organization = dbClient.organizationDao().selectByKey(dbSession, "orgfoo").get();
    Optional<GroupDto> groupDtoOptional = dbClient.groupDao().selectByName(dbSession, organization.getUuid(), "Owners");
    assertThat(groupDtoOptional).isNotEmpty();
    GroupDto groupDto = groupDtoOptional.get();
    assertThat(groupDto.getDescription()).isEqualTo("Owners of organization orgFoo");
    assertThat(dbClient.groupPermissionDao().selectGlobalPermissionsOfGroup(dbSession, groupDto.getOrganizationUuid(), groupDto.getId()))
      .containsOnly(GlobalPermissions.ALL.toArray(new String[GlobalPermissions.ALL.size()]));
    List<UserMembershipDto> members = dbClient.groupMembershipDao().selectMembers(
      dbSession,
      UserMembershipQuery.builder()
        .organizationUuid(organization.getUuid())
        .groupId(groupDto.getId())
        .membership(UserMembershipQuery.IN).build(),
      0, Integer.MAX_VALUE);
    assertThat(members)
      .extracting(UserMembershipDto::getLogin)
      .containsOnly(user.getLogin());
  }

  @Test
  public void request_creates_members_group_and_add_current_user_to_it() {
    mockForSuccessfulInsert(SOME_UUID, SOME_DATE);
    UserDto user = dbTester.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
    dbTester.qualityGates().insertBuiltInQualityGate();

    executeRequest("orgFoo");

    DbSession dbSession = dbTester.getSession();
    OrganizationDto organization = dbClient.organizationDao().selectByKey(dbSession, "orgfoo").get();
    Optional<GroupDto> groupDtoOptional = dbClient.groupDao().selectByName(dbSession, organization.getUuid(), "Members");
    assertThat(groupDtoOptional).isNotEmpty();
    GroupDto groupDto = groupDtoOptional.get();
    assertThat(groupDto.getDescription()).isEqualTo("All members of the organization");
    assertThat(dbClient.groupPermissionDao().selectGlobalPermissionsOfGroup(dbSession, groupDto.getOrganizationUuid(), groupDto.getId())).isEmpty();
    List<UserMembershipDto> members = dbClient.groupMembershipDao().selectMembers(
      dbSession,
      UserMembershipQuery.builder()
        .organizationUuid(organization.getUuid())
        .groupId(groupDto.getId())
        .membership(UserMembershipQuery.IN).build(),
      0, Integer.MAX_VALUE);
    assertThat(members)
      .extracting(UserMembershipDto::getLogin)
      .containsOnly(user.getLogin());
  }

  @Test
  public void request_creates_default_template_for_owner_group() {
    mockForSuccessfulInsert(SOME_UUID, SOME_DATE);
    UserDto user = dbTester.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
    dbTester.qualityGates().insertBuiltInQualityGate();

    executeRequest("orgFoo");

    OrganizationDto organization = dbClient.organizationDao().selectByKey(dbSession, "orgfoo").get();
    GroupDto ownersGroup = dbClient.groupDao().selectByName(dbSession, organization.getUuid(), "Owners").get();
    GroupDto defaultGroup = dbClient.groupDao().selectByName(dbSession, organization.getUuid(), "Members").get();
    PermissionTemplateDto defaultTemplate = dbClient.permissionTemplateDao().selectByName(dbSession, organization.getUuid(), "default template");
    assertThat(defaultTemplate.getName()).isEqualTo("Default template");
    assertThat(defaultTemplate.getDescription()).isEqualTo("Default permission template of organization orgFoo");
    DefaultTemplates defaultTemplates = dbClient.organizationDao().getDefaultTemplates(dbSession, organization.getUuid()).get();
    assertThat(defaultTemplates.getProjectUuid()).isEqualTo(defaultTemplate.getUuid());
    assertThat(defaultTemplates.getViewUuid()).isNull();
    assertThat(dbClient.permissionTemplateDao().selectGroupPermissionsByTemplateId(dbSession, defaultTemplate.getId()))
      .extracting(PermissionTemplateGroupDto::getGroupId, PermissionTemplateGroupDto::getPermission)
      .containsOnly(
        tuple(ownersGroup.getId(), UserRole.ADMIN), tuple(ownersGroup.getId(), UserRole.ISSUE_ADMIN), tuple(ownersGroup.getId(), GlobalPermissions.SCAN_EXECUTION),
        tuple(defaultGroup.getId(), UserRole.USER), tuple(defaultGroup.getId(), UserRole.CODEVIEWER));
  }

  @Test
  public void request_set_user_as_member_of_organization() {
    mockForSuccessfulInsert(SOME_UUID, SOME_DATE);
    UserDto user = dbTester.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
    dbTester.qualityGates().insertBuiltInQualityGate();

    executeRequest("orgFoo");

    assertThat(dbClient.organizationMemberDao().select(dbSession, SOME_UUID, user.getId())).isPresent();
    assertThat(userIndex.getNullableByLogin(user.getLogin()).organizationUuids()).contains(SOME_UUID);
  }

  @Test
  public void request_fails_with_IllegalStateException_if_organization_support_is_disabled() {
    organizationFlags.setEnabled(false);
    logInAsSystemAdministrator();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Organization support is disabled");

    executeJsonRequest("Foo Company", "foo-company", "The Foo company produces quality software for Bar.", "https://www.foo.com", "https://www.foo.com/foo.png");
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
    TestRequest request = wsTester.newRequest();
    populateRequest(name, key, description, url, avatar, request);
    return request.executeProtobuf(CreateWsResponse.class);
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

    OrganizationDto dto = dbClient.organizationDao().selectByUuid(dbTester.getSession(), id).get();
    assertThat(dto.getUuid()).isEqualTo(id);
    assertThat(dto.getKey()).isEqualTo(key);
    assertThat(dto.getName()).isEqualTo(name);
    assertThat(dto.getDescription()).isEqualTo(description);
    assertThat(dto.getUrl()).isEqualTo(url);
    assertThat(dto.getAvatarUrl()).isEqualTo(avatar);
    assertThat(dto.getCreatedAt()).isEqualTo(createdAt);
    assertThat(dto.getUpdatedAt()).isEqualTo(createdAt);
  }

  private void logInAsSystemAdministrator() {
    userSession.logIn(user).setSystemAdministrator();
  }
}
