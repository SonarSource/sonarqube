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

package org.sonar.server.rule;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.sonar.server.exceptions.BadRequestException;

import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class ProfileRuleQueryTest {

  @Test
  public void create_basic_query() {
    final int profileId = 42;
    ProfileRuleQuery query = ProfileRuleQuery.create(profileId);
    assertThat(query.profileId()).isEqualTo(profileId);
  }

  @Test
  public void parse_nominal_request() {
    final int profileId = 42;
    Map<String, Object> params = ImmutableMap.of("profileId", (Object) Integer.toString(profileId));
    ProfileRuleQuery query = ProfileRuleQuery.parse(params);
    assertThat(query.profileId()).isEqualTo(profileId);
  }

  @Test
  public void parse_with_inheritance() {
    final int profileId = 42;
    Map<String, Object> params = ImmutableMap.of(
      "profileId", (Object) Integer.toString(profileId),
      "inheritance", "OVERRIDES"
    );
    ProfileRuleQuery query = ProfileRuleQuery.parse(params);
    assertThat(query.profileId()).isEqualTo(profileId);
    assertThat(query.inheritance()).isEqualTo("OVERRIDES");
  }

  @Test
  public void fail_on_missing_profileId() {
    Map<String, Object> params = ImmutableMap.of();
    try {
      ProfileRuleQuery.parse(params);
      fail("Expected BadRequestException");
    } catch(BadRequestException bre) {
      assertThat(bre.errors().get(0).text()).isEqualTo("Missing parameter profileId");
    }
  }

  @Test
  public void fail_on_incorrect_profileId() {
    Map<String, Object> params = ImmutableMap.of("profileId", (Object) "not an integer");
    try {
      ProfileRuleQuery.parse(params);
      fail("Expected BadRequestException");
    } catch(Exception e) {
      assertThat(e).isInstanceOf(NumberFormatException.class);
    }
  }

  @Test
  public void fail_on_incorrect_inheritance() {
    final int profileId = 42;
    Map<String, Object> params = ImmutableMap.of(
      "profileId", (Object) Integer.toString(profileId),
      "inheritance", "bad value"
    );
    try {
      ProfileRuleQuery.parse(params);
      fail();
    } catch(Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class);
    }
  }
}
