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

package org.sonar.server.issue;

import org.junit.Test;
import org.sonar.server.exceptions.BadRequestException;

import java.util.Collections;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class IssueBulkChangeQueryTest {

  @Test
  public void should_create_query() {
    Map<String, Object> params = newHashMap();
    params.put("issues", newArrayList("ABCD", "EFGH"));
    params.put("actions", newArrayList("do_transition", "assign", "set_severity", "plan"));
    params.put("do_transition.transition", "confirm");
    params.put("assign.assignee", "arthur");
    params.put("set_severity.severity", "MINOR");
    params.put("plan.plan", "3.7");

    IssueBulkChangeQuery issueBulkChangeQuery = new IssueBulkChangeQuery(params, true);
    assertThat(issueBulkChangeQuery.actions()).containsOnly("do_transition", "assign", "set_severity", "plan");
    assertThat(issueBulkChangeQuery.issues()).containsOnly("ABCD", "EFGH");
  }

  @Test
  public void should_remove_empty_actions() {
    Map<String, Object> params = newHashMap();
    params.put("issues", newArrayList("ABCD", "EFGH"));
    params.put("actions", newArrayList("do_transition", "", null));
    params.put("do_transition.transition", "confirm");

    IssueBulkChangeQuery issueBulkChangeQuery = new IssueBulkChangeQuery(params, true);
    assertThat(issueBulkChangeQuery.actions()).containsOnly("do_transition");
    assertThat(issueBulkChangeQuery.issues()).containsOnly("ABCD", "EFGH");
  }

  @Test
  public void should_remove_empty_issues() {
    Map<String, Object> params = newHashMap();
    params.put("issues", newArrayList("ABCD", "EFGH", "", null));
    params.put("actions", newArrayList("do_transition"));
    params.put("do_transition.transition", "confirm");

    IssueBulkChangeQuery issueBulkChangeQuery = new IssueBulkChangeQuery(params, true);
    assertThat(issueBulkChangeQuery.actions()).containsOnly("do_transition");
    assertThat(issueBulkChangeQuery.issues()).containsOnly("ABCD", "EFGH");
  }

  @Test
  public void should_create_query_with_comment() {
    Map<String, Object> params = newHashMap();
    params.put("issues", newArrayList("ABCD", "EFGH"));
    params.put("actions", newArrayList("assign"));
    params.put("assign.assignee", "arthur");

    IssueBulkChangeQuery issueBulkChangeQuery = new IssueBulkChangeQuery(params, "My comment for bulk change", true);
    assertThat(issueBulkChangeQuery.hasComment()).isTrue();
    assertThat(issueBulkChangeQuery.actions()).containsOnly("assign");
    assertThat(issueBulkChangeQuery.properties("comment").get("comment")).isEqualTo("My comment for bulk change");
  }

  @Test
  public void should_get_properties_action() {
    Map<String, Object> params = newHashMap();
    params.put("issues", newArrayList("ABCD", "EFGH"));
    params.put("actions", newArrayList("assign"));
    params.put("assign.assignee", "arthur");

    IssueBulkChangeQuery issueBulkChangeQuery = new IssueBulkChangeQuery(params, true);
    assertThat(issueBulkChangeQuery.properties("assign")).hasSize(1);
    assertThat(issueBulkChangeQuery.properties("assign").get("assignee")).isEqualTo("arthur");
  }

  @Test
  public void fail_to_build_if_no_issue() {
    Map<String, Object> params = newHashMap();
    params.put("actions", newArrayList("assign"));
    params.put("assign.assignee", "arthur");
    try {
      new IssueBulkChangeQuery(params, true);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class);
      checkBadRequestException(e, "issue_bulk_change.error.empty_issues");
    }
  }

  @Test
  public void fail_to_build_if_issues_are_empty() {
    Map<String, Object> params = newHashMap();
    params.put("issues", Collections.emptyList());
    params.put("actions", newArrayList("assign"));
    params.put("assign.assignee", "arthur");
    try {
      new IssueBulkChangeQuery(params, true);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class);
      checkBadRequestException(e, "issue_bulk_change.error.empty_issues");
    }
  }

  @Test
  public void fail_to_build_if_no_action() {
    Map<String, Object> params = newHashMap();
    params.put("issues", newArrayList("ABCD", "EFGH"));
    try {
      new IssueBulkChangeQuery(params, true);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class);
      checkBadRequestException(e, "issue_bulk_change.error.need_one_action");
    }
  }

  @Test
  public void fail_to_build_if_actions_are_empty() {
    Map<String, Object> params = newHashMap();
    params.put("issues", newArrayList("ABCD", "EFGH"));
    params.put("actions", Collections.emptyList());
    try {
      new IssueBulkChangeQuery(params, true);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class);
      checkBadRequestException(e, "issue_bulk_change.error.need_one_action");
    }
  }

  private void checkBadRequestException(Exception e, String key, Object... params) {
    BadRequestException exception = (BadRequestException) e;
    assertThat(exception.firstError().getKey()).isEqualTo(key);
    assertThat(exception.firstError().getParams()).containsOnly(params);
  }

}
