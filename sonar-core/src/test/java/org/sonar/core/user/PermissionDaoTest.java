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

package org.sonar.core.user;

import org.junit.Before;
import org.junit.Test;
import org.sonar.core.persistence.AbstractDaoTestCase;

import static org.fest.assertions.Assertions.assertThat;

public class PermissionDaoTest extends AbstractDaoTestCase {

  private PermissionDao permissionDao;

  @Before
  public void setUpDao() {
    permissionDao = new PermissionDao(getMyBatis());
  }

  @Test
  public void should_create_permission_template() throws Exception {
    setupData("createPermissionTemplate");
    PermissionTemplateDto permissionTemplate = permissionDao.createPermissionTemplate("my template", "my description");
    assertThat(permissionTemplate).isNotNull();
    assertThat(permissionTemplate.getId()).isEqualTo(1L);
    checkTable("createPermissionTemplate", "permission_templates", "id", "name", "description");
  }

  @Test
  public void should_select_permission_template() throws Exception {
    setupData("selectPermissionTemplate");
    PermissionTemplateDto permissionTemplate = permissionDao.selectPermissionTemplate("my template");

    assertThat(permissionTemplate).isNotNull();
    assertThat(permissionTemplate.getName()).isEqualTo("my template");
    assertThat(permissionTemplate.getDescription()).isEqualTo("my description");
  }

  @Test
  public void should_add_user_permission_to_template() throws Exception {
    setupData("addUserPermissionToTemplate");
    permissionDao.addUserPermission(1L, 1L);

    checkTable("addUserPermissionToTemplate", "permission_templates", "id", "name", "description");
    checkTable("addUserPermissionToTemplate", "perm_templates_users", "id", "template_id", "user_id");
    checkTable("addUserPermissionToTemplate", "perm_templates_groups", "id", "template_id", "group_id");
  }

  @Test
  public void should_remove_user_permission_from_template() throws Exception {
    setupData("removeUserPermissionFromTemplate");
    permissionDao.removeUserPermission(1L, 2L);

    checkTable("removeUserPermissionFromTemplate", "permission_templates", "id", "name", "description");
    checkTable("removeUserPermissionFromTemplate", "perm_templates_users", "id", "template_id", "user_id");
    checkTable("removeUserPermissionFromTemplate", "perm_templates_groups", "id", "template_id", "group_id");
  }

  @Test
  public void should_add_group_permission_to_template() throws Exception {
    setupData("addGroupPermissionToTemplate");
    permissionDao.addGroupPermission(1L, 1L);

    checkTable("addGroupPermissionToTemplate", "permission_templates", "id", "name", "description");
    checkTable("addGroupPermissionToTemplate", "perm_templates_users", "id", "template_id", "user_id");
    checkTable("addGroupPermissionToTemplate", "perm_templates_groups", "id", "template_id", "group_id");
  }

  @Test
  public void should_remove_group_permission_from_template() throws Exception {
    setupData("removeGroupPermissionFromTemplate");
    permissionDao.removeGroupPermission(1L, 2L);

    checkTable("removeGroupPermissionFromTemplate", "permission_templates", "id", "name", "description");
    checkTable("removeGroupPermissionFromTemplate", "perm_templates_users", "id", "template_id", "user_id");
    checkTable("removeGroupPermissionFromTemplate", "perm_templates_groups", "id", "template_id", "group_id");
  }

  @Test
  public void should_add_group_permission_with_null_name() throws Exception {
    setupData("addNullGroupPermissionToTemplate");
    permissionDao.addGroupPermission(1L, null);

    checkTable("addNullGroupPermissionToTemplate", "permission_templates", "id", "name", "description");
    checkTable("addNullGroupPermissionToTemplate", "perm_templates_users", "id", "template_id", "user_id");
    checkTable("addNullGroupPermissionToTemplate", "perm_templates_groups", "id", "template_id", "group_id");
  }

  @Test
  public void should_remove_group_permission_with_null_name() throws Exception {
    setupData("removeNullGroupPermissionFromTemplate");
    permissionDao.removeGroupPermission(1L, null);

    checkTable("removeNullGroupPermissionFromTemplate", "permission_templates", "id", "name", "description");
    checkTable("removeNullGroupPermissionFromTemplate", "perm_templates_users", "id", "template_id", "user_id");
    checkTable("removeNullGroupPermissionFromTemplate", "perm_templates_groups", "id", "template_id", "group_id");
  }
}
