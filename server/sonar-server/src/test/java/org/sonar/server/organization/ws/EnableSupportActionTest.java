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
package org.sonar.server.organization.ws;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.GroupPermissionDto;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.permission.template.PermissionTemplateGroupDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.OrganizationFlags;
import org.sonar.server.organization.OrganizationFlagsImpl;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.usergroups.DefaultGroupCreatorImpl;
import org.sonar.server.usergroups.DefaultGroupFinder;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;

public class EnableSupportActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
  private OrganizationFlags organizationFlags = new OrganizationFlagsImpl(db.getDbClient());
  private RuleIndexer ruleIndexer = mock(RuleIndexer.class);
  private EnableSupportAction underTest = new EnableSupportAction(userSession, db.getDbClient(), defaultOrganizationProvider, organizationFlags,
    new DefaultGroupCreatorImpl(db.getDbClient()), new DefaultGroupFinder(db.getDbClient()), ruleIndexer);
  private WsActionTester tester = new WsActionTester(underTest);

  @Test
  public void enabling_support_saves_internal_property_and_flags_caller_as_root() {
    UserDto user = db.users().insertUser();
    UserDto otherUser = db.users().insertUser();
    db.users().insertDefaultGroup(db.getDefaultOrganization(), "sonar-users");
    verifyFeatureEnabled(false);
    verifyRoot(user, false);
    verifyRoot(otherUser, false);
    logInAsSystemAdministrator(user.getLogin());

    call();

    verifyFeatureEnabled(true);
    verifyRoot(user, true);
    verifyRoot(otherUser, false);
  }

  @Test
  public void enabling_support_creates_default_members_group_and_associate_org_members() throws Exception {
    OrganizationDto defaultOrganization = db.getDefaultOrganization();
    OrganizationDto anotherOrganization = db.organizations().insert();
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    UserDto userInAnotherOrganization = db.users().insertUser();
    db.organizations().addMember(defaultOrganization, user1);
    db.organizations().addMember(defaultOrganization, user2);
    db.organizations().addMember(anotherOrganization, userInAnotherOrganization);
    db.users().insertDefaultGroup(db.getDefaultOrganization(), "sonar-users");
    logInAsSystemAdministrator(user1.getLogin());

    call();

    Optional<Integer> defaultGroupId = db.getDbClient().organizationDao().getDefaultGroupId(db.getSession(), defaultOrganization.getUuid());
    assertThat(defaultGroupId).isPresent();
    GroupDto membersGroup = db.getDbClient().groupDao().selectById(db.getSession(), defaultGroupId.get());
    assertThat(membersGroup).isNotNull();
    assertThat(membersGroup.getName()).isEqualTo("Members");
    assertThat(db.getDbClient().groupMembershipDao().selectGroupIdsByUserId(db.getSession(), user1.getId())).containsOnly(defaultGroupId.get());
    assertThat(db.getDbClient().groupMembershipDao().selectGroupIdsByUserId(db.getSession(), user2.getId())).containsOnly(defaultGroupId.get());
    assertThat(db.getDbClient().groupMembershipDao().selectGroupIdsByUserId(db.getSession(), userInAnotherOrganization.getId())).isEmpty();
  }

  @Test
  public void enabling_support_copy_sonar_users_permissions_to_members_group() throws Exception {
    OrganizationDto defaultOrganization = db.getDefaultOrganization();
    UserDto user = db.users().insertUser();
    GroupDto sonarUsersGroup = db.users().insertDefaultGroup(defaultOrganization, "sonar-users");
    ComponentDto project = db.components().insertPrivateProject(defaultOrganization);
    db.users().insertPermissionOnGroup(sonarUsersGroup, "user");
    db.users().insertProjectPermissionOnGroup(sonarUsersGroup, "codeviewer", project);
    // Should be ignored
    GroupDto anotherGroup = db.users().insertGroup();
    db.users().insertPermissionOnGroup(anotherGroup, "admin");
    logInAsSystemAdministrator(user.getLogin());

    call();

    int defaultGroupId = db.getDbClient().organizationDao().getDefaultGroupId(db.getSession(), defaultOrganization.getUuid()).get();
    assertThat(defaultGroupId).isNotEqualTo(sonarUsersGroup.getId());
    List<GroupPermissionDto> result = new ArrayList<>();
    db.getDbClient().groupPermissionDao().selectAllPermissionsByGroupId(db.getSession(), defaultOrganization.getUuid(), defaultGroupId,
      context -> result.add((GroupPermissionDto) context.getResultObject()));
    assertThat(result).extracting(GroupPermissionDto::getResourceId, GroupPermissionDto::getRole).containsOnly(
      tuple(null, "user"), tuple(project.getId(), "codeviewer"));
  }

  @Test
  public void enabling_support_copy_sonar_users_permission_templates_to_members_group() throws Exception {
    OrganizationDto defaultOrganization = db.getDefaultOrganization();
    UserDto user = db.users().insertUser();
    GroupDto sonarUsersGroup = db.users().insertDefaultGroup(defaultOrganization, "sonar-users");
    PermissionTemplateDto permissionTemplate = db.permissionTemplates().insertTemplate(db.getDefaultOrganization());
    db.permissionTemplates().addGroupToTemplate(permissionTemplate, sonarUsersGroup, "user");
    db.permissionTemplates().addGroupToTemplate(permissionTemplate, sonarUsersGroup, "admin");
    // Should be ignored
    GroupDto otherGroup = db.users().insertGroup();
    db.permissionTemplates().addGroupToTemplate(permissionTemplate, otherGroup, "user");
    logInAsSystemAdministrator(user.getLogin());

    call();

    int defaultGroupId = db.getDbClient().organizationDao().getDefaultGroupId(db.getSession(), defaultOrganization.getUuid()).get();
    assertThat(db.getDbClient().permissionTemplateDao().selectAllGroupPermissionTemplatesByGroupId(db.getSession(), defaultGroupId))
      .extracting(PermissionTemplateGroupDto::getGroupId, PermissionTemplateGroupDto::getPermission)
      .containsOnly(tuple(defaultGroupId, "user"), tuple(defaultGroupId, "admin"));
  }

  @Test
  public void enabling_organizations_should_remove_template_rule_and_custom_rule() {
    RuleDefinitionDto normal = db.rules().insert();
    RuleDefinitionDto template = db.rules().insert(r -> r.setIsTemplate(true));
    RuleDefinitionDto custom = db.rules().insert(r -> r.setTemplateId(template.getId()));

    UserDto user = db.users().insertUser();
    db.users().insertDefaultGroup(db.getDefaultOrganization(), "sonar-users");
    logInAsSystemAdministrator(user.getLogin());

    assertThat(db.getDbClient().ruleDao().selectAllDefinitions(db.getSession()))
      .extracting(RuleDefinitionDto::getKey, RuleDefinitionDto::getStatus)
      .containsExactlyInAnyOrder(
        tuple(normal.getKey(), RuleStatus.READY),
        tuple(template.getKey(), RuleStatus.READY),
        tuple(custom.getKey(), RuleStatus.READY)
      );

    call();

    assertThat(db.getDbClient().ruleDao().selectAllDefinitions(db.getSession()))
      .extracting(RuleDefinitionDto::getKey, RuleDefinitionDto::getStatus)
      .containsExactlyInAnyOrder(
        tuple(normal.getKey(), RuleStatus.READY),
        tuple(template.getKey(), RuleStatus.REMOVED),
        tuple(custom.getKey(), RuleStatus.REMOVED)
      );

    @SuppressWarnings("unchecked")
    Class<ArrayList<RuleKey>> listClass = (Class<ArrayList<RuleKey>>)(Class)ArrayList.class;
    ArgumentCaptor<ArrayList<RuleKey>> indexedRuleKeys = ArgumentCaptor.forClass(listClass);
    verify(ruleIndexer).indexRuleDefinitions(indexedRuleKeys.capture());
    assertThat(indexedRuleKeys.getValue()).containsExactlyInAnyOrder(template.getKey(), custom.getKey());
  }

  @Test
  public void throw_IAE_when_members_group_already_exists() throws Exception {
    UserDto user = db.users().insertUser();
    db.users().insertDefaultGroup(db.getDefaultOrganization(), "sonar-users");
    db.users().insertGroup(db.getDefaultOrganization(), "Members");
    logInAsSystemAdministrator(user.getLogin());

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The group 'Members' already exist");

    call();
  }

  @Test
  public void throw_UnauthorizedException_if_not_logged_in() {
    userSession.anonymous();

    expectedException.expect(UnauthorizedException.class);
    expectedException.expectMessage("Authentication is required");

    call();
  }

  @Test
  public void throw_ForbiddenException_if_not_system_administrator() {
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    call();
  }

  @Test
  public void throw_ISE_when_default_organization_has_not_default_group() {
    UserDto user = db.users().insertUser();
    logInAsSystemAdministrator(user.getLogin());

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage(String.format("Default group cannot be found on organization '%s'", defaultOrganizationProvider.get().getUuid()));

    call();
  }

  @Test
  public void do_nothing_if_support_is_already_enabled() {
    db.users().insertDefaultGroup(db.getDefaultOrganization(), "sonar-users");
    logInAsSystemAdministrator("foo");

    call();
    verifyFeatureEnabled(true);

    // the test could be improved to verify that
    // the caller user is not flagged as root
    // if he was not already root
    call();
    verifyFeatureEnabled(true);
  }

  @Test
  public void test_definition() {
    WebService.Action def = tester.getDef();
    assertThat(def.key()).isEqualTo("enable_support");
    assertThat(def.isPost()).isTrue();
    assertThat(def.isInternal()).isTrue();
    assertThat(def.params()).isEmpty();
  }

  private void logInAsSystemAdministrator(String login) {
    userSession.logIn(login).addPermission(ADMINISTER, db.getDefaultOrganization());
  }

  private void call() {
    TestResponse response = tester.newRequest().setMethod("POST").execute();
    assertThat(response.getStatus()).isEqualTo(HttpURLConnection.HTTP_NO_CONTENT);
  }

  private void verifyFeatureEnabled(boolean enabled) {
    assertThat(organizationFlags.isEnabled(db.getSession())).isEqualTo(enabled);
  }

  private void verifyRoot(UserDto user, boolean root) {
    db.rootFlag().verify(user.getLogin(), root);
  }
}
