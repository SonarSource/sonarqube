/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.organization;

import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import javax.annotation.Nullable;
import org.assertj.core.groups.Tuple;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.alm.AlmAppInstallDto;
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
import org.sonar.server.user.index.UserDoc;
import org.sonar.server.user.index.UserIndex;
import org.sonar.server.user.index.UserIndexDefinition;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.user.index.UserQuery;
import org.sonar.server.usergroups.DefaultGroupFinder;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.sonar.api.CoreProperties.DEFAULT_ISSUE_ASSIGNEE;
import static org.sonar.api.web.UserRole.CODEVIEWER;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.alm.ALM.GITHUB;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
import static org.sonar.db.permission.OrganizationPermission.SCAN;
import static org.sonar.server.user.index.UserIndexDefinition.FIELD_ORGANIZATION_UUIDS;
import static org.sonar.server.user.index.UserIndexDefinition.FIELD_UUID;

public class MemberUpdaterTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public DbTester db = DbTester.create();

  private DbClient dbClient = db.getDbClient();
  private UserIndex userIndex = new UserIndex(es.client(), System2.INSTANCE);
  private UserIndexer userIndexer = new UserIndexer(dbClient, es.client());

  private MemberUpdater underTest = new MemberUpdater(dbClient, new DefaultGroupFinder(dbClient), userIndexer);

  @Test
  public void add_member_in_db_and_user_index() {
    OrganizationDto organization = db.organizations().insert();
    db.users().insertDefaultGroup(organization, "Members");
    UserDto user = db.users().insertUser();

    underTest.addMember(db.getSession(), organization, user);

    db.organizations().assertUserIsMemberOfOrganization(organization, user);
    assertThat(userIndex.search(UserQuery.builder().build(), new SearchOptions()).getDocs())
      .extracting(UserDoc::login, UserDoc::organizationUuids)
      .containsExactlyInAnyOrder(tuple(user.getLogin(), singletonList(organization.getUuid())));
  }

  @Test
  public void does_not_fail_to_add_member_if_user_already_added_in_organization() {
    OrganizationDto organization = db.organizations().insert();
    GroupDto defaultGroup = db.users().insertDefaultGroup(organization, "Members");
    UserDto user = db.users().insertUser();
    db.organizations().addMember(organization, user);
    db.users().insertMember(defaultGroup, user);
    db.organizations().assertUserIsMemberOfOrganization(organization, user);

    underTest.addMember(db.getSession(), organization, user);

    db.organizations().assertUserIsMemberOfOrganization(organization, user);
  }

  @Test
  public void add_member_fails_when_organization_has_no_default_group() {
    OrganizationDto organization = db.organizations().insert();
    UserDto user = db.users().insertUser();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage(format("Default group cannot be found on organization '%s'", organization.getUuid()));

    underTest.addMember(db.getSession(), organization, user);
  }

  @Test
  public void add_members_in_db_and_user_index() {
    OrganizationDto organization = db.organizations().insert();
    db.users().insertDefaultGroup(organization, "Members");
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    UserDto disableUser = db.users().insertDisabledUser();

    underTest.addMembers(db.getSession(), organization, asList(user1, user2, disableUser));

    db.organizations().assertUserIsMemberOfOrganization(organization, user1);
    db.organizations().assertUserIsMemberOfOrganization(organization, user2);
    assertUserIsNotMember(organization, disableUser);
    assertThat(userIndex.search(UserQuery.builder().build(), new SearchOptions()).getDocs())
      .extracting(UserDoc::login, UserDoc::organizationUuids)
      .containsExactlyInAnyOrder(
        tuple(user1.getLogin(), singletonList(organization.getUuid())),
        tuple(user2.getLogin(), singletonList(organization.getUuid())));
  }

  @Test
  public void add_members_does_not_fail_when_one_user_is_already_member_of_organization() {
    OrganizationDto organization = db.organizations().insert();
    GroupDto defaultGroup = db.users().insertDefaultGroup(organization, "Members");
    UserDto userAlreadyMember = db.users().insertUser();
    db.organizations().addMember(organization, userAlreadyMember);
    db.users().insertMember(defaultGroup, userAlreadyMember);
    UserDto userNotMember = db.users().insertUser();
    userIndexer.indexOnStartup(new HashSet<>());

    underTest.addMembers(db.getSession(), organization, asList(userAlreadyMember, userNotMember));

    db.organizations().assertUserIsMemberOfOrganization(organization, userAlreadyMember);
    db.organizations().assertUserIsMemberOfOrganization(organization, userNotMember);
    assertThat(userIndex.search(UserQuery.builder().build(), new SearchOptions()).getDocs())
      .extracting(UserDoc::login, UserDoc::organizationUuids)
      .containsExactlyInAnyOrder(
        tuple(userAlreadyMember.getLogin(), singletonList(organization.getUuid())),
        tuple(userNotMember.getLogin(), singletonList(organization.getUuid())));
  }

  @Test
  public void remove_member_from_db_and_user_index() {
    OrganizationDto organization = db.organizations().insert();
    GroupDto defaultGroup = db.users().insertDefaultGroup(organization, "Members");
    UserDto user = db.users().insertUser();
    UserDto adminUser = db.users().insertAdminByUserPermission(organization);
    db.organizations().addMember(organization, user, adminUser);
    db.users().insertMember(defaultGroup, user);
    userIndexer.indexOnStartup(new HashSet<>());

    underTest.removeMember(db.getSession(), organization, user);

    assertUserIsNotMember(organization, user);
  }

  @Test
  public void remove_members_from_db_and_user_index() {
    OrganizationDto organization = db.organizations().insert();
    GroupDto defaultGroup = db.users().insertDefaultGroup(organization, "Members");
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    UserDto adminUser = db.users().insertAdminByUserPermission(organization);
    db.organizations().addMember(organization, user1, user2, adminUser);
    db.users().insertMember(defaultGroup, user1);
    db.users().insertMember(defaultGroup, user2);
    db.users().insertMember(defaultGroup, adminUser);
    userIndexer.indexOnStartup(new HashSet<>());

    underTest.removeMembers(db.getSession(), organization, asList(user1, user2));

    assertUserIsNotMember(organization, user1);
    assertUserIsNotMember(organization, user2);
    db.organizations().assertUserIsMemberOfOrganization(organization, adminUser);
  }

  @Test
  public void remove_member_removes_permissions() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertPrivateProject(organization);
    GroupDto defaultGroup = db.users().insertDefaultGroup(organization, "Members");
    UserDto user = db.users().insertUser();
    UserDto adminUser = db.users().insertAdminByUserPermission(organization);
    db.organizations().addMember(organization, user, adminUser);
    db.users().insertMember(defaultGroup, user);
    UserDto anotherUser = db.users().insertUser();
    OrganizationDto anotherOrganization = db.organizations().insert();
    ComponentDto anotherProject = db.components().insertPrivateProject(anotherOrganization);
    userIndexer.indexOnStartup(new HashSet<>());

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

    underTest.removeMember(db.getSession(), organization, user);

    assertUserIsNotMember(organization, user);
    assertOrgPermissionsOfUser(user, organization);
    assertOrgPermissionsOfUser(user, anotherOrganization, ADMINISTER, SCAN);
    assertOrgPermissionsOfUser(anotherUser, organization, ADMINISTER, SCAN);
    assertProjectPermissionsOfUser(user, project);
    assertProjectPermissionsOfUser(user, anotherProject, CODEVIEWER, USER);
    assertProjectPermissionsOfUser(anotherUser, project, CODEVIEWER, USER);
  }

  @Test
  public void remove_member_removes_template_permissions() {
    OrganizationDto organization = db.organizations().insert();
    GroupDto defaultGroup = db.users().insertDefaultGroup(organization, "Members");
    UserDto user = db.users().insertUser();
    UserDto adminUser = db.users().insertAdminByUserPermission(organization);
    db.organizations().addMember(organization, user, adminUser);
    db.users().insertMember(defaultGroup, user);
    userIndexer.indexOnStartup(new HashSet<>());

    OrganizationDto anotherOrganization = db.organizations().insert();
    UserDto anotherUser = db.users().insertUser();
    PermissionTemplateDto template = db.permissionTemplates().insertTemplate(organization);
    PermissionTemplateDto anotherTemplate = db.permissionTemplates().insertTemplate(anotherOrganization);
    String permission = "browse";
    db.permissionTemplates().addUserToTemplate(template.getId(), user.getId(), permission);
    db.permissionTemplates().addUserToTemplate(template.getId(), anotherUser.getId(), permission);
    db.permissionTemplates().addUserToTemplate(anotherTemplate.getId(), user.getId(), permission);

    underTest.removeMember(db.getSession(), organization, user);

    assertThat(dbClient.permissionTemplateDao().selectUserPermissionsByTemplateId(db.getSession(), template.getId())).extracting(PermissionTemplateUserDto::getUserId)
      .containsOnly(anotherUser.getId());
    assertThat(dbClient.permissionTemplateDao().selectUserPermissionsByTemplateId(db.getSession(), anotherTemplate.getId())).extracting(PermissionTemplateUserDto::getUserId)
      .containsOnly(user.getId());
  }

  @Test
  public void remove_member_removes_qprofiles_user_permission() {
    OrganizationDto organization = db.organizations().insert();
    GroupDto defaultGroup = db.users().insertDefaultGroup(organization, "Members");
    UserDto user = db.users().insertUser();
    UserDto adminUser = db.users().insertAdminByUserPermission(organization);
    db.organizations().addMember(organization, user, adminUser);
    db.users().insertMember(defaultGroup, user);
    userIndexer.indexOnStartup(new HashSet<>());

    OrganizationDto anotherOrganization = db.organizations().insert();
    db.organizations().addMember(anotherOrganization, user);
    QProfileDto profile = db.qualityProfiles().insert(organization);
    QProfileDto anotherProfile = db.qualityProfiles().insert(anotherOrganization);
    db.qualityProfiles().addUserPermission(profile, user);
    db.qualityProfiles().addUserPermission(anotherProfile, user);

    underTest.removeMember(db.getSession(), organization, user);

    assertThat(db.getDbClient().qProfileEditUsersDao().exists(db.getSession(), profile, user)).isFalse();
    assertThat(db.getDbClient().qProfileEditUsersDao().exists(db.getSession(), anotherProfile, user)).isTrue();
  }

  @Test
  public void remove_member_removes_user_from_organization_groups() {
    OrganizationDto organization = db.organizations().insert();
    GroupDto defaultGroup = db.users().insertDefaultGroup(organization, "Members");
    UserDto user = db.users().insertUser();
    UserDto adminUser = db.users().insertAdminByUserPermission(organization);
    db.organizations().addMember(organization, user, adminUser);
    db.users().insertMember(defaultGroup, user);
    userIndexer.indexOnStartup(new HashSet<>());

    OrganizationDto anotherOrganization = db.organizations().insert();
    UserDto anotherUser = db.users().insertUser();
    GroupDto group = db.users().insertGroup(organization);
    GroupDto anotherGroup = db.users().insertGroup(anotherOrganization);
    db.users().insertMembers(group, user, anotherUser);
    db.users().insertMembers(anotherGroup, user, anotherUser);

    underTest.removeMember(db.getSession(), organization, user);

    assertThat(dbClient.groupMembershipDao().selectGroupIdsByUserId(db.getSession(), user.getId()))
      .containsOnly(anotherGroup.getId());
    assertThat(dbClient.groupMembershipDao().selectGroupIdsByUserId(db.getSession(), anotherUser.getId()))
      .containsOnly(group.getId(), anotherGroup.getId());
  }

  @Test
  public void remove_member_removes_user_from_default_organization_group() {
    OrganizationDto organization = db.organizations().insert();
    GroupDto defaultGroup = db.users().insertDefaultGroup(organization, "Members");
    UserDto user = db.users().insertUser();
    UserDto adminUser = db.users().insertAdminByUserPermission(organization);
    db.organizations().addMember(organization, user, adminUser);
    db.users().insertMember(defaultGroup, user);
    userIndexer.indexOnStartup(new HashSet<>());

    underTest.removeMember(db.getSession(), organization, user);

    assertThat(dbClient.groupMembershipDao().selectGroupIdsByUserId(db.getSession(), user.getId())).isEmpty();
  }

  @Test
  public void remove_member_removes_user_from_org_properties() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertPrivateProject(organization);
    GroupDto defaultGroup = db.users().insertDefaultGroup(organization, "Members");
    UserDto user = db.users().insertUser();
    UserDto adminUser = db.users().insertAdminByUserPermission(organization);
    db.organizations().addMember(organization, user, adminUser);
    db.users().insertMember(defaultGroup, user);
    userIndexer.indexOnStartup(new HashSet<>());

    OrganizationDto anotherOrganization = db.organizations().insert();
    ComponentDto anotherProject = db.components().insertPrivateProject(anotherOrganization);
    UserDto anotherUser = db.users().insertUser();
    insertProperty("KEY_11", "VALUE", project.getId(), user.getId());
    insertProperty("KEY_12", "VALUE", project.getId(), user.getId());
    insertProperty("KEY_11", "VALUE", project.getId(), anotherUser.getId());
    insertProperty("KEY_11", "VALUE", anotherProject.getId(), user.getId());

    underTest.removeMember(db.getSession(), organization, user);

    assertThat(dbClient.propertiesDao().selectByQuery(PropertyQuery.builder().setComponentId(project.getId()).build(), db.getSession()))
      .hasSize(1).extracting(PropertyDto::getUserId).containsOnly(anotherUser.getId());
    assertThat(dbClient.propertiesDao().selectByQuery(PropertyQuery.builder().setComponentId(anotherProject.getId()).build(), db.getSession())).extracting(PropertyDto::getUserId)
      .hasSize(1).containsOnly(user.getId());
  }

  @Test
  public void remove_member_removes_user_from_default_assignee_properties() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertPrivateProject(organization);
    GroupDto defaultGroup = db.users().insertDefaultGroup(organization, "Members");
    UserDto user = db.users().insertUser();
    UserDto adminUser = db.users().insertAdminByUserPermission(organization);
    db.organizations().addMember(organization, user, adminUser);
    db.users().insertMember(defaultGroup, user);
    userIndexer.indexOnStartup(new HashSet<>());

    OrganizationDto anotherOrganization = db.organizations().insert();
    ComponentDto anotherProject = db.components().insertPrivateProject(anotherOrganization);
    UserDto anotherUser = db.users().insertUser();
    insertProperty(DEFAULT_ISSUE_ASSIGNEE, user.getLogin(), project.getId(), null);
    insertProperty("ANOTHER_KEY", user.getLogin(), project.getId(), null);
    insertProperty(DEFAULT_ISSUE_ASSIGNEE, anotherUser.getLogin(), project.getId(), null);
    insertProperty(DEFAULT_ISSUE_ASSIGNEE, user.getLogin(), anotherProject.getId(), null);

    underTest.removeMember(db.getSession(), organization, user);

    assertThat(dbClient.propertiesDao().selectByQuery(PropertyQuery.builder().setComponentId(project.getId()).build(), db.getSession()))
      .hasSize(2).extracting(PropertyDto::getKey, PropertyDto::getValue)
      .containsOnly(Tuple.tuple("ANOTHER_KEY", user.getLogin()), Tuple.tuple(DEFAULT_ISSUE_ASSIGNEE, anotherUser.getLogin()));
    assertThat(dbClient.propertiesDao().selectByQuery(PropertyQuery.builder().setComponentId(anotherProject.getId()).build(), db.getSession())).extracting(PropertyDto::getValue)
      .hasSize(1).containsOnly(user.getLogin());
  }

  @Test
  public void fail_to_remove_members_when_no_more_admin() {
    OrganizationDto organization = db.organizations().insert();
    GroupDto defaultGroup = db.users().insertDefaultGroup(organization, "Members");
    GroupDto adminGroup = db.users().insertGroup(organization);
    db.users().insertPermissionOnGroup(adminGroup, ADMINISTER);
    UserDto user1 = db.users().insertUser();
    UserDto admin1 = db.users().insertAdminByUserPermission(organization);
    UserDto admin2 = db.users().insertUser();
    db.organizations().addMember(organization, user1, admin1, admin2);
    db.users().insertMember(defaultGroup, user1);
    db.users().insertMember(defaultGroup, admin1);
    db.users().insertMember(defaultGroup, admin2);
    db.users().insertMember(adminGroup, admin2);
    userIndexer.indexOnStartup(new HashSet<>());

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The last administrator member cannot be removed");

    underTest.removeMembers(db.getSession(), organization, asList(admin1, admin2));
  }

  @Test
  public void synchronize_user_organization_membership() {
    OrganizationDto organization1 = db.organizations().insert();
    GroupDto org1defaultGroup = db.users().insertDefaultGroup(organization1, "Members");
    AlmAppInstallDto gitHubInstall1 = db.alm().insertAlmAppInstall(a -> a.setAlm(GITHUB));
    db.alm().insertOrganizationAlmBinding(organization1, gitHubInstall1, true);
    OrganizationDto organization2 = db.organizations().insert();
    db.users().insertDefaultGroup(organization2, "Members");
    AlmAppInstallDto gitHubInstall2 = db.alm().insertAlmAppInstall(a -> a.setAlm(GITHUB));
    db.alm().insertOrganizationAlmBinding(organization2, gitHubInstall2, true);
    OrganizationDto organization3 = db.organizations().insert();
    GroupDto org3defaultGroup = db.users().insertDefaultGroup(organization3, "Members");
    AlmAppInstallDto gitHubInstall3 = db.alm().insertAlmAppInstall(a -> a.setAlm(GITHUB));
    db.alm().insertOrganizationAlmBinding(organization3, gitHubInstall3, true);
    // User is member of organization1 and organization3, but organization3 membership will be removed and organization2 membership will be
    // added
    UserDto user = db.users().insertUser();
    db.organizations().addMember(organization1, user);
    db.users().insertMember(org1defaultGroup, user);
    db.organizations().addMember(organization3, user);
    db.users().insertMember(org3defaultGroup, user);

    underTest.synchronizeUserOrganizationMembership(db.getSession(), user, GITHUB, ImmutableSet.of(gitHubInstall1.getOrganizationAlmId(), gitHubInstall2.getOrganizationAlmId()));

    db.organizations().assertUserIsMemberOfOrganization(organization1, user);
    db.organizations().assertUserIsMemberOfOrganization(organization2, user);
    assertUserIsNotMember(organization3, user);
  }

  @Test
  public void synchronize_user_organization_membership_does_not_update_es_index() {
    OrganizationDto organization = db.organizations().insert();
    db.users().insertDefaultGroup(organization, "Members");
    AlmAppInstallDto gitHubInstall = db.alm().insertAlmAppInstall(a -> a.setAlm(GITHUB));
    db.alm().insertOrganizationAlmBinding(organization, gitHubInstall, true);
    UserDto user = db.users().insertUser();

    underTest.synchronizeUserOrganizationMembership(db.getSession(), user, GITHUB, ImmutableSet.of(gitHubInstall.getOrganizationAlmId()));

    assertThat(userIndex.search(UserQuery.builder().build(), new SearchOptions()).getDocs()).isEmpty();
  }

  @Test
  public void synchronize_user_organization_membership_ignores_organization_alm_ids_match_no_existing_organizations() {
    OrganizationDto organization = db.organizations().insert();
    db.users().insertDefaultGroup(organization, "Members");
    AlmAppInstallDto gitHubInstall = db.alm().insertAlmAppInstall(a -> a.setAlm(GITHUB));
    db.alm().insertOrganizationAlmBinding(organization, gitHubInstall, true);
    UserDto user = db.users().insertUser();

    underTest.synchronizeUserOrganizationMembership(db.getSession(), user, GITHUB, ImmutableSet.of("unknown"));

    // User is member of no organization
    assertThat(db.getDbClient().organizationMemberDao().selectOrganizationUuidsByUser(db.getSession(), user.getId())).isEmpty();
  }

  @Test
  public void synchronize_user_organization_membership_ignores_organization_with_member_sync_disabled() {
    OrganizationDto organization = db.organizations().insert();
    db.users().insertDefaultGroup(organization, "Members");
    AlmAppInstallDto gitHubInstall = db.alm().insertAlmAppInstall(a -> a.setAlm(GITHUB));
    db.alm().insertOrganizationAlmBinding(organization, gitHubInstall, false);
    UserDto user = db.users().insertUser();

    underTest.synchronizeUserOrganizationMembership(db.getSession(), user, GITHUB, ImmutableSet.of(gitHubInstall.getOrganizationAlmId()));

    db.organizations().assertUserIsNotMemberOfOrganization(organization, user);
  }

  @Test
  public void synchronize_user_organization_membership_does_not_remove_existing_membership_on_organization_with_member_sync_disabled() {
    OrganizationDto organization = db.organizations().insert();
    GroupDto org1defaultGroup = db.users().insertDefaultGroup(organization, "Members");
    AlmAppInstallDto gitHubInstall = db.alm().insertAlmAppInstall(a -> a.setAlm(GITHUB));
    db.alm().insertOrganizationAlmBinding(organization, gitHubInstall, false);
    UserDto user = db.users().insertUser();
    db.users().insertMember(org1defaultGroup, user);
    db.organizations().addMember(organization, user);
    // User is member of a organization on which member sync is disabled
    db.organizations().assertUserIsMemberOfOrganization(organization, user);

    // The organization is not in the list, but membership should not be removed
    underTest.synchronizeUserOrganizationMembership(db.getSession(), user, GITHUB, ImmutableSet.of("other"));

    db.organizations().assertUserIsMemberOfOrganization(organization, user);
  }

  private void assertUserIsNotMember(OrganizationDto organization, UserDto user) {
    db.organizations().assertUserIsNotMemberOfOrganization(organization, user);
    SearchRequestBuilder request = es.client().prepareSearch(UserIndexDefinition.TYPE_USER)
      .setQuery(boolQuery()
        .must(termQuery(FIELD_ORGANIZATION_UUIDS, organization.getUuid()))
        .must(termQuery(FIELD_UUID, user.getUuid())));
    assertThat(request.get().getHits().getHits()).isEmpty();
  }

  private void assertOrgPermissionsOfUser(UserDto user, OrganizationDto organization, OrganizationPermission... permissions) {
    assertThat(dbClient.userPermissionDao().selectGlobalPermissionsOfUser(db.getSession(), user.getId(), organization.getUuid()).stream()
      .map(OrganizationPermission::fromKey))
        .containsOnly(permissions);
  }

  private void assertProjectPermissionsOfUser(UserDto user, ComponentDto project, String... permissions) {
    assertThat(dbClient.userPermissionDao().selectProjectPermissionsOfUser(db.getSession(), user.getId(), project.getId())).containsOnly(permissions);
  }

  private void insertProperty(String key, @Nullable String value, @Nullable Long resourceId, @Nullable Integer userId) {
    PropertyDto dto = new PropertyDto().setKey(key)
      .setResourceId(resourceId)
      .setUserId(userId)
      .setValue(value);
    db.properties().insertProperty(dto);
  }

}
