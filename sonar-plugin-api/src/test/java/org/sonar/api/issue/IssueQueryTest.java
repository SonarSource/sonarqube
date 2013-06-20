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
package org.sonar.api.issue;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.web.UserRole;

import java.util.Arrays;
import java.util.Date;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class IssueQueryTest {

  @Test
  public void should_build_query() throws Exception {
    IssueQuery query = IssueQuery.builder()
      .issueKeys(Lists.newArrayList("ABCDE"))
      .severities(Lists.newArrayList(Severity.BLOCKER))
      .statuses(Lists.newArrayList(Issue.STATUS_RESOLVED))
      .resolutions(Lists.newArrayList(Issue.RESOLUTION_FALSE_POSITIVE))
      .components(Lists.newArrayList("org/struts/Action.java"))
      .componentRoots(Lists.newArrayList("org.struts:core"))
      .rules(Lists.newArrayList(RuleKey.of("squid", "AvoidCycle")))
      .actionPlans(Lists.newArrayList("AP1", "AP2"))
      .reporters(Lists.newArrayList("crunky"))
      .assignees(Lists.newArrayList("gargantua"))
      .assigned(true)
      .createdAfter(new Date())
      .createdBefore(new Date())
      .planned(true)
      .resolved(true)
      .sort(IssueQuery.SORT_BY_ASSIGNEE)
      .asc(true)
      .pageSize(10)
      .pageIndex(2)
      .requiredRole(UserRole.USER)
      .build();
    assertThat(query.issueKeys()).containsOnly("ABCDE");
    assertThat(query.severities()).containsOnly(Severity.BLOCKER);
    assertThat(query.statuses()).containsOnly(Issue.STATUS_RESOLVED);
    assertThat(query.resolutions()).containsOnly(Issue.RESOLUTION_FALSE_POSITIVE);
    assertThat(query.components()).containsOnly("org/struts/Action.java");
    assertThat(query.componentRoots()).containsOnly("org.struts:core");
    assertThat(query.reporters()).containsOnly("crunky");
    assertThat(query.assignees()).containsOnly("gargantua");
    assertThat(query.assigned()).isTrue();
    assertThat(query.rules()).containsOnly(RuleKey.of("squid", "AvoidCycle"));
    assertThat(query.actionPlans()).containsOnly("AP1", "AP2");
    assertThat(query.createdAfter()).isNotNull();
    assertThat(query.createdBefore()).isNotNull();
    assertThat(query.planned()).isTrue();
    assertThat(query.resolved()).isTrue();
    assertThat(query.sort()).isEqualTo(IssueQuery.SORT_BY_ASSIGNEE);
    assertThat(query.asc()).isTrue();
    assertThat(query.pageSize()).isEqualTo(10);
    assertThat(query.pageIndex()).isEqualTo(2);
    assertThat(query.requiredRole()).isEqualTo(UserRole.USER);
  }

  @Test
  public void should_build_query_without_dates() throws Exception {
    IssueQuery query = IssueQuery.builder()
      .issueKeys(Lists.newArrayList("ABCDE"))
      .build();

    assertThat(query.issueKeys()).containsOnly("ABCDE");
    assertThat(query.createdAfter()).isNull();
    assertThat(query.createdBefore()).isNull();
  }

  @Test
  public void should_throw_exception_if_sort_is_not_valid() throws Exception {
    try {
      IssueQuery.builder()
        .sort("UNKNOWN")
        .build();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("Bad sort field: UNKNOWN");
    }
  }

  @Test
  public void collection_params_should_not_be_null_but_empty() throws Exception {
    IssueQuery query = IssueQuery.builder()
      .issueKeys(null)
      .components(null)
      .componentRoots(null)
      .statuses(null)
      .actionPlans(null)
      .assignees(null)
      .reporters(null)
      .resolutions(null)
      .rules(null)
      .severities(null)
      .build();
    assertThat(query.issueKeys()).isEmpty();
    assertThat(query.components()).isEmpty();
    assertThat(query.componentRoots()).isEmpty();
    assertThat(query.statuses()).isEmpty();
    assertThat(query.actionPlans()).isEmpty();
    assertThat(query.assignees()).isEmpty();
    assertThat(query.reporters()).isEmpty();
    assertThat(query.resolutions()).isEmpty();
    assertThat(query.rules()).isEmpty();
    assertThat(query.severities()).isEmpty();
  }

  @Test
  public void test_default_query() throws Exception {
    IssueQuery query = IssueQuery.builder().build();
    assertThat(query.issueKeys()).isEmpty();
    assertThat(query.components()).isEmpty();
    assertThat(query.componentRoots()).isEmpty();
    assertThat(query.statuses()).isEmpty();
    assertThat(query.actionPlans()).isEmpty();
    assertThat(query.assignees()).isEmpty();
    assertThat(query.reporters()).isEmpty();
    assertThat(query.resolutions()).isEmpty();
    assertThat(query.rules()).isEmpty();
    assertThat(query.severities()).isEmpty();
    assertThat(query.assigned()).isNull();
    assertThat(query.createdAfter()).isNull();
    assertThat(query.createdBefore()).isNull();
    assertThat(query.planned()).isNull();
    assertThat(query.resolved()).isNull();
    assertThat(query.sort()).isNull();
    assertThat(query.pageSize()).isEqualTo(100);
    assertThat(query.pageIndex()).isEqualTo(1);
    assertThat(query.requiredRole()).isEqualTo(UserRole.USER);
    assertThat(query.maxResults()).isEqualTo(IssueQuery.MAX_RESULTS);

  }

  @Test
  public void should_use_max_page_size_if_negative() throws Exception {
    IssueQuery query = IssueQuery.builder().pageSize(0).build();
    assertThat(query.pageSize()).isEqualTo(IssueQuery.MAX_PAGE_SIZE);

    query = IssueQuery.builder().pageSize(-1).build();
    assertThat(query.pageSize()).isEqualTo(IssueQuery.MAX_PAGE_SIZE);
  }

  @Test
  public void test_default_page_index_and_size() throws Exception {
    IssueQuery query = IssueQuery.builder().build();
    assertThat(query.pageSize()).isEqualTo(IssueQuery.DEFAULT_PAGE_SIZE);
    assertThat(query.pageIndex()).isEqualTo(IssueQuery.DEFAULT_PAGE_INDEX);
  }

  @Test
  public void should_reset_to_max_page_size() throws Exception {
    IssueQuery query = IssueQuery.builder()
      .pageSize(IssueQuery.MAX_PAGE_SIZE + 100)
      .build();
    assertThat(query.pageSize()).isEqualTo(IssueQuery.MAX_PAGE_SIZE);
  }

  @Test
  public void could_disable_paging_on_single_component() throws Exception {
    IssueQuery query = IssueQuery.builder().components(Arrays.asList("Action.java")).build();
    assertThat(query.pageSize()).isGreaterThan(IssueQuery.MAX_PAGE_SIZE);
  }

  @Test
  public void page_index_should_be_positive() throws Exception {
    try {
      IssueQuery.builder()
        .pageIndex(0)
        .build();
      fail();
    } catch (Exception e) {
      assertThat(e).hasMessage("Page index must be greater than 0 (got 0)").isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Test
  public void should_accept_null_sort() throws Exception {
    IssueQuery query = IssueQuery.builder().sort(null).build();
    assertThat(query.sort()).isNull();
  }
}
