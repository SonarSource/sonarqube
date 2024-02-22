/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.audit.NoOpAuditPersister;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.permission.template.PermissionTemplateTesting.newPermissionTemplateDto;
import static org.sonar.db.user.GroupTesting.newGroupDto;

class PermissionTemplateDaoIT {

  private static final Date PAST = new Date(100_000_000_000L);
  private static final Date NOW = new Date(500_000_000_000L);

  @RegisterExtension
  private final DbTester db = DbTester.create();

  private final System2 system2 = mock(System2.class);
  private final DbSession dbSession = db.getSession();
  private final PermissionTemplateDbTester templateDb = db.permissionTemplates();
  private final UuidFactory uuidFactory = UuidFactoryFast.getInstance();

  private final PermissionTemplateDao underTest = new PermissionTemplateDao(uuidFactory, system2, new NoOpAuditPersister());

  @BeforeEach
  void setUp() {
    when(system2.now()).thenReturn(NOW.getTime());
  }

  @Test
  void create_permission_template() {
    PermissionTemplateDto permissionTemplate = underTest.insert(db.getSession(), newPermissionTemplateDto()
      .setUuid("ABCD")
      .setName("my template")
      .setDescription("my description")
      .setKeyPattern("myregexp")
      .setCreatedAt(PAST)
      .setUpdatedAt(NOW));
    db.commit();

    assertThat(underTest.selectByUuid(db.getSession(), permissionTemplate.getUuid()))
      .extracting(PermissionTemplateDto::getUuid, PermissionTemplateDto::getName, PermissionTemplateDto::getDescription,
        PermissionTemplateDto::getKeyPattern,
        PermissionTemplateDto::getCreatedAt, PermissionTemplateDto::getUpdatedAt)
      .containsOnly("ABCD", "my template", "my description", "myregexp", PAST, NOW);
  }

  @Test
  void select_permission_template_by_uuid() {
    templateDb.insertTemplate(newPermissionTemplateDto()
      .setUuid("ABCD")
      .setName("my template")
      .setDescription("my description")
      .setKeyPattern("myregexp"));

    assertThat(underTest.selectByUuid(db.getSession(), "ABCD"))
      .extracting(PermissionTemplateDto::getUuid, PermissionTemplateDto::getName, PermissionTemplateDto::getDescription,
        PermissionTemplateDto::getKeyPattern)
      .containsOnly("ABCD", "my template", "my description", "myregexp");
  }

  @Test
  void selectAll_without_name_filtering() {
    templateDb.insertTemplate(newPermissionTemplateDto()
      .setUuid("tpl1")
      .setName("template1")
      .setDescription("description1"));
    templateDb.insertTemplate(newPermissionTemplateDto()
      .setUuid("tpl2")
      .setName("template2")
      .setDescription("description2"));
    templateDb.insertTemplate(newPermissionTemplateDto()
      .setUuid("tpl3")
      .setName("template3")
      .setDescription("description3"));

    assertThat(underTest.selectAll(dbSession, null))
      .extracting(PermissionTemplateDto::getUuid, PermissionTemplateDto::getName, PermissionTemplateDto::getDescription)
      .containsOnly(
        tuple("tpl1", "template1", "description1"),
        tuple("tpl2", "template2", "description2"),
        tuple("tpl3", "template3", "description3"));
  }

  @Test
  void selectAll_with_name_filtering() {
    PermissionTemplateDto t1InOrg1 = templateDb.insertTemplate(newPermissionTemplateDto().setName("aBcDeF"));
    PermissionTemplateDto t2InOrg1 = templateDb.insertTemplate(newPermissionTemplateDto().setName("cdefgh"));
    PermissionTemplateDto t3InOrg1 = templateDb.insertTemplate(newPermissionTemplateDto().setName("hijkl"));

    assertThat(underTest.selectAll(dbSession, "def")).extracting(PermissionTemplateDto::getUuid).containsExactly(t1InOrg1.getUuid(),
      t2InOrg1.getUuid());
    assertThat(underTest.selectAll(dbSession, "missing")).isEmpty();
  }

  @Test
  void should_update_permission_template() {
    PermissionTemplateDto permissionTemplateDto = templateDb.insertTemplate(newPermissionTemplateDto()
      .setUuid("ABCD")
      .setName("name")
      .setDescription("description")
      .setKeyPattern("regexp")
      .setCreatedAt(PAST)
      .setUpdatedAt(PAST));

    underTest.update(dbSession, permissionTemplateDto
      .setName("new_name")
      .setDescription("new_description")
      .setKeyPattern("new_regexp")
      .setUpdatedAt(NOW)
      // Invariant fields, should not be updated
      .setUuid("ABCD")
      .setCreatedAt(NOW));
    db.commit();

    assertThat(underTest.selectByUuid(db.getSession(), "ABCD"))
      .extracting(PermissionTemplateDto::getUuid, PermissionTemplateDto::getName, PermissionTemplateDto::getDescription,
        PermissionTemplateDto::getKeyPattern,
        PermissionTemplateDto::getCreatedAt, PermissionTemplateDto::getUpdatedAt)
      .containsOnly("ABCD", "new_name", "new_description", "new_regexp", PAST, NOW);
  }

  @Test
  void delete_permission_template() {
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    PermissionTemplateDto permissionTemplate1 = templateDb.insertTemplate();
    PermissionTemplateDto permissionTemplate2 = templateDb.insertTemplate();
    templateDb.addUserToTemplate(permissionTemplate1, user1, "user");
    templateDb.addUserToTemplate(permissionTemplate1, user2, "user");
    templateDb.addUserToTemplate(permissionTemplate1, user2, "admin");
    templateDb.addUserToTemplate(permissionTemplate2, user2, "admin");
    templateDb.addGroupToTemplate(permissionTemplate1, group1, "user");
    templateDb.addGroupToTemplate(permissionTemplate1, group2, "user");
    templateDb.addAnyoneToTemplate(permissionTemplate1, "admin");
    templateDb.addAnyoneToTemplate(permissionTemplate2, "admin");
    templateDb.addProjectCreatorToTemplate(permissionTemplate1.getUuid(), "user", permissionTemplate1.getName());
    templateDb.addProjectCreatorToTemplate(permissionTemplate2.getUuid(), "user", permissionTemplate2.getName());

    underTest.deleteByUuid(dbSession, permissionTemplate1.getUuid(), permissionTemplate1.getName());
    dbSession.commit();

    assertThat(underTest.selectAll(db.getSession(), null))
      .extracting(PermissionTemplateDto::getUuid)
      .containsOnly(permissionTemplate2.getUuid());
    assertThat(db.getDbClient().permissionTemplateDao().selectUserPermissionsByTemplateId(db.getSession(), permissionTemplate1.getUuid())).isEmpty();
    assertThat(db.getDbClient().permissionTemplateDao().selectUserPermissionsByTemplateId(db.getSession(), permissionTemplate2.getUuid())).hasSize(1);
    assertThat(db.getDbClient().permissionTemplateDao().selectGroupPermissionsByTemplateUuid(db.getSession(),
      permissionTemplate1.getUuid())).isEmpty();
    assertThat(db.getDbClient().permissionTemplateDao().selectGroupPermissionsByTemplateUuid(db.getSession(),
      permissionTemplate2.getUuid())).hasSize(1);
    assertThat(db.getDbClient().permissionTemplateCharacteristicDao().selectByTemplateUuids(db.getSession(),
      asList(permissionTemplate1.getUuid(), permissionTemplate2.getUuid())))
      .extracting(PermissionTemplateCharacteristicDto::getTemplateUuid)
      .containsOnly(permissionTemplate2.getUuid());
  }

  @Test
  void add_user_permission_to_template() {
    PermissionTemplateDto permissionTemplate = templateDb.insertTemplate();
    UserDto user = db.users().insertUser();

    underTest.insertUserPermission(dbSession, permissionTemplate.getUuid(), user.getUuid(), "user", permissionTemplate.getName(),
      user.getLogin());

    assertThat(db.getDbClient().permissionTemplateDao().selectUserPermissionsByTemplateId(db.getSession(), permissionTemplate.getUuid()))
      .extracting(PermissionTemplateUserDto::getTemplateUuid, PermissionTemplateUserDto::getUserUuid,
        PermissionTemplateUserDto::getPermission,
        PermissionTemplateUserDto::getCreatedAt,
        PermissionTemplateUserDto::getUpdatedAt)
      .containsOnly(tuple(permissionTemplate.getUuid(), user.getUuid(), "user", NOW, NOW));
  }

  @Test
  void remove_user_permission_from_template() {
    PermissionTemplateDto permissionTemplate = templateDb.insertTemplate();
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    templateDb.addUserToTemplate(permissionTemplate, user1, "user");
    templateDb.addUserToTemplate(permissionTemplate, user1, "admin");
    templateDb.addUserToTemplate(permissionTemplate, user2, "user");

    underTest.deleteUserPermission(dbSession, permissionTemplate.getUuid(), user1.getUuid(), "user", permissionTemplate.getName(),
      user1.getLogin());

    assertThat(db.getDbClient().permissionTemplateDao().selectUserPermissionsByTemplateId(db.getSession(), permissionTemplate.getUuid()))
      .extracting(PermissionTemplateUserDto::getUserUuid, PermissionTemplateUserDto::getPermission)
      .containsOnly(tuple(user1.getUuid(), "admin"), tuple(user2.getUuid(), "user"));
  }

  @Test
  void add_group_permission_to_template() {
    PermissionTemplateDto permissionTemplate = templateDb.insertTemplate();
    GroupDto group = db.users().insertGroup();

    underTest.insertGroupPermission(dbSession, permissionTemplate.getUuid(), group.getUuid(), "user", permissionTemplate.getName(),
      group.getName());
    dbSession.commit();

    assertThat(db.getDbClient().permissionTemplateDao().selectGroupPermissionsByTemplateUuid(db.getSession(), permissionTemplate.getUuid()))
      .extracting(PermissionTemplateGroupDto::getTemplateUuid, PermissionTemplateGroupDto::getGroupUuid,
        PermissionTemplateGroupDto::getPermission,
        PermissionTemplateGroupDto::getCreatedAt,
        PermissionTemplateGroupDto::getUpdatedAt)
      .containsOnly(tuple(permissionTemplate.getUuid(), group.getUuid(), "user", NOW, NOW));
  }

  @Test
  void remove_by_group() {
    PermissionTemplateDto permissionTemplate = templateDb.insertTemplate();
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    templateDb.addGroupToTemplate(permissionTemplate, group1, "user");
    templateDb.addGroupToTemplate(permissionTemplate, group1, "admin");
    templateDb.addGroupToTemplate(permissionTemplate, group2, "user");

    underTest.deleteByGroup(db.getSession(), group1.getUuid(), group1.getName());
    db.getSession().commit();

    assertThat(db.getDbClient().permissionTemplateDao().selectGroupPermissionsByTemplateUuid(db.getSession(), permissionTemplate.getUuid()))
      .extracting(PermissionTemplateGroupDto::getGroupUuid, PermissionTemplateGroupDto::getPermission)
      .containsOnly(tuple(group2.getUuid(), "user"));
  }

  @Test
  void add_group_permission_to_anyone() {
    PermissionTemplateDto permissionTemplate = templateDb.insertTemplate();

    underTest.insertGroupPermission(dbSession, permissionTemplate.getUuid(), null, "user", permissionTemplate.getName(), null);
    dbSession.commit();

    assertThat(db.getDbClient().permissionTemplateDao().selectGroupPermissionsByTemplateUuid(db.getSession(), permissionTemplate.getUuid()))
      .extracting(PermissionTemplateGroupDto::getTemplateUuid, PermissionTemplateGroupDto::getGroupUuid,
        PermissionTemplateGroupDto::getGroupName,
        PermissionTemplateGroupDto::getPermission)
      .containsOnly(tuple(permissionTemplate.getUuid(), "Anyone", "Anyone", "user"));
  }

  @Test
  void group_count_by_template_and_permission() {
    PermissionTemplateDto template1 = templateDb.insertTemplate();
    PermissionTemplateDto template2 = templateDb.insertTemplate();
    PermissionTemplateDto template3 = templateDb.insertTemplate();
    PermissionTemplateDto template4 = templateDb.insertTemplate();
    GroupDto group1 = db.users().insertGroup(newGroupDto());
    GroupDto group2 = db.users().insertGroup(newGroupDto());
    GroupDto group3 = db.users().insertGroup(newGroupDto());
    templateDb.addGroupToTemplate(template1.getUuid(), group1.getUuid(), UserRole.CODEVIEWER, template1.getName(), group1.getName());
    templateDb.addGroupToTemplate(template1.getUuid(), group2.getUuid(), UserRole.CODEVIEWER, template1.getName(), group2.getName());
    templateDb.addGroupToTemplate(template1.getUuid(), group3.getUuid(), UserRole.CODEVIEWER, template1.getName(), group3.getName());
    templateDb.addGroupToTemplate(template1.getUuid(), null, UserRole.CODEVIEWER, template1.getName(), null);
    templateDb.addGroupToTemplate(template1.getUuid(), group1.getUuid(), UserRole.ADMIN, template1.getName(), group1.getName());
    templateDb.addGroupToTemplate(template2.getUuid(), group1.getUuid(), UserRole.ADMIN, template2.getName(), group1.getName());
    templateDb.addGroupToTemplate(template4.getUuid(), group1.getUuid(), UserRole.ISSUE_ADMIN, template4.getName(), group1.getName());

    final List<CountByTemplateAndPermissionDto> result = new ArrayList<>();
    underTest.groupsCountByTemplateUuidAndPermission(dbSession, asList(template1.getUuid(), template2.getUuid(), template3.getUuid()),
      context -> result.add(context.getResultObject()));

    assertThat(result).extracting(CountByTemplateAndPermissionDto::getPermission, CountByTemplateAndPermissionDto::getTemplateUuid,
        CountByTemplateAndPermissionDto::getCount)
      .containsOnly(tuple(UserRole.ADMIN, template1.getUuid(), 1), tuple(UserRole.CODEVIEWER, template1.getUuid(), 4),
        tuple(UserRole.ADMIN, template2.getUuid(), 1));
  }

  @Test
  void user_count_by_template_and_permission() {
    PermissionTemplateDto template1 = templateDb.insertTemplate();
    PermissionTemplateDto template2 = templateDb.insertTemplate();
    PermissionTemplateDto template3 = templateDb.insertTemplate();
    PermissionTemplateDto anotherTemplate = templateDb.insertTemplate();

    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    UserDto user3 = db.users().insertUser();

    templateDb.addUserToTemplate(template1.getUuid(), user1.getUuid(), UserRole.ADMIN, template1.getName(), user1.getLogin());
    templateDb.addUserToTemplate(template1.getUuid(), user2.getUuid(), UserRole.ADMIN, template1.getName(), user2.getLogin());
    templateDb.addUserToTemplate(template1.getUuid(), user3.getUuid(), UserRole.ADMIN, template1.getName(), user3.getLogin());
    templateDb.addUserToTemplate(template1.getUuid(), user1.getUuid(), UserRole.USER, template1.getName(), user1.getLogin());
    templateDb.addUserToTemplate(template2.getUuid(), user1.getUuid(), UserRole.USER, template2.getName(), user1.getLogin());
    templateDb.addUserToTemplate(anotherTemplate.getUuid(), user1.getUuid(), UserRole.ISSUE_ADMIN, anotherTemplate.getName(),
      user1.getLogin());

    final List<CountByTemplateAndPermissionDto> result = new ArrayList<>();
    underTest.usersCountByTemplateUuidAndPermission(dbSession, asList(template1.getUuid(), template2.getUuid(), template3.getUuid()),
      context -> result.add(context.getResultObject()));
    assertThat(result)
      .extracting(CountByTemplateAndPermissionDto::getPermission, CountByTemplateAndPermissionDto::getTemplateUuid,
        CountByTemplateAndPermissionDto::getCount)
      .containsExactlyInAnyOrder(
        tuple(UserRole.ADMIN, template1.getUuid(), 3),
        tuple(UserRole.USER, template1.getUuid(), 1),
        tuple(UserRole.USER, template2.getUuid(), 1));
  }

  @Test
  void selectPotentialPermissions_with_unknown_template_and_no_user() {
    List<String> result = underTest.selectPotentialPermissionsByUserUuidAndTemplateUuid(dbSession, null, "42");

    assertThat(result).isEmpty();
  }

  @Test
  void selectPotentialPermissions_with_empty_template_and_new_user() {
    UserDto user = db.users().insertUser();
    PermissionTemplateDto template = templateDb.insertTemplate();

    List<String> result = underTest.selectPotentialPermissionsByUserUuidAndTemplateUuid(dbSession, user.getUuid(), template.getUuid());

    assertThat(result).isEmpty();
  }

  @Test
  void selectPotentialPermission_with_template_users_groups_and_project_creator() {
    UserDto user = db.users().insertUser();
    GroupDto group = db.users().insertGroup(newGroupDto());
    db.users().insertMember(group, user);
    PermissionTemplateDto template = templateDb.insertTemplate();
    templateDb.addProjectCreatorToTemplate(template.getUuid(), GlobalPermission.SCAN.getKey(), template.getName());
    templateDb.addProjectCreatorToTemplate(template.getUuid(), UserRole.ADMIN, template.getName());
    templateDb.addUserToTemplate(template.getUuid(), user.getUuid(), UserRole.USER, template.getName(), user.getLogin());
    templateDb.addUserToTemplate(template.getUuid(), user.getUuid(), UserRole.ADMIN, template.getName(), user.getLogin());
    templateDb.addGroupToTemplate(template.getUuid(), group.getUuid(), UserRole.CODEVIEWER, template.getName(), group.getName());
    templateDb.addGroupToTemplate(template.getUuid(), group.getUuid(), UserRole.ADMIN, template.getName(), group.getName());
    templateDb.addGroupToTemplate(template.getUuid(), null, UserRole.ISSUE_ADMIN, template.getName(), null);

    List<String> resultWithUser = underTest.selectPotentialPermissionsByUserUuidAndTemplateUuid(dbSession, user.getUuid(),
      template.getUuid());
    List<String> resultWithoutUser = underTest.selectPotentialPermissionsByUserUuidAndTemplateUuid(dbSession, null, template.getUuid());

    assertThat(resultWithUser).containsOnlyOnce(GlobalPermission.SCAN.getKey(), UserRole.ADMIN, UserRole.USER, UserRole.CODEVIEWER,
      UserRole.ISSUE_ADMIN);
    // only permission from anyone group
    assertThat(resultWithoutUser).containsOnly(UserRole.ISSUE_ADMIN);
  }

  @Test
  void selectAllGroupPermissionTemplatesByGroupUuid() {
    PermissionTemplateDto permissionTemplate = templateDb.insertTemplate();
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    templateDb.addGroupToTemplate(permissionTemplate, group1, "user");
    templateDb.addGroupToTemplate(permissionTemplate, group1, "admin");
    templateDb.addGroupToTemplate(permissionTemplate, group2, "user");

    assertThat(db.getDbClient().permissionTemplateDao().selectAllGroupPermissionTemplatesByGroupUuid(db.getSession(), group1.getUuid()))
      .extracting(PermissionTemplateGroupDto::getGroupUuid, PermissionTemplateGroupDto::getPermission)
      .containsOnly(tuple(group1.getUuid(), "user"), tuple(group1.getUuid(), "admin"));
  }

  @Test
  void delete_user_permissions_by_user_uuid() {
    UserDto user = db.users().insertUser();
    UserDto anotherUser = db.users().insertUser();
    PermissionTemplateDto template = db.permissionTemplates().insertTemplate();
    String permission = "PERMISSION";
    db.permissionTemplates().addUserToTemplate(template.getUuid(), user.getUuid(), permission, template.getName(), user.getLogin());
    db.permissionTemplates().addUserToTemplate(template.getUuid(), anotherUser.getUuid(), permission, template.getName(),
      anotherUser.getLogin());

    underTest.deleteUserPermissionsByUserUuid(dbSession, user.getUuid(), user.getLogin());
    db.commit();

    assertThat(db.select("select template_uuid as \"templateUuid\", user_uuid as \"userUuid\", permission_reference as \"permission\" " +
      "from perm_templates_users"))
      .extracting((row) -> row.get("templateUuid"), (row) -> row.get("userUuid"), (row) -> row.get("permission"))
      .containsOnly(tuple(template.getUuid(), anotherUser.getUuid(), permission));
  }
}
