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

package org.sonar.server.qualityprofile;

import org.sonar.server.qualityprofile.ProfileRuleQuery;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
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
    assertThat(query.anyInheritance()).isTrue();
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
  public void fail_on_incorrect_inheritance() {
    Map<String, Object> params = ImmutableMap.of("profileId", (Object) Integer.toString(42), "inheritance", "bad value");
    try {
      ProfileRuleQuery.parse(params);
      fail();
    } catch(Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class);
    }
  }

  @Test
  public void parse_with_sort() {
    ProfileRuleQuery query = ProfileRuleQuery.parse(ImmutableMap.of("profileId", (Object) Integer.toString(42),
      "sort_by", ProfileRuleQuery.SORT_BY_RULE_NAME, "asc", "true"));
    assertThat(query.sort()).isEqualTo(ProfileRuleQuery.SORT_BY_RULE_NAME);
    assertThat(query.asc()).isTrue();

    query = ProfileRuleQuery.parse(ImmutableMap.of("profileId", (Object) Integer.toString(42),
      "sort_by", ProfileRuleQuery.SORT_BY_RULE_NAME, "asc", "false"));
    assertThat(query.sort()).isEqualTo(ProfileRuleQuery.SORT_BY_RULE_NAME);
    assertThat(query.asc()).isFalse();

    // Default sort
    query = ProfileRuleQuery.parse(ImmutableMap.of("profileId", (Object) Integer.toString(42)));
    assertThat(query.sort()).isEqualTo(ProfileRuleQuery.SORT_BY_RULE_NAME);
    assertThat(query.asc()).isTrue();
  }

  @Test
  public void fail_on_incorrect_sort() {
    Map<String, Object> params = ImmutableMap.of("profileId", (Object) Integer.toString(42), "sort_by", "bad sort");
    try {
      ProfileRuleQuery.parse(params);
      fail();
    } catch(Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class);
    }
  }

  @Test
  public void should_parse_all_parameters() {
    final int profileId = 42;
    final String language = "xoo";
    final String nameOrKey = "unused parameter";
    Map<String, Object> params = ImmutableMap.<String, Object> builder()
        .put("profileId", (Object) Integer.toString(profileId))
        .put("language", (Object) language)
        .put("nameOrKey", (Object) nameOrKey)
        .put("repositoryKeys", (Object) Lists.newArrayList("", "repo1", "repo2"))
        .put("severities", new Object())
        .put("statuses", (Object) Lists.newArrayList("", "BETA", "DEPRECATED"))
        .put("tags", (Object) Lists.newArrayList("", "tag1", "tag2"))
        .build();
    ProfileRuleQuery query = ProfileRuleQuery.parse(params);
    assertThat(query.profileId()).isEqualTo(profileId);
    assertThat(query.language()).isEqualTo(language);
    assertThat(query.nameOrKey()).isEqualTo(nameOrKey);
    assertThat(query.repositoryKeys()).containsOnly("repo1", "repo2");
    assertThat(query.severities()).isEmpty();
    assertThat(query.statuses()).containsOnly("BETA", "DEPRECATED");
    assertThat(query.tags()).containsOnly("tag1", "tag2");
  }
}
