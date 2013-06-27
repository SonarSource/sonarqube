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

package org.sonar.server.permission;

import com.google.common.collect.Maps;
import org.junit.Test;
import org.sonar.core.user.Permissions;

import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PermissionChangeQueryTest {

  @Test
  public void should_populate_from_params() throws Exception {

    Map<String, Object> params = Maps.newHashMap();
    params.put("user", "my_login");
    params.put("group", "my_group");
    params.put("permission", Permissions.SYSTEM_ADMIN);

    PermissionChangeQuery query = PermissionChangeQuery.buildFromParams(params);

    assertThat(query.getUser()).isEqualTo("my_login");
    assertThat(query.getGroup()).isEqualTo("my_group");
    assertThat(query.getRole()).isEqualTo(Permissions.SYSTEM_ADMIN);
  }

  @Test
  public void should_validate_user_query() throws Exception {

    Map<String, Object> validUserParams = Maps.newHashMap();
    validUserParams.put("user", "my_login");
    validUserParams.put("permission", Permissions.SYSTEM_ADMIN);

    PermissionChangeQuery query = PermissionChangeQuery.buildFromParams(validUserParams);
    assertTrue(query.isValid());
  }

  @Test
  public void should_validate_group_query() throws Exception {

    Map<String, Object> validGroupParams = Maps.newHashMap();
    validGroupParams.put("group", "my_group");
    validGroupParams.put("permission", Permissions.SYSTEM_ADMIN);

    PermissionChangeQuery query = PermissionChangeQuery.buildFromParams(validGroupParams);
    assertTrue(query.isValid());
  }

  @Test
  public void should_reject_inconsistent_query() throws Exception {

    Map<String, Object> inconsistentParams = Maps.newHashMap();
    inconsistentParams.put("user", "my_login");
    inconsistentParams.put("group", "my_group");
    inconsistentParams.put("permission", Permissions.SYSTEM_ADMIN);

    PermissionChangeQuery query = PermissionChangeQuery.buildFromParams(inconsistentParams);
    assertFalse(query.isValid());
  }

  @Test
  public void should_detect_missing_parameters() throws Exception {
    Map<String, Object> validGroupParams = Maps.newHashMap();
    validGroupParams.put("permission", "admin");

    PermissionChangeQuery query = PermissionChangeQuery.buildFromParams(validGroupParams);
    assertFalse(query.isValid());
  }

  @Test
  public void should_validate_permission_reference() throws Exception {
    Map<String, Object> inconsistentParams = Maps.newHashMap();
    inconsistentParams.put("user", "my_login");
    inconsistentParams.put("permission", "invalid_role");

    PermissionChangeQuery query = PermissionChangeQuery.buildFromParams(inconsistentParams);
    assertFalse(query.isValid());
  }
}
