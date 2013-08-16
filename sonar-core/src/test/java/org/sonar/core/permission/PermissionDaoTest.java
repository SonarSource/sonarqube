/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

import org.apache.ibatis.session.SqlSession;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.date.DateProvider;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.persistence.MyBatis;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class PermissionDaoTest extends AbstractDaoTestCase {

  Date now;
  PermissionDao permissionDao;
  DateProvider dateProvider;

  @Before
  public void setUpDao() throws ParseException {
    now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2013-01-02 01:04:05");
    dateProvider = mock(DateProvider.class);
    stub(dateProvider.now()).toReturn(now);
    permissionDao = new PermissionDao(getMyBatis(), dateProvider);
  }

  @Test
  public void should_create_permission_template() throws Exception {
    setupData("createPermissionTemplate");
    PermissionTemplateDto permissionTemplate = permissionDao.createPermissionTemplate("my template", "my description");
    assertThat(permissionTemplate).isNotNull();
    assertThat(permissionTemplate.getId()).isEqualTo(1L);
    checkTable("createPermissionTemplate", "permission_templates", "id", "name", "kee", "description");
  }

  @Test
  public void should_normalize_kee_on_template_creation() throws Exception {
    setupData("createNonAsciiPermissionTemplate");
    PermissionTemplateDto permissionTemplate = permissionDao.createPermissionTemplate("Môü Gnô Gnèçàß", "my description");
    assertThat(permissionTemplate).isNotNull();
    assertThat(permissionTemplate.getId()).isEqualTo(1L);
    checkTable("createNonAsciiPermissionTemplate", "permission_templates", "id", "name", "kee", "description");
  }

  @Test
  public void should_skip_key_normalization_on_default_template() throws Exception {

    PermissionTemplateMapper mapper = mock(PermissionTemplateMapper.class);

    SqlSession session = mock(SqlSession.class);
    when(session.getMapper(PermissionTemplateMapper.class)).thenReturn(mapper);

    MyBatis myBatis = mock(MyBatis.class);
    when(myBatis.openSession()).thenReturn(session);

    permissionDao = new PermissionDao(myBatis, dateProvider);
    PermissionTemplateDto permissionTemplate = permissionDao.createPermissionTemplate(PermissionTemplateDto.DEFAULT.getName(), null);

    verify(mapper).insert(permissionTemplate);
    verify(session).commit();

    assertThat(permissionTemplate.getKee()).isEqualTo(PermissionTemplateDto.DEFAULT.getKee());
  }

  @Test
  public void should_select_permission_template() throws Exception {
    setupData("selectPermissionTemplate");
    PermissionTemplateDto permissionTemplate = permissionDao.selectPermissionTemplate("my template");

    assertThat(permissionTemplate).isNotNull();
    assertThat(permissionTemplate.getName()).isEqualTo("my template");
    assertThat(permissionTemplate.getKee()).isEqualTo("my_template_20130102_030405");
    assertThat(permissionTemplate.getDescription()).isEqualTo("my description");
    assertThat(permissionTemplate.getUsersPermissions()).hasSize(3);
    assertThat(permissionTemplate.getUsersPermissions()).onProperty("userId").containsOnly(1L, 2L, 1L);
    assertThat(permissionTemplate.getUsersPermissions()).onProperty("userLogin").containsOnly("login1", "login2", "login2");
    assertThat(permissionTemplate.getUsersPermissions()).onProperty("userName").containsOnly("user1", "user2", "user2");
    assertThat(permissionTemplate.getUsersPermissions()).onProperty("permission").containsOnly("user_permission1", "user_permission1", "user_permission2");
    assertThat(permissionTemplate.getGroupsPermissions()).hasSize(3);
    assertThat(permissionTemplate.getGroupsPermissions()).onProperty("groupId").containsOnly(1L, 2L, null);
    assertThat(permissionTemplate.getGroupsPermissions()).onProperty("groupName").containsOnly("group1", "group2", null);
    assertThat(permissionTemplate.getGroupsPermissions()).onProperty("permission").containsOnly("group_permission1", "group_permission1", "group_permission2");
  }

  @Test
  public void should_select_empty_permission_template() throws Exception {
    setupData("selectEmptyPermissionTemplate");
    PermissionTemplateDto permissionTemplate = permissionDao.selectPermissionTemplate("my template");

    assertThat(permissionTemplate).isNotNull();
    assertThat(permissionTemplate.getName()).isEqualTo("my template");
    assertThat(permissionTemplate.getDescription()).isEqualTo("my description");
    assertThat(permissionTemplate.getUsersPermissions()).isNull();
    assertThat(permissionTemplate.getGroupsPermissions()).isNull();
  }

  @Test
  public void should_select_permission_template_by_name() throws Exception {
    setupData("selectPermissionTemplate");

    PermissionTemplateDto permissionTemplate = permissionDao.selectTemplateByName("my template");

    assertThat(permissionTemplate).isNotNull();
    assertThat(permissionTemplate.getId()).isEqualTo(1L);
    assertThat(permissionTemplate.getName()).isEqualTo("my template");
    assertThat(permissionTemplate.getKee()).isEqualTo("my_template_20130102_030405");
    assertThat(permissionTemplate.getDescription()).isEqualTo("my description");
  }

  @Test
  public void should_select_permission_template_by_key() throws Exception {
    setupData("selectPermissionTemplate");

    PermissionTemplateDto permissionTemplate = permissionDao.selectTemplateByKey("my_template_20130102_030405");

    assertThat(permissionTemplate).isNotNull();
    assertThat(permissionTemplate.getId()).isEqualTo(1L);
    assertThat(permissionTemplate.getName()).isEqualTo("my template");
    assertThat(permissionTemplate.getKee()).isEqualTo("my_template_20130102_030405");
    assertThat(permissionTemplate.getDescription()).isEqualTo("my description");
  }

  @Test
  public void should_select_all_permission_templates() throws Exception {
    setupData("selectAllPermissionTemplates");

    List<PermissionTemplateDto> permissionTemplates = permissionDao.selectAllPermissionTemplates();

    assertThat(permissionTemplates).hasSize(3);
    assertThat(permissionTemplates).onProperty("id").containsOnly(1L, 2L, 3L);
    assertThat(permissionTemplates).onProperty("name").containsOnly("template1", "template2", "template3");
    assertThat(permissionTemplates).onProperty("kee").containsOnly("template1_20130102_030405", "template2_20130102_030405", "template3_20130102_030405");
    assertThat(permissionTemplates).onProperty("description").containsOnly("description1", "description2", "description3");
  }

  @Test
  public void should_update_permission_template() throws Exception {
    setupData("updatePermissionTemplate");

    permissionDao.updatePermissionTemplate(1L, "new_name", "new_description");

    checkTable("updatePermissionTemplate", "permission_templates", "id", "name", "kee", "description");
  }

  @Test
  public void should_delete_permission_template() throws Exception {
    setupData("deletePermissionTemplate");

    permissionDao.deletePermissionTemplate(1L);

    checkTable("deletePermissionTemplate", "permission_templates", "id", "name", "description");
    checkTable("deletePermissionTemplate", "perm_templates_users", "id", "template_id", "user_id", "permission_reference");
    checkTable("deletePermissionTemplate", "perm_templates_groups", "id", "template_id", "group_id", "permission_reference");
  }

  @Test
  public void should_add_user_permission_to_template() throws Exception {
    setupData("addUserPermissionToTemplate");
    permissionDao.addUserPermission(1L, 1L, "new_permission");

    checkTable("addUserPermissionToTemplate", "permission_templates", "id", "name", "description");
    checkTable("addUserPermissionToTemplate", "perm_templates_users", "id", "template_id", "user_id", "permission_reference");
    checkTable("addUserPermissionToTemplate", "perm_templates_groups", "id", "template_id", "group_id", "permission_reference");
  }

  @Test
  public void should_remove_user_permission_from_template() throws Exception {
    setupData("removeUserPermissionFromTemplate");
    permissionDao.removeUserPermission(1L, 2L, "permission_to_remove");

    checkTable("removeUserPermissionFromTemplate", "permission_templates", "id", "name", "description");
    checkTable("removeUserPermissionFromTemplate", "perm_templates_users", "id", "template_id", "user_id", "permission_reference");
    checkTable("removeUserPermissionFromTemplate", "perm_templates_groups", "id", "template_id", "group_id", "permission_reference");
  }

  @Test
  public void should_add_group_permission_to_template() throws Exception {
    setupData("addGroupPermissionToTemplate");
    permissionDao.addGroupPermission(1L, 1L, "new_permission");

    checkTable("addGroupPermissionToTemplate", "permission_templates", "id", "name", "description");
    checkTable("addGroupPermissionToTemplate", "perm_templates_users", "id", "template_id", "user_id", "permission_reference");
    checkTable("addGroupPermissionToTemplate", "perm_templates_groups", "id", "template_id", "group_id", "permission_reference");
  }

  @Test
  public void should_remove_group_permission_from_template() throws Exception {
    setupData("removeGroupPermissionFromTemplate");
    permissionDao.removeGroupPermission(1L, 2L, "permission_to_remove");

    checkTable("removeGroupPermissionFromTemplate", "permission_templates", "id", "name", "description");
    checkTable("removeGroupPermissionFromTemplate", "perm_templates_users", "id", "template_id", "user_id", "permission_reference");
    checkTable("removeGroupPermissionFromTemplate", "perm_templates_groups", "id", "template_id", "group_id", "permission_reference");
  }

  @Test
  public void should_add_group_permission_with_null_name() throws Exception {
    setupData("addNullGroupPermissionToTemplate");
    permissionDao.addGroupPermission(1L, null, "new_permission");

    checkTable("addNullGroupPermissionToTemplate", "permission_templates", "id", "name", "description");
    checkTable("addNullGroupPermissionToTemplate", "perm_templates_users", "id", "template_id", "user_id", "permission_reference");
    checkTable("addNullGroupPermissionToTemplate", "perm_templates_groups", "id", "template_id", "group_id", "permission_reference");
  }

  @Test
  public void should_remove_group_permission_with_null_name() throws Exception {
    setupData("removeNullGroupPermissionFromTemplate");
    permissionDao.removeGroupPermission(1L, null, "permission_to_remove");

    checkTable("removeNullGroupPermissionFromTemplate", "permission_templates", "id", "name", "description");
    checkTable("removeNullGroupPermissionFromTemplate", "perm_templates_users", "id", "template_id", "user_id", "permission_reference");
    checkTable("removeNullGroupPermissionFromTemplate", "perm_templates_groups", "id", "template_id", "group_id", "permission_reference");
  }
}
