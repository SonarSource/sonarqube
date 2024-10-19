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
package org.sonar.auth.gitlab;

import java.util.List;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GsonGroupTest {

  @Test
  public void test_parse() {
    List<GsonGroup> groups = GsonGroup.parse("[{\n" +
      "\"id\": 123456789,\n" +
      "\"web_url\": \"https://gitlab.com/groups/my-awesome-group/my-project\",\n" +
      "\"name\": \"my-project\",\n" +
      "\"path\": \"my-project\",\n" +
      "\"description\": \"toto\",\n" +
      "\"visibility\": \"private\",\n" +
      "\"lfs_enabled\": true,\n" +
      "\"avatar_url\": null,\n" +
      "\"request_access_enabled\": false,\n" +
      "\"full_name\": \"my-awesome-group / my-project\",\n" +
      "\"full_path\": \"my-awesome-group/my-project\",\n" +
      "\"parent_id\": 987654321,\n" +
      "\"ldap_cn\": null,\n" +
      "\"ldap_access\": null\n" +
      "}]");

    assertThat(groups).isNotNull();
    assertThat(groups.size()).isOne();
    assertThat(groups.get(0).getId()).isEqualTo("123456789");
    assertThat(groups.get(0).getFullPath()).isEqualTo("my-awesome-group/my-project");
  }
}
