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

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.sonar.api.config.internal.MapSettings;
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
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.OrganizationFlags;
import org.sonar.server.organization.OrganizationFlagsImpl;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.rule.index.RuleIndexDefinition;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.usergroups.DefaultGroupCreatorImpl;
import org.sonar.server.usergroups.DefaultGroupFinder;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;

public class EnableSupportActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester dbTester = DbTester.create();
  @Rule
  public EsTester esTester = new EsTester(new RuleIndexDefinition(new MapSettings().asConfig()));

  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(dbTester);
  private OrganizationFlags organizationFlags = new OrganizationFlagsImpl(dbTester.getDbClient());
  private RuleIndexer ruleIndexer = spy(new RuleIndexer(esTester.client(), dbTester.getDbClient()));
  private EnableSupportAction underTest = new EnableSupportAction(userSession, dbTester.getDbClient(), defaultOrganizationProvider, organizationFlags,
    new DefaultGroupCreatorImpl(dbTester.getDbClient()), new DefaultGroupFinder(dbTester.getDbClient()), ruleIndexer);
  private WsActionTester tester = new WsActionTester(underTest);

  @Test
  public void enabling_support_saves_internal_property_and_flags_caller_as_root() {
    UserDto user = dbTester.users().insertUser();
    UserDto otherUser = dbTester.users().insertUser();
    dbTester.users().insertDefaultGroup(dbTester.getDefaultOrganization(), "sonar-users");
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
  public void enabling_support_creates_default_members_group_and_associate_org_members() {
    OrganizationDto defaultOrganization = dbTester.getDefaultOrganization();
    OrganizationDto anotherOrganization = dbTester.organizations().insert();
    UserDto user1 = dbTester.users().insertUser();
    UserDto user2 = dbTester.users().insertUser();
    UserDto userInAnotherOrganization = dbTester.users().insertUser();
    dbTester.organizations().addMember(defaultOrganization, user1);
    dbTester.organizations().addMember(defaultOrganization, user2);
    dbTester.organizations().addMember(anotherOrganization, userInAnotherOrganization);
    dbTester.users().insertDefaultGroup(dbTester.getDefaultOrganization(), "sonar-users");
    logInAsSystemAdministrator(user1.getLogin());

    call();

    Optional<Integer> defaultGroupId = dbTester.getDbClient().organizationDao().getDefaultGroupId(dbTester.getSession(), defaultOrganization.getUuid());
    assertThat(defaultGroupId).isPresent();
    GroupDto membersGroup = dbTester.getDbClient().groupDao().selectById(dbTester.getSession(), defaultGroupId.get());
    assertThat(membersGroup).isNotNull();
    assertThat(membersGroup.getName()).isEqualTo("Members");
    assertThat(dbTester.getDbClient().groupMembershipDao().selectGroupIdsByUserId(dbTester.getSession(), user1.getId())).containsOnly(defaultGroupId.get());
    assertThat(dbTester.getDbClient().groupMembershipDao().selectGroupIdsByUserId(dbTester.getSession(), user2.getId())).containsOnly(defaultGroupId.get());
    assertThat(dbTester.getDbClient().groupMembershipDao().selectGroupIdsByUserId(dbTester.getSession(), userInAnotherOrganization.getId())).isEmpty();
  }

  @Test
  public void enabling_support_copy_sonar_users_permissions_to_members_group() {
    OrganizationDto defaultOrganization = dbTester.getDefaultOrganization();
    UserDto user = dbTester.users().insertUser();
    GroupDto sonarUsersGroup = dbTester.users().insertDefaultGroup(defaultOrganization, "sonar-users");
    ComponentDto project = dbTester.components().insertPrivateProject(defaultOrganization);
    dbTester.users().insertPermissionOnGroup(sonarUsersGroup, "user");
    dbTester.users().insertProjectPermissionOnGroup(sonarUsersGroup, "codeviewer", project);
    // Should be ignored
    GroupDto anotherGroup = dbTester.users().insertGroup();
    dbTester.users().insertPermissionOnGroup(anotherGroup, "admin");
    logInAsSystemAdministrator(user.getLogin());

    call();

    int defaultGroupId = dbTester.getDbClient().organizationDao().getDefaultGroupId(dbTester.getSession(), defaultOrganization.getUuid()).get();
    assertThat(defaultGroupId).isNotEqualTo(sonarUsersGroup.getId());
    List<GroupPermissionDto> result = new ArrayList<>();
    dbTester.getDbClient().groupPermissionDao().selectAllPermissionsByGroupId(dbTester.getSession(), defaultOrganization.getUuid(), defaultGroupId,
      context -> result.add((GroupPermissionDto) context.getResultObject()));
    assertThat(result).extracting(GroupPermissionDto::getResourceId, GroupPermissionDto::getRole).containsOnly(
      tuple(null, "user"), tuple(project.getId(), "codeviewer"));
  }

  @Test
  public void enabling_support_copy_sonar_users_permission_templates_to_members_group() {
    OrganizationDto defaultOrganization = dbTester.getDefaultOrganization();
    UserDto user = dbTester.users().insertUser();
    GroupDto sonarUsersGroup = dbTester.users().insertDefaultGroup(defaultOrganization, "sonar-users");
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate(dbTester.getDefaultOrganization());
    dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, sonarUsersGroup, "user");
    dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, sonarUsersGroup, "admin");
    // Should be ignored
    GroupDto otherGroup = dbTester.users().insertGroup();
    dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, otherGroup, "user");
    logInAsSystemAdministrator(user.getLogin());

    call();

    int defaultGroupId = dbTester.getDbClient().organizationDao().getDefaultGroupId(dbTester.getSession(), defaultOrganization.getUuid()).get();
    assertThat(dbTester.getDbClient().permissionTemplateDao().selectAllGroupPermissionTemplatesByGroupId(dbTester.getSession(), defaultGroupId))
      .extracting(PermissionTemplateGroupDto::getGroupId, PermissionTemplateGroupDto::getPermission)
      .containsOnly(tuple(defaultGroupId, "user"), tuple(defaultGroupId, "admin"));
  }

  @Test
  public void enabling_organizations_should_remove_template_rule_and_custom_rule() {
    RuleDefinitionDto normal = dbTester.rules().insert();
    RuleDefinitionDto template = dbTester.rules().insert(r -> r.setIsTemplate(true));
    RuleDefinitionDto custom = dbTester.rules().insert(r -> r.setTemplateId(template.getId()));

    UserDto user = dbTester.users().insertUser();
    dbTester.users().insertDefaultGroup(dbTester.getDefaultOrganization(), "sonar-users");
    logInAsSystemAdministrator(user.getLogin());

    assertThat(dbTester.getDbClient().ruleDao().selectAllDefinitions(dbTester.getSession()))
      .extracting(RuleDefinitionDto::getKey, RuleDefinitionDto::getStatus)
      .containsExactlyInAnyOrder(
        tuple(normal.getKey(), RuleStatus.READY),
        tuple(template.getKey(), RuleStatus.READY),
        tuple(custom.getKey(), RuleStatus.READY));

    call();

    assertThat(dbTester.getDbClient().ruleDao().selectAllDefinitions(dbTester.getSession()))
      .extracting(RuleDefinitionDto::getKey, RuleDefinitionDto::getStatus)
      .containsExactlyInAnyOrder(
        tuple(normal.getKey(), RuleStatus.READY),
        tuple(template.getKey(), RuleStatus.REMOVED),
        tuple(custom.getKey(), RuleStatus.REMOVED));

    @SuppressWarnings("unchecked")
    Class<ArrayList<Integer>> listClass = (Class<ArrayList<Integer>>) (Class) ArrayList.class;
    ArgumentCaptor<ArrayList<Integer>> indexedRuleKeys = ArgumentCaptor.forClass(listClass);
    verify(ruleIndexer).commitAndIndex(any(), indexedRuleKeys.capture());
    assertThat(indexedRuleKeys.getValue()).containsExactlyInAnyOrder(template.getId(), custom.getId());
  }

  @Test
  public void throw_IAE_when_members_group_already_exists() {
    UserDto user = dbTester.users().insertUser();
    dbTester.users().insertDefaultGroup(dbTester.getDefaultOrganization(), "sonar-users");
    dbTester.users().insertGroup(dbTester.getDefaultOrganization(), "Members");
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
    UserDto user = dbTester.users().insertUser();
    logInAsSystemAdministrator(user.getLogin());

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage(String.format("Default group cannot be found on organization '%s'", defaultOrganizationProvider.get().getUuid()));

    call();
  }

  @Test
  public void do_nothing_if_support_is_already_enabled() {
    dbTester.users().insertDefaultGroup(dbTester.getDefaultOrganization(), "sonar-users");
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
    userSession.logIn(login).addPermission(ADMINISTER, dbTester.getDefaultOrganization());
  }

  private void call() {
    TestResponse response = tester.newRequest().setMethod("POST").execute();
    assertThat(response.getStatus()).isEqualTo(HttpURLConnection.HTTP_NO_CONTENT);
  }

  private void verifyFeatureEnabled(boolean enabled) {
    assertThat(organizationFlags.isEnabled(dbTester.getSession())).isEqualTo(enabled);
  }

  private void verifyRoot(UserDto user, boolean root) {
    dbTester.rootFlag().verify(user.getLogin(), root);
  }
}
