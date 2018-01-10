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

import java.util.HashSet;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.permission.template.PermissionTemplateUserDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.property.PropertyQuery;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.index.UserIndex;
import org.sonar.server.user.index.UserIndexDefinition;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.user.index.UserQuery;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.sonar.api.CoreProperties.DEFAULT_ISSUE_ASSIGNEE;
import static org.sonar.api.web.UserRole.CODEVIEWER;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_GATES;
import static org.sonar.db.permission.OrganizationPermission.SCAN;
import static org.sonar.server.organization.ws.OrganizationsWsSupport.PARAM_ORGANIZATION;

public class RemoveMemberActionTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone().logIn().setRoot();
  @Rule
  public EsTester es = new EsTester(new UserIndexDefinition(new MapSettings().asConfig()));
  @Rule
  public DbTester db = DbTester.create();
  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();

  private UserIndex userIndex = new UserIndex(es.client(), System2.INSTANCE);
  private UserIndexer userIndexer = new UserIndexer(dbClient, es.client());

  private WsActionTester ws = new WsActionTester(new RemoveMemberAction(dbClient, userSession, userIndexer));

  private OrganizationDto organization;
  private ComponentDto project;
  private UserDto user;

  @Before
  public void setUp() {
    organization = db.organizations().insert();
    project = db.components().insertPrivateProject(organization);

    user = db.users().insertUser();
    db.organizations().addMember(organization, user);

    UserDto adminUser = db.users().insertAdminByUserPermission(organization);
    db.organizations().addMember(organization, adminUser);

    userIndexer.indexOnStartup(new HashSet<>());
  }

  @Test
  public void definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.key()).isEqualTo("remove_member");
    assertThat(definition.since()).isEqualTo("6.4");
    assertThat(definition.isPost()).isTrue();
    assertThat(definition.isInternal()).isTrue();
    assertThat(definition.params()).extracting(WebService.Param::key).containsOnly("organization", "login");

    WebService.Param organization = definition.param("organization");
    assertThat(organization.isRequired()).isTrue();

    WebService.Param login = definition.param("login");
    assertThat(login.isRequired()).isTrue();
  }

  @Test
  public void no_content_http_204_returned() {
    TestResponse result = call(organization.getKey(), user.getLogin());

    assertThat(result.getStatus()).isEqualTo(HTTP_NO_CONTENT);
    assertThat(result.getInput()).isEmpty();
  }

  @Test
  public void remove_member_from_db_and_user_index() {
    assertMember(organization.getUuid(), user);

    call(organization.getKey(), user.getLogin());

    assertNotAMember(organization.getUuid(), user);
  }

  @Test
  public void remove_organization_permissions() {
    UserDto anotherUser = db.users().insertUser();
    OrganizationDto anotherOrganization = db.organizations().insert();
    ComponentDto anotherProject = db.components().insertPrivateProject(anotherOrganization);
    assertMember(organization.getUuid(), user);
    db.users().insertPermissionOnUser(organization, user, ADMINISTER);
    db.users().insertPermissionOnUser(organization, user, SCAN);
    db.users().insertPermissionOnUser(anotherOrganization, user, ADMINISTER);
    db.users().insertPermissionOnUser(anotherOrganization, user, SCAN);
    db.users().insertPermissionOnUser(organization, anotherUser, ADMINISTER);
    db.users().insertPermissionOnUser(organization, anotherUser, SCAN);
    db.users().insertProjectPermissionOnUser(user, CODEVIEWER, project);
    db.users().insertProjectPermissionOnUser(user, USER, project);
    db.users().insertProjectPermissionOnUser(user, CODEVIEWER, anotherProject);
    db.users().insertProjectPermissionOnUser(user, USER, anotherProject);
    db.users().insertProjectPermissionOnUser(anotherUser, CODEVIEWER, project);
    db.users().insertProjectPermissionOnUser(anotherUser, USER, project);

    call(organization.getKey(), user.getLogin());

    assertNotAMember(organization.getUuid(), user);
    assertOrgPermissionsOfUser(user, organization);
    assertOrgPermissionsOfUser(user, anotherOrganization, ADMINISTER, SCAN);
    assertOrgPermissionsOfUser(anotherUser, organization, ADMINISTER, SCAN);
    assertProjectPermissionsOfUser(user, project);
    assertProjectPermissionsOfUser(user, anotherProject, CODEVIEWER, USER);
    assertProjectPermissionsOfUser(anotherUser, project, CODEVIEWER, USER);
  }

  @Test
  public void remove_template_permissions() {
    OrganizationDto anotherOrganization = db.organizations().insert();
    UserDto anotherUser = db.users().insertUser();
    PermissionTemplateDto template = db.permissionTemplates().insertTemplate(organization);
    PermissionTemplateDto anotherTemplate = db.permissionTemplates().insertTemplate(anotherOrganization);
    String permission = "PERMISSION";
    db.permissionTemplates().addUserToTemplate(template.getId(), user.getId(), permission);
    db.permissionTemplates().addUserToTemplate(template.getId(), anotherUser.getId(), permission);
    db.permissionTemplates().addUserToTemplate(anotherTemplate.getId(), user.getId(), permission);

    call(organization.getKey(), user.getLogin());

    assertThat(dbClient.permissionTemplateDao().selectUserPermissionsByTemplateId(dbSession, template.getId())).extracting(PermissionTemplateUserDto::getUserId)
      .containsOnly(anotherUser.getId());
    assertThat(dbClient.permissionTemplateDao().selectUserPermissionsByTemplateId(dbSession, anotherTemplate.getId())).extracting(PermissionTemplateUserDto::getUserId)
      .containsOnly(user.getId());
  }

  @Test
  public void remove_qprofiles_user_permission() {
    OrganizationDto anotherOrganization = db.organizations().insert();
    db.organizations().addMember(anotherOrganization, user);
    QProfileDto profile = db.qualityProfiles().insert(organization);
    QProfileDto anotherProfile = db.qualityProfiles().insert(anotherOrganization);
    db.qualityProfiles().addUserPermission(profile, user);
    db.qualityProfiles().addUserPermission(anotherProfile, user);

    call(organization.getKey(), user.getLogin());

    assertThat(db.getDbClient().qProfileEditUsersDao().exists(dbSession, profile, user)).isFalse();
    assertThat(db.getDbClient().qProfileEditUsersDao().exists(dbSession, anotherProfile, user)).isTrue();
  }

  @Test
  public void remove_from_organization_groups() {
    OrganizationDto anotherOrganization = db.organizations().insert();
    UserDto anotherUser = db.users().insertUser();
    GroupDto group = db.users().insertGroup(organization);
    GroupDto anotherGroup = db.users().insertGroup(anotherOrganization);
    db.users().insertMembers(group, user, anotherUser);
    db.users().insertMembers(anotherGroup, user, anotherUser);

    call(organization.getKey(), user.getLogin());

    assertThat(dbClient.groupMembershipDao().selectGroupIdsByUserId(dbSession, user.getId()))
      .containsOnly(anotherGroup.getId());
    assertThat(dbClient.groupMembershipDao().selectGroupIdsByUserId(dbSession, anotherUser.getId()))
      .containsOnly(group.getId(), anotherGroup.getId());
  }

  @Test
  public void remove_from_default_organization_group() {
    GroupDto defaultGroup = db.users().insertDefaultGroup(organization, "default");
    db.users().insertMember(defaultGroup, user);

    call(organization.getKey(), user.getLogin());

    assertThat(dbClient.groupMembershipDao().selectGroupIdsByUserId(dbSession, user.getId())).isEmpty();
  }

  @Test
  public void remove_from_org_properties() {
    OrganizationDto anotherOrganization = db.organizations().insert();
    ComponentDto anotherProject = db.components().insertPrivateProject(anotherOrganization);
    UserDto anotherUser = db.users().insertUser();
    insertProperty("KEY_11", "VALUE", project.getId(), user.getId());
    insertProperty("KEY_12", "VALUE", project.getId(), user.getId());
    insertProperty("KEY_11", "VALUE", project.getId(), anotherUser.getId());
    insertProperty("KEY_11", "VALUE", anotherProject.getId(), user.getId());

    call(organization.getKey(), user.getLogin());

    assertThat(dbClient.propertiesDao().selectByQuery(PropertyQuery.builder().setComponentId(project.getId()).build(), dbSession))
      .hasSize(1).extracting(PropertyDto::getUserId).containsOnly(anotherUser.getId());
    assertThat(dbClient.propertiesDao().selectByQuery(PropertyQuery.builder().setComponentId(anotherProject.getId()).build(), dbSession)).extracting(PropertyDto::getUserId)
      .hasSize(1).containsOnly(user.getId());
  }

  @Test
  public void remove_from_default_assignee_properties() {
    OrganizationDto anotherOrganization = db.organizations().insert();
    ComponentDto anotherProject = db.components().insertPrivateProject(anotherOrganization);
    UserDto anotherUser = db.users().insertUser();
    insertProperty(DEFAULT_ISSUE_ASSIGNEE, user.getLogin(), project.getId(), null);
    insertProperty("ANOTHER_KEY", user.getLogin(), project.getId(), null);
    insertProperty(DEFAULT_ISSUE_ASSIGNEE, anotherUser.getLogin(), project.getId(), null);
    insertProperty(DEFAULT_ISSUE_ASSIGNEE, user.getLogin(), anotherProject.getId(), null);

    call(organization.getKey(), user.getLogin());

    assertThat(dbClient.propertiesDao().selectByQuery(PropertyQuery.builder().setComponentId(project.getId()).build(), dbSession))
      .hasSize(2).extracting(PropertyDto::getKey, PropertyDto::getValue)
      .containsOnly(tuple("ANOTHER_KEY", user.getLogin()), tuple(DEFAULT_ISSUE_ASSIGNEE, anotherUser.getLogin()));
    assertThat(dbClient.propertiesDao().selectByQuery(PropertyQuery.builder().setComponentId(anotherProject.getId()).build(), dbSession)).extracting(PropertyDto::getValue)
      .hasSize(1).containsOnly(user.getLogin());
  }

  @Test
  public void user_is_removed_only_from_designated_organization() {
    OrganizationDto anotherOrg = db.organizations().insert();
    db.organizations().addMember(anotherOrg, user);

    call(organization.getKey(), user.getLogin());

    assertMember(anotherOrg.getUuid(), user);
  }

  @Test
  public void remove_member_as_organization_admin() {
    userSession.logIn().addPermission(ADMINISTER, organization);

    call(organization.getKey(), user.getLogin());

    assertNotAMember(organization.getUuid(), user);
  }

  @Test
  public void do_not_fail_if_user_already_removed_from_organization() {
    call(organization.getKey(), user.getLogin());

    call(organization.getKey(), user.getLogin());
  }

  @Test
  public void fail_if_login_does_not_exist() {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("User 'login-42' is not found");

    call(organization.getKey(), "login-42");
  }

  @Test
  public void fail_if_organization_does_not_exist() {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Organization 'org-42' is not found");

    call("org-42", user.getLogin());
  }

  @Test
  public void fail_if_no_login_provided() {
    expectedException.expect(IllegalArgumentException.class);

    call(organization.getKey(), null);
  }

  @Test
  public void fail_if_no_organization_provided() {
    expectedException.expect(IllegalArgumentException.class);

    call(null, user.getLogin());
  }

  @Test
  public void fail_if_insufficient_permissions() {
    userSession.logIn().addPermission(ADMINISTER_QUALITY_GATES, organization);

    expectedException.expect(ForbiddenException.class);

    call(organization.getKey(), user.getLogin());
  }

  @Test
  public void remove_org_admin_is_allowed_when_another_org_admin_exists() {
    OrganizationDto anotherOrganization = db.organizations().insert();
    UserDto admin1 = db.users().insertAdminByUserPermission(anotherOrganization);
    db.organizations().addMember(anotherOrganization, admin1);
    UserDto admin2 = db.users().insertAdminByUserPermission(anotherOrganization);
    db.organizations().addMember(anotherOrganization, admin2);
    userIndexer.commitAndIndex(db.getSession(), asList(admin1, admin2));

    call(anotherOrganization.getKey(), admin1.getLogin());

    assertNotAMember(anotherOrganization.getUuid(), admin1);
    assertMember(anotherOrganization.getUuid(), admin2);
  }

  @Test
  public void fail_to_remove_last_organization_admin() {
    OrganizationDto anotherOrganization = db.organizations().insert();
    UserDto admin = db.users().insertAdminByUserPermission(anotherOrganization);
    db.organizations().addMember(anotherOrganization, admin);
    UserDto user = db.users().insertUser();
    db.organizations().addMember(anotherOrganization, user);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("The last administrator member cannot be removed");

    call(anotherOrganization.getKey(), admin.getLogin());
  }

  private TestResponse call(@Nullable String organizationKey, @Nullable String login) {
    TestRequest request = ws.newRequest();
    setNullable(organizationKey, o -> request.setParam(PARAM_ORGANIZATION, o));
    setNullable(login, l -> request.setParam("login", l));

    return request.execute();
  }

  private void assertNotAMember(String organizationUuid, UserDto user) {
    assertThat(dbClient.organizationMemberDao().select(dbSession, organizationUuid, user.getId())).isNotPresent();
    assertThat(userIndex.search(UserQuery.builder().setOrganizationUuid(organizationUuid).setTextQuery(user.getLogin()).build(), new SearchOptions()).getDocs()).isEmpty();
  }

  private void assertMember(String organizationUuid, UserDto user) {
    assertThat(dbClient.organizationMemberDao().select(dbSession, organizationUuid, user.getId())).isPresent();
    assertThat(userIndex.getNullableByLogin(user.getLogin()).organizationUuids()).contains(organizationUuid);
  }

  private void assertOrgPermissionsOfUser(UserDto user, OrganizationDto organization, OrganizationPermission... permissions) {
    assertThat(dbClient.userPermissionDao().selectGlobalPermissionsOfUser(dbSession, user.getId(), organization.getUuid()).stream()
      .map(OrganizationPermission::fromKey))
        .containsOnly(permissions);
  }

  private void assertProjectPermissionsOfUser(UserDto user, ComponentDto project, String... permissions) {
    assertThat(dbClient.userPermissionDao().selectProjectPermissionsOfUser(dbSession, user.getId(), project.getId())).containsOnly(permissions);
  }

  private void insertProperty(String key, @Nullable String value, @Nullable Long resourceId, @Nullable Integer userId) {
    PropertyDto dto = new PropertyDto().setKey(key)
      .setResourceId(resourceId)
      .setUserId(userId)
      .setValue(value);
    db.properties().insertProperty(dto);
  }
}
