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
package org.sonar.wsclient.user;

import org.junit.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class UserQueryTest {
  @Test
  public void test_params() throws Exception {
    UserQuery query = UserQuery.create().includeDeactivated().logins("simon", "loic");
    Map<String, Object> params = query.urlParams();

    assertThat(params.get("includeDeactivated")).isEqualTo("true");
    assertThat(params.get("logins")).isEqualTo("simon,loic");
  }

  @Test
  public void test_empty_params() throws Exception {
    UserQuery query = UserQuery.create();
    Map<String, Object> params = query.urlParams();

    assertThat(params).isEmpty();
  }

  @Test
  public void should_replace_logins() throws Exception {
    UserQuery query = UserQuery.create().logins("simon").logins("loic");
    assertThat(query.urlParams().get("logins")).isEqualTo("loic");
  }

  @Test
  public void should_search_by_text() throws Exception {
    UserQuery query = UserQuery.create().searchText("sim");
    assertThat(query.urlParams().get("s")).isEqualTo("sim");

    query = UserQuery.create().searchText("sim").searchText(null);
    assertThat(query.urlParams().get("s")).isNull();
  }
}
