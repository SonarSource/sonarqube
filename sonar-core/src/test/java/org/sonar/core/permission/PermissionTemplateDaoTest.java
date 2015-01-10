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

package org.sonar.core.permission;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class PermissionTemplateDaoTest extends AbstractDaoTestCase {

  Date now;
  PermissionTemplateDao permissionTemplateDao;
  DbSession session;
  System2 system = mock(System2.class);

  @Before
  public void setUpDao() throws ParseException {
    session = getMyBatis().openSession(false);
    now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2013-01-02 01:04:05");
    when(system.now()).thenReturn(now.getTime());
    permissionTemplateDao = new PermissionTemplateDao(getMyBatis(), system);
  }

  @After
  public void after() {
    this.session.close();
  }

  @Test
  public void should_create_permission_template() throws Exception {
    setupData("createPermissionTemplate");
    PermissionTemplateDto permissionTemplate = permissionTemplateDao.createPermissionTemplate("my template", "my description", "myregexp");
    assertThat(permissionTemplate).isNotNull();
    assertThat(permissionTemplate.getId()).isEqualTo(1L);
    checkTable("createPermissionTemplate", "permission_templates", "id", "name", "kee", "description");
  }

  @Test
  public void should_normalize_kee_on_template_creation() throws Exception {
    setupData("createNonAsciiPermissionTemplate");
    PermissionTemplateDto permissionTemplate = permissionTemplateDao.createPermissionTemplate("Môü Gnô Gnèçàß", "my description", null);
    assertThat(permissionTemplate).isNotNull();
    assertThat(permissionTemplate.getId()).isEqualTo(1L);
    checkTable("createNonAsciiPermissionTemplate", "permission_templates", "id", "name", "kee", "description");
  }

  @Test
  public void should_skip_key_normalization_on_default_template() throws Exception {

    PermissionTemplateMapper mapper = mock(PermissionTemplateMapper.class);

    DbSession session = mock(DbSession.class);
    when(session.getMapper(PermissionTemplateMapper.class)).thenReturn(mapper);

    MyBatis myBatis = mock(MyBatis.class);
    when(myBatis.openSession(false)).thenReturn(session);

    permissionTemplateDao = new PermissionTemplateDao(myBatis, system);
    PermissionTemplateDto permissionTemplate = permissionTemplateDao.createPermissionTemplate(PermissionTemplateDto.DEFAULT.getName(), null, null);

    verify(mapper).insert(permissionTemplate);
    verify(session).commit();

    assertThat(permissionTemplate.getKee()).isEqualTo(PermissionTemplateDto.DEFAULT.getKee());
  }

  @Test
  public void should_select_permission_template() throws Exception {
    setupData("selectPermissionTemplate");
    PermissionTemplateDto permissionTemplate = permissionTemplateDao.selectPermissionTemplate("my_template_20130102_030405");

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
  public void should_select_empty_permission_template() throws Exception {
    setupData("selectEmptyPermissionTemplate");
    PermissionTemplateDto permissionTemplate = permissionTemplateDao.selectPermissionTemplate("my_template_20130102_030405");

    assertThat(permissionTemplate).isNotNull();
    assertThat(permissionTemplate.getName()).isEqualTo("my template");
    assertThat(permissionTemplate.getDescription()).isEqualTo("my description");
    assertThat(permissionTemplate.getUsersPermissions()).isNull();
    assertThat(permissionTemplate.getGroupsPermissions()).isNull();
  }

  @Test
  public void should_select_permission_template_by_key() throws Exception {
    setupData("selectPermissionTemplate");

    PermissionTemplateDto permissionTemplate = permissionTemplateDao.selectTemplateByKey("my_template_20130102_030405");

    assertThat(permissionTemplate).isNotNull();
    assertThat(permissionTemplate.getId()).isEqualTo(1L);
    assertThat(permissionTemplate.getName()).isEqualTo("my template");
    assertThat(permissionTemplate.getKee()).isEqualTo("my_template_20130102_030405");
    assertThat(permissionTemplate.getDescription()).isEqualTo("my description");
  }

  @Test
  public void should_select_all_permission_templates() throws Exception {
    setupData("selectAllPermissionTemplates");

    List<PermissionTemplateDto> permissionTemplates = permissionTemplateDao.selectAllPermissionTemplates();

    assertThat(permissionTemplates).hasSize(3);
    assertThat(permissionTemplates).extracting("id").containsOnly(1L, 2L, 3L);
    assertThat(permissionTemplates).extracting("name").containsOnly("template1", "template2", "template3");
    assertThat(permissionTemplates).extracting("kee").containsOnly("template1_20130102_030405", "template2_20130102_030405", "template3_20130102_030405");
    assertThat(permissionTemplates).extracting("description").containsOnly("description1", "description2", "description3");
  }

  @Test
  public void should_update_permission_template() throws Exception {
    setupData("updatePermissionTemplate");

    permissionTemplateDao.updatePermissionTemplate(1L, "new_name", "new_description", "new_regexp");

    checkTable("updatePermissionTemplate", "permission_templates", "id", "name", "kee", "description");
  }

  @Test
  public void should_delete_permission_template() throws Exception {
    setupData("deletePermissionTemplate");

    permissionTemplateDao.deletePermissionTemplate(1L);

    checkTable("deletePermissionTemplate", "permission_templates", "id", "name", "description");
    checkTable("deletePermissionTemplate", "perm_templates_users", "id", "template_id", "user_id", "permission_reference");
    checkTable("deletePermissionTemplate", "perm_templates_groups", "id", "template_id", "group_id", "permission_reference");
  }

  @Test
  public void should_add_user_permission_to_template() throws Exception {
    setupData("addUserPermissionToTemplate");
    permissionTemplateDao.addUserPermission(1L, 1L, "new_permission");

    checkTable("addUserPermissionToTemplate", "permission_templates", "id", "name", "description");
    checkTable("addUserPermissionToTemplate", "perm_templates_users", "id", "template_id", "user_id", "permission_reference");
    checkTable("addUserPermissionToTemplate", "perm_templates_groups", "id", "template_id", "group_id", "permission_reference");
  }

  @Test
  public void should_remove_user_permission_from_template() throws Exception {
    setupData("removeUserPermissionFromTemplate");
    permissionTemplateDao.removeUserPermission(1L, 2L, "permission_to_remove");

    checkTable("removeUserPermissionFromTemplate", "permission_templates", "id", "name", "description");
    checkTable("removeUserPermissionFromTemplate", "perm_templates_users", "id", "template_id", "user_id", "permission_reference");
    checkTable("removeUserPermissionFromTemplate", "perm_templates_groups", "id", "template_id", "group_id", "permission_reference");
  }

  @Test
  public void should_add_group_permission_to_template() throws Exception {
    setupData("addGroupPermissionToTemplate");
    permissionTemplateDao.addGroupPermission(1L, 1L, "new_permission");

    checkTable("addGroupPermissionToTemplate", "permission_templates", "id", "name", "description");
    checkTable("addGroupPermissionToTemplate", "perm_templates_users", "id", "template_id", "user_id", "permission_reference");
    checkTable("addGroupPermissionToTemplate", "perm_templates_groups", "id", "template_id", "group_id", "permission_reference");
  }

  @Test
  public void should_remove_group_permission_from_template() throws Exception {
    setupData("removeGroupPermissionFromTemplate");
    permissionTemplateDao.removeGroupPermission(1L, 2L, "permission_to_remove");

    checkTable("removeGroupPermissionFromTemplate", "permission_templates", "id", "name", "description");
    checkTable("removeGroupPermissionFromTemplate", "perm_templates_users", "id", "template_id", "user_id", "permission_reference");
    checkTable("removeGroupPermissionFromTemplate", "perm_templates_groups", "id", "template_id", "group_id", "permission_reference");
  }

  @Test
  public void remove_by_group() throws Exception {
    setupData("remove_by_group");
    permissionTemplateDao.removeByGroup(2L, session);
    session.commit();

    checkTable("remove_by_group", "perm_templates_groups", "id", "template_id", "group_id", "permission_reference");
  }

  @Test
  public void should_add_group_permission_with_null_name() throws Exception {
    setupData("addNullGroupPermissionToTemplate");
    permissionTemplateDao.addGroupPermission(1L, null, "new_permission");

    checkTable("addNullGroupPermissionToTemplate", "permission_templates", "id", "name", "description");
    checkTable("addNullGroupPermissionToTemplate", "perm_templates_users", "id", "template_id", "user_id", "permission_reference");
    checkTable("addNullGroupPermissionToTemplate", "perm_templates_groups", "id", "template_id", "group_id", "permission_reference");
  }

  @Test
  public void should_remove_group_permission_with_null_name() throws Exception {
    setupData("removeNullGroupPermissionFromTemplate");
    permissionTemplateDao.removeGroupPermission(1L, null, "permission_to_remove");

    checkTable("removeNullGroupPermissionFromTemplate", "permission_templates", "id", "name", "description");
    checkTable("removeNullGroupPermissionFromTemplate", "perm_templates_users", "id", "template_id", "user_id", "permission_reference");
    checkTable("removeNullGroupPermissionFromTemplate", "perm_templates_groups", "id", "template_id", "group_id", "permission_reference");
  }
}
