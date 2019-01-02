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
package org.sonar.db.permission.template;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.PermissionQuery;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;

import static com.google.common.primitives.Longs.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.CODEVIEWER;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.core.permission.GlobalPermissions.SCAN_EXECUTION;
import static org.sonar.db.permission.template.PermissionTemplateTesting.newPermissionTemplateDto;
import static org.sonar.db.user.GroupTesting.newGroupDto;

public class PermissionTemplateDaoTest {

  private static final Date PAST = new Date(100_000_000_000L);
  private static final Date NOW = new Date(500_000_000_000L);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public DbTester db = DbTester.create();

  private System2 system2 = mock(System2.class);
  private DbSession dbSession = db.getSession();
  private PermissionTemplateDbTester templateDb = db.permissionTemplates();

  private PermissionTemplateDao underTest = new PermissionTemplateDao(system2);

  @Before
  public void setUp() throws Exception {
    when(system2.now()).thenReturn(NOW.getTime());
  }

  @Test
  public void should_create_permission_template() {
    PermissionTemplateDto permissionTemplate = underTest.insert(db.getSession(), newPermissionTemplateDto()
      .setUuid("ABCD")
      .setName("my template")
      .setDescription("my description")
      .setKeyPattern("myregexp")
      .setOrganizationUuid("org")
      .setCreatedAt(PAST)
      .setUpdatedAt(NOW));
    db.commit();

    assertThat(underTest.selectByUuid(db.getSession(), permissionTemplate.getUuid()))
      .extracting(PermissionTemplateDto::getUuid, PermissionTemplateDto::getName, PermissionTemplateDto::getDescription, PermissionTemplateDto::getKeyPattern,
        PermissionTemplateDto::getOrganizationUuid, PermissionTemplateDto::getCreatedAt, PermissionTemplateDto::getUpdatedAt)
      .containsOnly("ABCD", "my template", "my description", "myregexp", "org", PAST, NOW);
  }

  @Test
  public void should_select_permission_template_by_uuid() {
    templateDb.insertTemplate(newPermissionTemplateDto()
      .setUuid("ABCD")
      .setName("my template")
      .setDescription("my description")
      .setKeyPattern("myregexp")
      .setOrganizationUuid("org"));

    assertThat(underTest.selectByUuid(db.getSession(), "ABCD"))
      .extracting(PermissionTemplateDto::getUuid, PermissionTemplateDto::getName, PermissionTemplateDto::getDescription, PermissionTemplateDto::getKeyPattern,
        PermissionTemplateDto::getOrganizationUuid)
      .containsOnly("ABCD", "my template", "my description", "myregexp", "org");
  }

  @Test
  public void selectAll_without_name_filtering() {
    templateDb.insertTemplate(newPermissionTemplateDto()
      .setUuid("tpl1")
      .setName("template1")
      .setDescription("description1")
      .setOrganizationUuid("org"));
    templateDb.insertTemplate(newPermissionTemplateDto()
      .setUuid("tpl2")
      .setName("template2")
      .setDescription("description2")
      .setOrganizationUuid("org"));
    templateDb.insertTemplate(newPermissionTemplateDto()
      .setUuid("tpl3")
      .setName("template3")
      .setDescription("description3")
      .setOrganizationUuid("org"));

    assertThat(underTest.selectAll(dbSession, "org", null))
      .extracting(PermissionTemplateDto::getUuid, PermissionTemplateDto::getName, PermissionTemplateDto::getDescription)
      .containsOnly(
        tuple("tpl1", "template1", "description1"),
        tuple("tpl2", "template2", "description2"),
        tuple("tpl3", "template3", "description3"));
    assertThat(underTest.selectAll(dbSession, "missingOrg", null)).isEmpty();
  }

  @Test
  public void selectAll_with_name_filtering() {
    PermissionTemplateDto t1InOrg1 = templateDb.insertTemplate(newPermissionTemplateDto().setName("aBcDeF").setOrganizationUuid("org1"));
    PermissionTemplateDto t2InOrg1 = templateDb.insertTemplate(newPermissionTemplateDto().setName("cdefgh").setOrganizationUuid("org1"));
    PermissionTemplateDto t3InOrg1 = templateDb.insertTemplate(newPermissionTemplateDto().setName("hijkl").setOrganizationUuid("org2"));
    PermissionTemplateDto t4InOrg2 = templateDb.insertTemplate(newPermissionTemplateDto().setName("cdefgh").setOrganizationUuid("org2"));

    assertThat(underTest.selectAll(dbSession, "org1", "def")).extracting(PermissionTemplateDto::getId).containsExactly(t1InOrg1.getId(), t2InOrg1.getId());
    assertThat(underTest.selectAll(dbSession, "org1", "missing")).isEmpty();
  }

  @Test
  public void should_update_permission_template() {
    PermissionTemplateDto permissionTemplateDto = templateDb.insertTemplate(newPermissionTemplateDto()
      .setUuid("ABCD")
      .setName("name")
      .setDescription("description")
      .setKeyPattern("regexp")
      .setOrganizationUuid("org")
      .setCreatedAt(PAST)
      .setUpdatedAt(PAST));

    underTest.update(dbSession, permissionTemplateDto
      .setName("new_name")
      .setDescription("new_description")
      .setKeyPattern("new_regexp")
      .setUpdatedAt(NOW)
      // Invariant fields, should not be updated
      .setUuid("new UUID")
      .setOrganizationUuid("new org")
      .setCreatedAt(NOW));
    db.commit();

    assertThat(underTest.selectByUuid(db.getSession(), "ABCD"))
      .extracting(PermissionTemplateDto::getUuid, PermissionTemplateDto::getName, PermissionTemplateDto::getDescription, PermissionTemplateDto::getKeyPattern,
        PermissionTemplateDto::getOrganizationUuid, PermissionTemplateDto::getCreatedAt, PermissionTemplateDto::getUpdatedAt)
      .containsOnly("ABCD", "new_name", "new_description", "new_regexp", "org", PAST, NOW);
  }

  @Test
  public void should_delete_permission_template() {
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    PermissionTemplateDto permissionTemplate1 = templateDb.insertTemplate(db.getDefaultOrganization());
    PermissionTemplateDto permissionTemplate2 = templateDb.insertTemplate(db.getDefaultOrganization());
    templateDb.addUserToTemplate(permissionTemplate1, user1, "user");
    templateDb.addUserToTemplate(permissionTemplate1, user2, "user");
    templateDb.addUserToTemplate(permissionTemplate1, user2, "admin");
    templateDb.addUserToTemplate(permissionTemplate2, user2, "admin");
    templateDb.addGroupToTemplate(permissionTemplate1, group1, "user");
    templateDb.addGroupToTemplate(permissionTemplate1, group2, "user");
    templateDb.addAnyoneToTemplate(permissionTemplate1, "admin");
    templateDb.addAnyoneToTemplate(permissionTemplate2, "admin");
    templateDb.addProjectCreatorToTemplate(permissionTemplate1.getId(), "user");
    templateDb.addProjectCreatorToTemplate(permissionTemplate2.getId(), "user");

    underTest.deleteById(dbSession, permissionTemplate1.getId());
    dbSession.commit();

    assertThat(underTest.selectAll(db.getSession(), db.getDefaultOrganization().getUuid(), null))
      .extracting(PermissionTemplateDto::getUuid)
      .containsOnly(permissionTemplate2.getUuid());
    assertThat(db.getDbClient().permissionTemplateDao().selectUserPermissionsByTemplateId(db.getSession(), permissionTemplate1.getId())).isEmpty();
    assertThat(db.getDbClient().permissionTemplateDao().selectUserPermissionsByTemplateId(db.getSession(), permissionTemplate2.getId())).hasSize(1);
    assertThat(db.getDbClient().permissionTemplateDao().selectGroupPermissionsByTemplateId(db.getSession(), permissionTemplate1.getId())).isEmpty();
    assertThat(db.getDbClient().permissionTemplateDao().selectGroupPermissionsByTemplateId(db.getSession(), permissionTemplate2.getId())).hasSize(1);
    assertThat(db.getDbClient().permissionTemplateCharacteristicDao().selectByTemplateIds(db.getSession(), asList(permissionTemplate1.getId(), permissionTemplate2.getId())))
      .extracting(PermissionTemplateCharacteristicDto::getTemplateId)
      .containsOnly(permissionTemplate2.getId());
  }

  @Test
  public void should_add_user_permission_to_template() {
    PermissionTemplateDto permissionTemplate = templateDb.insertTemplate(db.getDefaultOrganization());
    UserDto user = db.users().insertUser();

    underTest.insertUserPermission(dbSession, permissionTemplate.getId(), user.getId(), "user");

    assertThat(db.getDbClient().permissionTemplateDao().selectUserPermissionsByTemplateId(db.getSession(), permissionTemplate.getId()))
      .extracting(PermissionTemplateUserDto::getTemplateId, PermissionTemplateUserDto::getUserId, PermissionTemplateUserDto::getPermission, PermissionTemplateUserDto::getCreatedAt,
        PermissionTemplateUserDto::getUpdatedAt)
      .containsOnly(tuple(permissionTemplate.getId(), user.getId(), "user", NOW, NOW));
  }

  @Test
  public void should_remove_user_permission_from_template() {
    PermissionTemplateDto permissionTemplate = templateDb.insertTemplate(db.getDefaultOrganization());
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    templateDb.addUserToTemplate(permissionTemplate, user1, "user");
    templateDb.addUserToTemplate(permissionTemplate, user1, "admin");
    templateDb.addUserToTemplate(permissionTemplate, user2, "user");

    underTest.deleteUserPermission(dbSession, permissionTemplate.getId(), user1.getId(), "user");

    assertThat(db.getDbClient().permissionTemplateDao().selectUserPermissionsByTemplateId(db.getSession(), permissionTemplate.getId()))
      .extracting(PermissionTemplateUserDto::getUserId, PermissionTemplateUserDto::getPermission)
      .containsOnly(tuple(user1.getId(), "admin"), tuple(user2.getId(), "user"));
  }

  @Test
  public void should_add_group_permission_to_template() {
    PermissionTemplateDto permissionTemplate = templateDb.insertTemplate(db.getDefaultOrganization());
    GroupDto group = db.users().insertGroup();

    underTest.insertGroupPermission(dbSession, permissionTemplate.getId(), group.getId(), "user");
    dbSession.commit();

    assertThat(db.getDbClient().permissionTemplateDao().selectGroupPermissionsByTemplateId(db.getSession(), permissionTemplate.getId()))
      .extracting(PermissionTemplateGroupDto::getTemplateId, PermissionTemplateGroupDto::getGroupId, PermissionTemplateGroupDto::getPermission,
        PermissionTemplateGroupDto::getCreatedAt,
        PermissionTemplateGroupDto::getUpdatedAt)
      .containsOnly(tuple(permissionTemplate.getId(), group.getId(), "user", NOW, NOW));
  }

  @Test
  public void remove_by_group() {
    PermissionTemplateDto permissionTemplate = templateDb.insertTemplate(db.getDefaultOrganization());
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    templateDb.addGroupToTemplate(permissionTemplate, group1, "user");
    templateDb.addGroupToTemplate(permissionTemplate, group1, "admin");
    templateDb.addGroupToTemplate(permissionTemplate, group2, "user");

    underTest.deleteByGroup(db.getSession(), group1.getId());
    db.getSession().commit();

    assertThat(db.getDbClient().permissionTemplateDao().selectGroupPermissionsByTemplateId(db.getSession(), permissionTemplate.getId()))
      .extracting(PermissionTemplateGroupDto::getGroupId, PermissionTemplateGroupDto::getPermission)
      .containsOnly(tuple(group2.getId(), "user"));
  }

  @Test
  public void should_add_group_permission_to_anyone() {
    PermissionTemplateDto permissionTemplate = templateDb.insertTemplate(db.getDefaultOrganization());

    underTest.insertGroupPermission(dbSession, permissionTemplate.getId(), null, "user");
    dbSession.commit();

    assertThat(db.getDbClient().permissionTemplateDao().selectGroupPermissionsByTemplateId(db.getSession(), permissionTemplate.getId()))
      .extracting(PermissionTemplateGroupDto::getTemplateId, PermissionTemplateGroupDto::getGroupId, PermissionTemplateGroupDto::getGroupName,
        PermissionTemplateGroupDto::getPermission)
      .containsOnly(tuple(permissionTemplate.getId(), 0, "Anyone", "user"));
  }

  @Test
  public void group_count_by_template_and_permission() {
    PermissionTemplateDto template1 = templateDb.insertTemplate();
    PermissionTemplateDto template2 = templateDb.insertTemplate();
    PermissionTemplateDto template3 = templateDb.insertTemplate();
    PermissionTemplateDto template4 = templateDb.insertTemplate();
    GroupDto group1 = db.users().insertGroup(newGroupDto());
    GroupDto group2 = db.users().insertGroup(newGroupDto());
    GroupDto group3 = db.users().insertGroup(newGroupDto());
    templateDb.addGroupToTemplate(template1.getId(), group1.getId(), CODEVIEWER);
    templateDb.addGroupToTemplate(template1.getId(), group2.getId(), CODEVIEWER);
    templateDb.addGroupToTemplate(template1.getId(), group3.getId(), CODEVIEWER);
    templateDb.addGroupToTemplate(template1.getId(), null, CODEVIEWER);
    templateDb.addGroupToTemplate(template1.getId(), group1.getId(), ADMIN);
    templateDb.addGroupToTemplate(template2.getId(), group1.getId(), ADMIN);
    templateDb.addGroupToTemplate(template4.getId(), group1.getId(), ISSUE_ADMIN);

    final List<CountByTemplateAndPermissionDto> result = new ArrayList<>();
    underTest.groupsCountByTemplateIdAndPermission(dbSession, asList(template1.getId(), template2.getId(), template3.getId()),
      context -> result.add(context.getResultObject()));

    assertThat(result).extracting(CountByTemplateAndPermissionDto::getPermission, CountByTemplateAndPermissionDto::getTemplateId, CountByTemplateAndPermissionDto::getCount)
      .containsOnly(tuple(ADMIN, template1.getId(), 1), tuple(CODEVIEWER, template1.getId(), 4), tuple(ADMIN, template2.getId(), 1));
  }

  @Test
  public void user_count_by_template_and_permission() {
    PermissionTemplateDto template1 = templateDb.insertTemplate();
    PermissionTemplateDto template2 = templateDb.insertTemplate();
    PermissionTemplateDto template3 = templateDb.insertTemplate();
    PermissionTemplateDto anotherTemplate = templateDb.insertTemplate();

    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    UserDto user3 = db.users().insertUser();

    templateDb.addUserToTemplate(template1.getId(), user1.getId(), ADMIN);
    templateDb.addUserToTemplate(template1.getId(), user2.getId(), ADMIN);
    templateDb.addUserToTemplate(template1.getId(), user3.getId(), ADMIN);
    templateDb.addUserToTemplate(template1.getId(), user1.getId(), USER);
    templateDb.addUserToTemplate(template2.getId(), user1.getId(), USER);
    templateDb.addUserToTemplate(anotherTemplate.getId(), user1.getId(), ISSUE_ADMIN);

    final List<CountByTemplateAndPermissionDto> result = new ArrayList<>();
    underTest.usersCountByTemplateIdAndPermission(dbSession, asList(template1.getId(), template2.getId(), template3.getId()),
      context -> result.add(context.getResultObject()));
    assertThat(result)
      .extracting(CountByTemplateAndPermissionDto::getPermission, CountByTemplateAndPermissionDto::getTemplateId, CountByTemplateAndPermissionDto::getCount)
      .containsExactlyInAnyOrder(
        tuple(ADMIN, template1.getId(), 3),
        tuple(USER, template1.getId(), 1),
        tuple(USER, template2.getId(), 1));
  }

  @Test
  public void selectPotentialPermissions_with_unknown_template_and_no_user() {
    List<String> result = underTest.selectPotentialPermissionsByUserIdAndTemplateId(dbSession, null, 42L);

    assertThat(result).isEmpty();
  }

  @Test
  public void selectPotentialPermissions_with_empty_template_and_new_user() {
    UserDto user = db.users().insertUser();
    PermissionTemplateDto template = templateDb.insertTemplate();

    List<String> result = underTest.selectPotentialPermissionsByUserIdAndTemplateId(dbSession, user.getId(), template.getId());

    assertThat(result).isEmpty();
  }

  @Test
  public void selectPotentialPermission_with_template_users_groups_and_project_creator() {
    UserDto user = db.users().insertUser();
    GroupDto group = db.users().insertGroup(newGroupDto());
    db.users().insertMember(group, user);
    PermissionTemplateDto template = templateDb.insertTemplate();
    templateDb.addProjectCreatorToTemplate(template.getId(), SCAN_EXECUTION);
    templateDb.addProjectCreatorToTemplate(template.getId(), UserRole.ADMIN);
    templateDb.addUserToTemplate(template.getId(), user.getId(), UserRole.USER);
    templateDb.addUserToTemplate(template.getId(), user.getId(), UserRole.ADMIN);
    templateDb.addGroupToTemplate(template.getId(), group.getId(), UserRole.CODEVIEWER);
    templateDb.addGroupToTemplate(template.getId(), group.getId(), UserRole.ADMIN);
    templateDb.addGroupToTemplate(template.getId(), null, UserRole.ISSUE_ADMIN);

    List<String> resultWithUser = underTest.selectPotentialPermissionsByUserIdAndTemplateId(dbSession, user.getId(), template.getId());
    List<String> resultWithoutUser = underTest.selectPotentialPermissionsByUserIdAndTemplateId(dbSession, null, template.getId());

    assertThat(resultWithUser).containsOnlyOnce(SCAN_EXECUTION, UserRole.ADMIN, UserRole.USER, UserRole.CODEVIEWER, UserRole.ISSUE_ADMIN);
    // only permission from anyone group
    assertThat(resultWithoutUser).containsOnly(UserRole.ISSUE_ADMIN);
  }

  @Test
  public void selectAllGroupPermissionTemplatesByGroupId() {
    PermissionTemplateDto permissionTemplate = templateDb.insertTemplate(db.getDefaultOrganization());
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    templateDb.addGroupToTemplate(permissionTemplate, group1, "user");
    templateDb.addGroupToTemplate(permissionTemplate, group1, "admin");
    templateDb.addGroupToTemplate(permissionTemplate, group2, "user");

    assertThat(db.getDbClient().permissionTemplateDao().selectAllGroupPermissionTemplatesByGroupId(db.getSession(), group1.getId()))
      .extracting(PermissionTemplateGroupDto::getGroupId, PermissionTemplateGroupDto::getPermission)
      .containsOnly(tuple(group1.getId(), "user"), tuple(group1.getId(), "admin"));
  }

  @Test
  public void deleteByOrganization_does_not_fail_on_empty_db() {
    underTest.deleteByOrganization(dbSession, "some uuid");
    dbSession.commit();
  }

  @Test
  public void deleteByOrganization_does_not_fail_when_organization_has_no_template() {
    OrganizationDto organization = db.organizations().insert();

    underTest.deleteByOrganization(dbSession, organization.getUuid());
    dbSession.commit();
  }

  @Test
  public void deleteByOrganization_delete_all_templates_of_organization_and_content_of_child_tables() {
    OrganizationDto organization1 = db.organizations().insert();
    OrganizationDto organization2 = db.organizations().insert();
    OrganizationDto organization3 = db.organizations().insert();

    PermissionTemplateDto[] templates = {
      createTemplate(organization1),
      createTemplate(organization2),
      createTemplate(organization3),
      createTemplate(organization1),
      createTemplate(organization2)
    };

    verifyTemplateIdsInDb(templates[0].getId(), templates[1].getId(), templates[2].getId(), templates[3].getId(), templates[4].getId());

    underTest.deleteByOrganization(dbSession, organization2.getUuid());
    dbSession.commit();
    verifyTemplateIdsInDb(templates[0].getId(), templates[2].getId(), templates[3].getId());

    underTest.deleteByOrganization(dbSession, organization3.getUuid());
    dbSession.commit();
    verifyTemplateIdsInDb(templates[0].getId(), templates[3].getId());

    underTest.deleteByOrganization(dbSession, organization1.getUuid());
    dbSession.commit();
    verifyTemplateIdsInDb();
  }

  @Test
  public void delete_user_permissions_by_organization() {
    OrganizationDto organization = db.organizations().insert();
    OrganizationDto anotherOrganization = db.organizations().insert();
    UserDto user = db.users().insertUser();
    UserDto anotherUser = db.users().insertUser();
    PermissionTemplateDto template = db.permissionTemplates().insertTemplate(organization);
    PermissionTemplateDto anotherTemplate = db.permissionTemplates().insertTemplate(anotherOrganization);
    String permission = "PERMISSION";
    db.permissionTemplates().addUserToTemplate(template.getId(), user.getId(), permission);
    db.permissionTemplates().addUserToTemplate(template.getId(), anotherUser.getId(), permission);
    db.permissionTemplates().addUserToTemplate(anotherTemplate.getId(), user.getId(), permission);

    underTest.deleteUserPermissionsByOrganization(dbSession, organization.getUuid(), user.getId());

    assertThat(underTest.selectUserPermissionsByTemplateId(dbSession, template.getId())).extracting(PermissionTemplateUserDto::getUserId).containsOnly(anotherUser.getId());
    assertThat(underTest.selectUserPermissionsByTemplateId(dbSession, anotherTemplate.getId())).extracting(PermissionTemplateUserDto::getUserId).containsOnly(user.getId());
  }

  @Test
  public void delete_user_permissions_by_user_id() {
    OrganizationDto organization = db.organizations().insert();
    OrganizationDto anotherOrganization = db.organizations().insert();
    UserDto user = db.users().insertUser();
    UserDto anotherUser = db.users().insertUser();
    PermissionTemplateDto template = db.permissionTemplates().insertTemplate(organization);
    PermissionTemplateDto anotherTemplate = db.permissionTemplates().insertTemplate(anotherOrganization);
    String permission = "PERMISSION";
    db.permissionTemplates().addUserToTemplate(template.getId(), user.getId(), permission);
    db.permissionTemplates().addUserToTemplate(template.getId(), anotherUser.getId(), permission);
    db.permissionTemplates().addUserToTemplate(anotherTemplate.getId(), user.getId(), permission);

    underTest.deleteUserPermissionsByUserId(dbSession, user.getId());
    db.commit();

    assertThat(db.select("select template_id as \"templateId\", user_id as \"userId\", permission_reference as \"permission\" from perm_templates_users"))
      .extracting((row) -> row.get("templateId"), (row) -> row.get("userId"), (row) -> row.get("permission"))
      .containsOnly(tuple(template.getId(), anotherUser.getId().longValue(), permission));
  }

  private PermissionTemplateDto createTemplate(OrganizationDto organization) {
    UserDto user = db.users().insertUser();
    GroupDto group = db.users().insertGroup();
    db.users().insertMember(group, user);
    PermissionTemplateDto template = templateDb.insertTemplate(organization);
    templateDb.addProjectCreatorToTemplate(template.getId(), SCAN_EXECUTION);
    templateDb.addProjectCreatorToTemplate(template.getId(), UserRole.ADMIN);
    templateDb.addUserToTemplate(template.getId(), user.getId(), UserRole.USER);
    templateDb.addUserToTemplate(template.getId(), user.getId(), UserRole.ADMIN);
    templateDb.addGroupToTemplate(template.getId(), group.getId(), UserRole.CODEVIEWER);
    templateDb.addGroupToTemplate(template.getId(), group.getId(), UserRole.ADMIN);
    templateDb.addGroupToTemplate(template.getId(), null, UserRole.ISSUE_ADMIN);
    return template;
  }

  private void verifyTemplateIdsInDb(Long... expectedTemplateIds) {
    assertThat(db.select("select distinct template_id as \"templateId\" from perm_templates_groups"))
      .extracting((row) -> (Long) row.get("templateId"))
      .containsOnly(expectedTemplateIds);
    assertThat(db.select("select distinct template_id as \"templateId\" from perm_templates_users"))
      .extracting((row) -> (Long) row.get("templateId"))
      .containsOnly(expectedTemplateIds);
    assertThat(db.select("select distinct template_id as \"templateId\" from perm_tpl_characteristics"))
      .extracting((row) -> (Long) row.get("templateId"))
      .containsOnly(expectedTemplateIds);
    assertThat(db.select("select distinct id as \"templateId\" from permission_templates"))
      .extracting((row) -> (Long) row.get("templateId"))
      .containsOnly(expectedTemplateIds);
  }

}
