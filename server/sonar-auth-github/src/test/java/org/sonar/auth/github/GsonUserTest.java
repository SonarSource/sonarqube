/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
package org.sonar.auth.github;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GsonUserTest {

  @Test
  public void parse_json() {
    GsonUser user = GsonUser.parse(
      "{\n" +
        "  \"login\": \"octocat\",\n" +
        "  \"id\": 1,\n" +
        "  \"name\": \"monalisa octocat\",\n" +
        "  \"email\": \"octocat@github.com\"\n" +
        "}");
    assertThat(user.getId()).isEqualTo("1");
    assertThat(user.getLogin()).isEqualTo("octocat");
    assertThat(user.getName()).isEqualTo("monalisa octocat");
    assertThat(user.getEmail()).isEqualTo("octocat@github.com");
  }

  @Test
  public void name_can_be_null() {
    GsonUser underTest = GsonUser.parse("{login:octocat, email:octocat@github.com}");
    assertThat(underTest.getLogin()).isEqualTo("octocat");
    assertThat(underTest.getName()).isNull();
  }

  @Test
  public void email_can_be_null() {
    GsonUser underTest = GsonUser.parse("{login:octocat}");
    assertThat(underTest.getLogin()).isEqualTo("octocat");
    assertThat(underTest.getEmail()).isNull();
  }
}
