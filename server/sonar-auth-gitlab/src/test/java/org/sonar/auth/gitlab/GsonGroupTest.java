/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.auth.gitlab;

import java.util.List;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GsonGroupTest {

  @Test
  public void test_parse() {
    List<GsonGroup> groups = GsonGroup.parse("""
      [{
      "id": 123456789,
      "web_url": "https://gitlab.com/groups/my-awesome-group/my-project",
      "name": "my-project",
      "path": "my-project",
      "description": "toto",
      "visibility": "private",
      "lfs_enabled": true,
      "avatar_url": null,
      "request_access_enabled": false,
      "full_name": "my-awesome-group / my-project",
      "full_path": "my-awesome-group/my-project",
      "parent_id": 987654321,
      "ldap_cn": null,
      "ldap_access": null
      }]""");

    assertThat(groups).isNotNull();
    assertThat(groups.size()).isOne();
    assertThat(groups.get(0).getId()).isEqualTo("123456789");
    assertThat(groups.get(0).getFullPath()).isEqualTo("my-awesome-group/my-project");
  }
}
