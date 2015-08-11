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

package org.sonar.server.permission;

import com.google.common.collect.Maps;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.server.exceptions.BadRequestException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class PermissionChangeTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void populate_from_params() {
    Map<String, Object> params = Maps.newHashMap();
    params.put("user", "my_login");
    params.put("group", "my_group");
    params.put("component", "org.sample.Sample");
    params.put("permission", GlobalPermissions.SYSTEM_ADMIN);

    PermissionChange query = PermissionChange.buildFromParams(params);

    assertThat(query.userLogin()).isEqualTo("my_login");
    assertThat(query.groupName()).isEqualTo("my_group");
    assertThat(query.componentKey()).isEqualTo("org.sample.Sample");
    assertThat(query.permission()).isEqualTo(GlobalPermissions.SYSTEM_ADMIN);
  }

  @Test
  public void validate_user_query() {
    Map<String, Object> validUserParams = Maps.newHashMap();
    validUserParams.put("user", "my_login");
    validUserParams.put("permission", GlobalPermissions.SYSTEM_ADMIN);

    PermissionChange query = PermissionChange.buildFromParams(validUserParams);
    query.validate();
  }

  @Test
  public void validate_group_query() {
    Map<String, Object> validGroupParams = Maps.newHashMap();
    validGroupParams.put("group", "my_group");
    validGroupParams.put("permission", GlobalPermissions.SYSTEM_ADMIN);

    PermissionChange query = PermissionChange.buildFromParams(validGroupParams);
    query.validate();
  }

  @Test
  public void reject_inconsistent_query() {
    Map<String, Object> inconsistentParams = Maps.newHashMap();
    inconsistentParams.put("user", "my_login");
    inconsistentParams.put("group", "my_group");
    inconsistentParams.put("permission", GlobalPermissions.SYSTEM_ADMIN);

    PermissionChange query = PermissionChange.buildFromParams(inconsistentParams);

    thrown.expect(BadRequestException.class);
    thrown.expectMessage("Only one of user or group parameter should be provided");
    query.validate();
  }

  @Test
  public void detect_missing_user_or_group() {
    Map<String, Object> inconsistentParams = Maps.newHashMap();
    inconsistentParams.put("permission", "admin");

    PermissionChange query = PermissionChange.buildFromParams(inconsistentParams);

    thrown.expect(BadRequestException.class);
    thrown.expectMessage("Missing user or group parameter");
    query.validate();
  }

  @Test
  public void detect_missing_permission() {
    Map<String, Object> inconsistentParams = Maps.newHashMap();
    inconsistentParams.put("user", "my_login");

    PermissionChange query = PermissionChange.buildFromParams(inconsistentParams);

    thrown.expect(BadRequestException.class);
    thrown.expectMessage("Missing permission parameter");
    query.validate();
  }

  @Test
  public void validate_global_permission_reference() {
    Map<String, Object> inconsistentParams = Maps.newHashMap();
    inconsistentParams.put("user", "my_login");
    inconsistentParams.put("permission", "invalid");

    PermissionChange query = PermissionChange.buildFromParams(inconsistentParams);

    thrown.expect(BadRequestException.class);
    thrown.expectMessage("Invalid global permission key invalid. Valid values are [admin, profileadmin, shareDashboard, scan, dryRunScan, provisioning]");
    query.validate();
  }

  @Test
  public void validate_component_permission_reference() {
    Map<String, Object> inconsistentParams = Maps.newHashMap();
    inconsistentParams.put("user", "my_login");
    inconsistentParams.put("component", "org.sample.Sample");
    inconsistentParams.put("permission", "invalid");

    PermissionChange query = PermissionChange.buildFromParams(inconsistentParams);

    thrown.expect(BadRequestException.class);
    thrown.expectMessage("Invalid component permission key invalid. Valid values are [user, admin, issueadmin, codeviewer]");
    query.validate();
  }
}
