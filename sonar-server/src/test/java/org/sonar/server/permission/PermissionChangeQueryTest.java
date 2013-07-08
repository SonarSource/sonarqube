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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.core.user.Permission;
import org.sonar.server.exceptions.BadRequestException;

import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;

public class PermissionChangeQueryTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void should_populate_from_params() throws Exception {

    Map<String, Object> params = Maps.newHashMap();
    params.put("user", "my_login");
    params.put("group", "my_group");
    params.put("permission", Permission.SYSTEM_ADMIN.key());

    PermissionChangeQuery query = PermissionChangeQuery.buildFromParams(params);

    assertThat(query.getUser()).isEqualTo("my_login");
    assertThat(query.getGroup()).isEqualTo("my_group");
    assertThat(query.getRole()).isEqualTo(Permission.SYSTEM_ADMIN.key());
  }

  @Test
  public void should_validate_user_query() throws Exception {

    Map<String, Object> validUserParams = Maps.newHashMap();
    validUserParams.put("user", "my_login");
    validUserParams.put("permission", Permission.SYSTEM_ADMIN.key());

    PermissionChangeQuery query = PermissionChangeQuery.buildFromParams(validUserParams);
    query.validate();
  }

  @Test
  public void should_validate_group_query() throws Exception {

    Map<String, Object> validGroupParams = Maps.newHashMap();
    validGroupParams.put("group", "my_group");
    validGroupParams.put("permission", Permission.SYSTEM_ADMIN.key());

    PermissionChangeQuery query = PermissionChangeQuery.buildFromParams(validGroupParams);
    query.validate();
  }

  @Test
  public void should_reject_inconsistent_query() throws Exception {

    Map<String, Object> inconsistentParams = Maps.newHashMap();
    inconsistentParams.put("user", "my_login");
    inconsistentParams.put("group", "my_group");
    inconsistentParams.put("permission", Permission.SYSTEM_ADMIN.key());

    PermissionChangeQuery query = PermissionChangeQuery.buildFromParams(inconsistentParams);

    thrown.expect(BadRequestException.class);
    thrown.expectMessage("Only one of user or group parameter should be provided");
    query.validate();
  }

  @Test
  public void should_detect_missing_user_or_group() throws Exception {
    Map<String, Object> inconsistentParams = Maps.newHashMap();
    inconsistentParams.put("permission", "admin");

    PermissionChangeQuery query = PermissionChangeQuery.buildFromParams(inconsistentParams);

    thrown.expect(BadRequestException.class);
    thrown.expectMessage("Missing user or group parameter");
    query.validate();
  }

  @Test
  public void should_detect_missing_permission() throws Exception {
    Map<String, Object> inconsistentParams = Maps.newHashMap();
    inconsistentParams.put("user", "my_login");

    PermissionChangeQuery query = PermissionChangeQuery.buildFromParams(inconsistentParams);

    thrown.expect(BadRequestException.class);
    thrown.expectMessage("Missing permission parameter");
    query.validate();
  }

  @Test
  public void should_validate_permission_reference() throws Exception {
    Map<String, Object> inconsistentParams = Maps.newHashMap();
    inconsistentParams.put("user", "my_login");
    inconsistentParams.put("permission", "invalid_role");

    PermissionChangeQuery query = PermissionChangeQuery.buildFromParams(inconsistentParams);

    thrown.expect(BadRequestException.class);
    thrown.expectMessage("Invalid permission key invalid_role");
    query.validate();
  }
}
