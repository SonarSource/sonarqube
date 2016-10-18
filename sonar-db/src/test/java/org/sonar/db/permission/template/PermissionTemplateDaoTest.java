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
package org.sonar.db.permission.template;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;

import static org.assertj.core.api.Assertions.assertThat;
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

  private System2 system = mock(System2.class);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create(system);
  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();
  private PermissionTemplateDbTester templateDb = db.permissionTemplates();

  private PermissionTemplateDao underTest = new PermissionTemplateDao(system);

  @Test
  public void should_create_permission_template() throws ParseException {
    db.prepareDbUnit(getClass(), "createPermissionTemplate.xml");

    Date now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2013-01-02 01:04:05");
    when(system.now()).thenReturn(now.getTime());

    PermissionTemplateDto permissionTemplate = underTest.insert(db.getSession(), newPermissionTemplateDto()
      .setName("my template")
      .setDescription("my description")
      .setKeyPattern("myregexp"));
    assertThat(permissionTemplate).isNotNull();
    assertThat(permissionTemplate.getId()).isEqualTo(1L);

    db.assertDbUnitTable(getClass(), "createPermissionTemplate-result.xml", "permission_templates", "id", "name", "description");
  }

  @Test
  public void should_select_permission_template_by_key() {
    db.prepareDbUnit(getClass(), "selectPermissionTemplate.xml");

    PermissionTemplateDto permissionTemplate = underTest.selectByUuid(dbSession, "my_template_20130102_030405");

    assertThat(permissionTemplate).isNotNull();
    assertThat(permissionTemplate.getId()).isEqualTo(1L);
    assertThat(permissionTemplate.getName()).isEqualTo("my template");
    assertThat(permissionTemplate.getUuid()).isEqualTo("my_template_20130102_030405");
    assertThat(permissionTemplate.getDescription()).isEqualTo("my description");
  }

  @Test
  public void should_select_all_permission_templates() {
    db.prepareDbUnit(getClass(), "selectAllPermissionTemplates.xml");
    commit();

    List<PermissionTemplateDto> permissionTemplates = underTest.selectAll(dbSession);

    assertThat(permissionTemplates).hasSize(3);
    assertThat(permissionTemplates).extracting("id").containsOnly(1L, 2L, 3L);
    assertThat(permissionTemplates).extracting("name").containsOnly("template1", "template2", "template3");
    assertThat(permissionTemplates).extracting("kee").containsOnly("template1_20130102_030405", "template2_20130102_030405", "template3_20130102_030405");
    assertThat(permissionTemplates).extracting("description").containsOnly("description1", "description2", "description3");
  }

  @Test
  public void should_update_permission_template() {
    db.prepareDbUnit(getClass(), "updatePermissionTemplate.xml");

    PermissionTemplateDto dto = new PermissionTemplateDto()
      .setId(1L)
      .setName("new_name")
      .setDescription("new_description")
      .setKeyPattern("new_regexp");
    underTest.update(dbSession, dto);

    db.assertDbUnitTable(getClass(), "updatePermissionTemplate-result.xml", "permission_templates", "id", "name", "kee", "description");
  }

  @Test
  public void should_delete_permission_template() {
    db.prepareDbUnit(getClass(), "deletePermissionTemplate.xml");

    underTest.deleteById(dbSession, 1L);
    dbSession.commit();

    checkTemplateTables("deletePermissionTemplate-result.xml");
    db.assertDbUnitTable(getClass(), "deletePermissionTemplate-result.xml", "perm_tpl_characteristics");
  }

  @Test
  public void should_add_user_permission_to_template() {
    db.prepareDbUnit(getClass(), "addUserPermissionToTemplate.xml");

    underTest.insertUserPermission(dbSession, 1L, 1L, "new_permission");

    checkTemplateTables("addUserPermissionToTemplate-result.xml");
  }

  @Test
  public void should_remove_user_permission_from_template() {
    db.prepareDbUnit(getClass(), "removeUserPermissionFromTemplate.xml");

    underTest.deleteUserPermission(dbSession, 1L, 2L, "permission_to_remove");

    checkTemplateTables("removeUserPermissionFromTemplate-result.xml");
  }

  @Test
  public void should_add_group_permission_to_template() {
    db.prepareDbUnit(getClass(), "addGroupPermissionToTemplate.xml");

    underTest.insertGroupPermission(dbSession, 1L, 1L, "new_permission");
    dbSession.commit();

    checkTemplateTables("addGroupPermissionToTemplate-result.xml");
  }

  @Test
  public void remove_by_group() {
    db.prepareDbUnit(getClass(), "remove_by_group.xml");

    underTest.deleteByGroup(db.getSession(), 2L);
    db.getSession().commit();

    db.assertDbUnitTable(getClass(), "remove_by_group-result.xml", "permission_templates", "id", "name", "kee", "description");
  }

  @Test
  public void should_add_group_permission_with_null_name() {
    db.prepareDbUnit(getClass(), "addNullGroupPermissionToTemplate.xml");

    underTest.insertGroupPermission(dbSession, 1L, null, "new_permission");
    dbSession.commit();

    checkTemplateTables("addNullGroupPermissionToTemplate-result.xml");
  }

  @Test
  public void group_count_by_template_and_permission() {
    PermissionTemplateDto template1 = templateDb.insertTemplate();
    PermissionTemplateDto template2 = templateDb.insertTemplate();
    PermissionTemplateDto template3 = templateDb.insertTemplate();

    GroupDto group1 = db.users().insertGroup(newGroupDto());
    GroupDto group2 = db.users().insertGroup(newGroupDto());
    GroupDto group3 = db.users().insertGroup(newGroupDto());

    templateDb.addGroupToTemplate(42L, group1.getId(), ISSUE_ADMIN);
    templateDb.addGroupToTemplate(template1.getId(), group1.getId(), CODEVIEWER);
    templateDb.addGroupToTemplate(template1.getId(), group2.getId(), CODEVIEWER);
    templateDb.addGroupToTemplate(template1.getId(), group3.getId(), CODEVIEWER);
    templateDb.addGroupToTemplate(template1.getId(), null, CODEVIEWER);
    templateDb.addGroupToTemplate(template1.getId(), group1.getId(), ADMIN);
    templateDb.addGroupToTemplate(template2.getId(), group1.getId(), ADMIN);

    final List<CountByTemplateAndPermissionDto> result = new ArrayList<>();
    underTest.groupsCountByTemplateIdAndPermission(dbSession, Arrays.asList(template1.getId(), template2.getId(), template3.getId()), new ResultHandler() {
      @Override
      public void handleResult(ResultContext context) {
        result.add((CountByTemplateAndPermissionDto) context.getResultObject());
      }
    });

    assertThat(result).hasSize(3);
    assertThat(result).extracting("permission").containsOnly(ADMIN, CODEVIEWER);
    assertThat(result).extracting("templateId").containsOnly(template1.getId(), template2.getId());
    assertThat(result).extracting("count").containsOnly(4, 1);
  }

  @Test
  public void user_count_by_template_and_permission() {
    PermissionTemplateDto template1 = templateDb.insertTemplate();
    PermissionTemplateDto template2 = templateDb.insertTemplate();
    PermissionTemplateDto template3 = templateDb.insertTemplate();

    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    UserDto user3 = db.users().insertUser();

    templateDb.addUserToTemplate(42L, user1.getId(), ISSUE_ADMIN);
    templateDb.addUserToTemplate(template1.getId(), user1.getId(), ADMIN);
    templateDb.addUserToTemplate(template1.getId(), user2.getId(), ADMIN);
    templateDb.addUserToTemplate(template1.getId(), user3.getId(), ADMIN);
    templateDb.addUserToTemplate(template1.getId(), user1.getId(), USER);
    templateDb.addUserToTemplate(template2.getId(), user1.getId(), USER);

    final List<CountByTemplateAndPermissionDto> result = new ArrayList<>();
    underTest.usersCountByTemplateIdAndPermission(dbSession, Arrays.asList(template1.getId(), template2.getId(), template3.getId()),
      context -> result.add((CountByTemplateAndPermissionDto) context.getResultObject()));
    assertThat(result).hasSize(3);
    assertThat(result).extracting("permission").containsOnly(ADMIN, USER);
    assertThat(result).extracting("templateId").containsOnly(template1.getId(), template2.getId());
    assertThat(result).extracting("count").containsOnly(3, 1);
  }

  @Test
  public void select_by_name_query_and_pagination() {
    templateDb.insertTemplate(newPermissionTemplateDto().setName("aaabbb"));
    templateDb.insertTemplate(newPermissionTemplateDto().setName("aaaccc"));

    List<PermissionTemplateDto> templates = underTest.selectAll(dbSession, "aaa");

    assertThat(templates.get(0).getName()).isEqualTo("aaabbb");
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

  private void commit() {
    dbSession.commit();
  }

  private void checkTemplateTables(String fileName) {
    db.assertDbUnitTable(getClass(), fileName, "permission_templates", "id", "name", "description");
    db.assertDbUnitTable(getClass(), fileName, "perm_templates_users", "id", "template_id", "user_id", "permission_reference");
    db.assertDbUnitTable(getClass(), fileName, "perm_templates_groups", "id", "template_id", "group_id", "permission_reference");
  }

}
