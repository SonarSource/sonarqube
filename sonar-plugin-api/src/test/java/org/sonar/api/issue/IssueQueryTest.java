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
      .sort(IssueQuery.SORT_BY_ASSIGNEE)
      .pageSize(10)
      .pageIndex(2)
      .requiredRole(UserRole.CODEVIEWER)
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
    assertThat(query.sort()).isEqualTo(IssueQuery.SORT_BY_ASSIGNEE);
    assertThat(query.pageSize()).isEqualTo(10);
    assertThat(query.pageIndex()).isEqualTo(2);
    assertThat(query.requiredRole()).isEqualTo(UserRole.CODEVIEWER);
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
