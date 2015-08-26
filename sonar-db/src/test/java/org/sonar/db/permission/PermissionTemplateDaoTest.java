/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.db.permission;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.MyBatis;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Category(DbTests.class)
public class PermissionTemplateDaoTest {

  System2 system = mock(System2.class);

  @Rule
  public DbTester dbTester = DbTester.create(system);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  PermissionTemplateDao underTest = dbTester.getDbClient().permissionTemplateDao();

  @Test
  public void should_create_permission_template() throws ParseException {
    dbTester.prepareDbUnit(getClass(), "createPermissionTemplate.xml");

    Date now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2013-01-02 01:04:05");
    when(system.now()).thenReturn(now.getTime());

    PermissionTemplateDto permissionTemplate = underTest.insert("my template", "my description", "myregexp");
    assertThat(permissionTemplate).isNotNull();
    assertThat(permissionTemplate.getId()).isEqualTo(1L);

    dbTester.assertDbUnitTable(getClass(), "createPermissionTemplate-result.xml", "permission_templates", "id", "name", "description");
  }

  @Test
  public void should_skip_key_normalization_on_default_template() {
    dbTester.truncateTables();

    PermissionTemplateMapper mapper = mock(PermissionTemplateMapper.class);

    DbSession session = mock(DbSession.class);
    when(session.getMapper(PermissionTemplateMapper.class)).thenReturn(mapper);

    MyBatis myBatis = mock(MyBatis.class);
    when(myBatis.openSession(false)).thenReturn(session);

    underTest = new PermissionTemplateDao(myBatis, system);
    PermissionTemplateDto permissionTemplate = underTest.insert(PermissionTemplateDto.DEFAULT.getName(), null, null);

    verify(mapper).insert(permissionTemplate);
    verify(session).commit();

    assertThat(permissionTemplate.getKee()).isEqualTo(PermissionTemplateDto.DEFAULT.getKee());
  }

  @Test
  public void should_select_permission_template() {
    dbTester.prepareDbUnit(getClass(), "selectPermissionTemplate.xml");

    PermissionTemplateDto permissionTemplate = underTest.selectPermissionTemplate("my_template_20130102_030405");

    assertThat(permissionTemplate).isNotNull();
    assertThat(permissionTemplate.getName()).isEqualTo("my template");
    assertThat(permissionTemplate.getKee()).isEqualTo("my_template_20130102_030405");
    assertThat(permissionTemplate.getDescription()).isEqualTo("my description");
    assertThat(permissionTemplate.getUsersPermissions()).hasSize(3);
    assertThat(permissionTemplate.getUsersPermissions()).extracting("userId").containsOnly(1L, 2L, 1L);
    assertThat(permissionTemplate.getUsersPermissions()).extracting("userLogin").containsOnly("login1", "login2", "login2");
    assertThat(permissionTemplate.getUsersPermissions()).extracting("userName").containsOnly("user1", "user2", "user2");
    assertThat(permissionTemplate.getUsersPermissions()).extracting("permission").containsOnly("user_permission1", "user_permission1", "user_permission2");
    assertThat(permissionTemplate.getGroupsPermissions()).hasSize(3);
    assertThat(permissionTemplate.getGroupsPermissions()).extracting("groupId").containsOnly(1L, 2L, null);
    assertThat(permissionTemplate.getGroupsPermissions()).extracting("groupName").containsOnly("group1", "group2", null);
    assertThat(permissionTemplate.getGroupsPermissions()).extracting("permission").containsOnly("group_permission1", "group_permission1", "group_permission2");
  }

  @Test
  public void should_select_empty_permission_template() {
    dbTester.prepareDbUnit(getClass(), "selectEmptyPermissionTemplate.xml");

    PermissionTemplateDto permissionTemplate = underTest.selectPermissionTemplate("my_template_20130102_030405");

    assertThat(permissionTemplate).isNotNull();
    assertThat(permissionTemplate.getName()).isEqualTo("my template");
    assertThat(permissionTemplate.getDescription()).isEqualTo("my description");
    assertThat(permissionTemplate.getUsersPermissions()).isNull();
    assertThat(permissionTemplate.getGroupsPermissions()).isNull();
  }

  @Test
  public void should_select_permission_template_by_key() {
    dbTester.prepareDbUnit(getClass(), "selectPermissionTemplate.xml");

    PermissionTemplateDto permissionTemplate = underTest.selectByKey("my_template_20130102_030405");

    assertThat(permissionTemplate).isNotNull();
    assertThat(permissionTemplate.getId()).isEqualTo(1L);
    assertThat(permissionTemplate.getName()).isEqualTo("my template");
    assertThat(permissionTemplate.getKee()).isEqualTo("my_template_20130102_030405");
    assertThat(permissionTemplate.getDescription()).isEqualTo("my description");
  }

  @Test
  public void should_select_all_permission_templates() {
    dbTester.prepareDbUnit(getClass(), "selectAllPermissionTemplates.xml");

    List<PermissionTemplateDto> permissionTemplates = underTest.selectAllPermissionTemplates();

    assertThat(permissionTemplates).hasSize(3);
    assertThat(permissionTemplates).extracting("id").containsOnly(1L, 2L, 3L);
    assertThat(permissionTemplates).extracting("name").containsOnly("template1", "template2", "template3");
    assertThat(permissionTemplates).extracting("kee").containsOnly("template1_20130102_030405", "template2_20130102_030405", "template3_20130102_030405");
    assertThat(permissionTemplates).extracting("description").containsOnly("description1", "description2", "description3");
  }

  @Test
  public void should_update_permission_template() {
    dbTester.prepareDbUnit(getClass(), "updatePermissionTemplate.xml");

    underTest.update(1L, "new_name", "new_description", "new_regexp");

    dbTester.assertDbUnitTable(getClass(), "updatePermissionTemplate-result.xml", "permission_templates", "id", "name", "kee", "description");
  }

  @Test
  public void should_delete_permission_template() {
    dbTester.prepareDbUnit(getClass(), "deletePermissionTemplate.xml");

    underTest.deletePermissionTemplate(1L);

    checkTemplateTables("deletePermissionTemplate-result.xml");
  }

  @Test
  public void should_add_user_permission_to_template() {
    dbTester.prepareDbUnit(getClass(), "addUserPermissionToTemplate.xml");

    underTest.insertUserPermission(1L, 1L, "new_permission");

    checkTemplateTables("addUserPermissionToTemplate-result.xml");
  }

  @Test
  public void should_remove_user_permission_from_template() {
    dbTester.prepareDbUnit(getClass(), "removeUserPermissionFromTemplate.xml");

    underTest.deleteUserPermission(1L, 2L, "permission_to_remove");

    checkTemplateTables("removeUserPermissionFromTemplate-result.xml");
  }

  @Test
  public void should_add_group_permission_to_template() {
    dbTester.prepareDbUnit(getClass(), "addGroupPermissionToTemplate.xml");

    underTest.insertGroupPermission(1L, 1L, "new_permission");

    checkTemplateTables("addGroupPermissionToTemplate-result.xml");
  }

  @Test
  public void should_remove_group_permission_from_template() {
    dbTester.prepareDbUnit(getClass(), "removeGroupPermissionFromTemplate.xml");

    underTest.deleteGroupPermission(1L, 2L, "permission_to_remove");

    checkTemplateTables("removeGroupPermissionFromTemplate-result.xml");
  }

  @Test
  public void remove_by_group() {
    dbTester.prepareDbUnit(getClass(), "remove_by_group.xml");

    underTest.deleteByGroup(dbTester.getSession(), 2L);
    dbTester.getSession().commit();

    dbTester.assertDbUnitTable(getClass(), "remove_by_group-result.xml", "permission_templates", "id", "name", "kee", "description");
  }

  @Test
  public void should_add_group_permission_with_null_name() {
    dbTester.prepareDbUnit(getClass(), "addNullGroupPermissionToTemplate.xml");

    underTest.insertGroupPermission(1L, null, "new_permission");

    checkTemplateTables("addNullGroupPermissionToTemplate-result.xml");
  }

  @Test
  public void should_remove_group_permission_with_null_name() {
    dbTester.prepareDbUnit(getClass(), "removeNullGroupPermissionFromTemplate.xml");

    underTest.deleteGroupPermission(1L, null, "permission_to_remove");

    checkTemplateTables("removeNullGroupPermissionFromTemplate-result.xml");
  }

  @Test
  public void should_retrieve_permission_template() {
    dbTester.truncateTables();

    PermissionTemplateDto permissionTemplateDto = new PermissionTemplateDto().setName("Test template").setKee("test_template");
    PermissionTemplateDto templateWithPermissions = new PermissionTemplateDto().setKee("test_template");
    underTest = mock(PermissionTemplateDao.class);
    when(underTest.selectByKey(dbTester.getSession(), "test_template")).thenReturn(permissionTemplateDto);
    when(underTest.selectPermissionTemplate(dbTester.getSession(), "test_template")).thenReturn(templateWithPermissions);
    when(underTest.selectPermissionTemplateWithPermissions(dbTester.getSession(), "test_template")).thenCallRealMethod();

    PermissionTemplateDto permissionTemplate = underTest.selectPermissionTemplateWithPermissions(dbTester.getSession(), "test_template");

    assertThat(permissionTemplate).isSameAs(templateWithPermissions);
  }

  @Test
  public void should_fail_on_unmatched_template() {
    dbTester.truncateTables();

    expectedException.expect(IllegalArgumentException.class);

    underTest.selectPermissionTemplateWithPermissions(dbTester.getSession(), "unmatched");
  }

  private void checkTemplateTables(String fileName) {
    dbTester.assertDbUnitTable(getClass(), fileName, "permission_templates", "id", "name", "description");
    dbTester.assertDbUnitTable(getClass(), fileName, "perm_templates_users", "id", "template_id", "user_id", "permission_reference");
    dbTester.assertDbUnitTable(getClass(), fileName, "perm_templates_groups", "id", "template_id", "group_id", "permission_reference");
  }

}
