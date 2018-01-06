/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.api.user;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class UserQueryTest {
  @Test
  public void test_all_actives() throws Exception {
    assertThat(UserQuery.ALL_ACTIVES.includeDeactivated()).isFalse();
    assertThat(UserQuery.ALL_ACTIVES.logins()).isNull();
    assertThat(UserQuery.ALL_ACTIVES.searchText()).isNull();
    assertThat(UserQuery.ALL_ACTIVES.searchTextSql).isNull();
  }

  @Test
  public void test_all() throws Exception {
    UserQuery all = UserQuery.builder().includeDeactivated().build();
    assertThat(all.includeDeactivated()).isTrue();
    assertThat(all.logins()).isNull();
  }

  @Test
  public void test_logins() throws Exception {
    UserQuery query = UserQuery.builder().logins("simon", "loic").build();
    assertThat(query.includeDeactivated()).isFalse();
    assertThat(query.logins()).containsOnly("simon", "loic");

    query = UserQuery.builder().logins(Arrays.asList("simon", "loic")).build();
    assertThat(query.logins()).containsOnly("simon", "loic");
  }

  @Test
  public void should_limit_number_of_logins() {
    List<String> logins = new ArrayList<>();
    for (int i = 0; i < 1010; i++) {
      logins.add(String.valueOf(i));
    }
    try {
      UserQuery.builder().logins(logins).build();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Max number of logins is 1000. Got 1010");
    }
  }

  @Test
  public void searchText() {
    UserQuery query = UserQuery.builder().searchText("sim").build();
    assertThat(query.searchText()).isEqualTo("sim");
    assertThat(query.searchTextSql).isEqualTo("%sim%");
  }

  @Test
  public void searchText_escape_special_characters_in_like() {
    UserQuery query = UserQuery.builder().searchText("%sim_").build();
    assertThat(query.searchText()).isEqualTo("%sim_");
    assertThat(query.searchTextSql).isEqualTo("%/%sim/_%");
  }
}
