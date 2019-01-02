/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.issue.index;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.Date;
import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.Severity;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.server.issue.index.IssueQuery.PeriodStart;

import static com.google.common.collect.Lists.newArrayList;
import static org.apache.commons.lang.math.RandomUtils.nextInt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class IssueQueryTest {

  @Test
  public void build_query() {
    RuleDefinitionDto rule = new RuleDefinitionDto().setId(nextInt(1000));
    PeriodStart filterDate = new IssueQuery.PeriodStart(new Date(10_000_000_000L), false);
    IssueQuery query = IssueQuery.builder()
      .issueKeys(newArrayList("ABCDE"))
      .severities(newArrayList(Severity.BLOCKER))
      .statuses(Lists.newArrayList(Issue.STATUS_RESOLVED))
      .resolutions(newArrayList(Issue.RESOLUTION_FALSE_POSITIVE))
      .projectUuids(newArrayList("PROJECT"))
      .componentUuids(newArrayList("org/struts/Action.java"))
      .moduleUuids(newArrayList("org.struts:core"))
      .rules(newArrayList(rule))
      .assigneeUuids(newArrayList("gargantua"))
      .languages(newArrayList("xoo"))
      .tags(newArrayList("tag1", "tag2"))
      .types(newArrayList("RELIABILITY", "SECURITY"))
      .owaspTop10(newArrayList("a1", "a2"))
      .sansTop25(newArrayList("insecure-interaction", "porous-defenses"))
      .cwe(newArrayList("12", "125"))
      .organizationUuid("orga")
      .branchUuid("my_branch")
      .createdAfterByProjectUuids(ImmutableMap.of("PROJECT", filterDate))
      .assigned(true)
      .createdAfter(new Date())
      .createdBefore(new Date())
      .createdAt(new Date())
      .resolved(true)
      .sort(IssueQuery.SORT_BY_CREATION_DATE)
      .asc(true)
      .build();
    assertThat(query.issueKeys()).containsOnly("ABCDE");
    assertThat(query.severities()).containsOnly(Severity.BLOCKER);
    assertThat(query.statuses()).containsOnly(Issue.STATUS_RESOLVED);
    assertThat(query.resolutions()).containsOnly(Issue.RESOLUTION_FALSE_POSITIVE);
    assertThat(query.projectUuids()).containsOnly("PROJECT");
    assertThat(query.componentUuids()).containsOnly("org/struts/Action.java");
    assertThat(query.moduleUuids()).containsOnly("org.struts:core");
    assertThat(query.assignees()).containsOnly("gargantua");
    assertThat(query.languages()).containsOnly("xoo");
    assertThat(query.tags()).containsOnly("tag1", "tag2");
    assertThat(query.types()).containsOnly("RELIABILITY", "SECURITY");
    assertThat(query.owaspTop10()).containsOnly("a1", "a2");
    assertThat(query.sansTop25()).containsOnly("insecure-interaction", "porous-defenses");
    assertThat(query.cwe()).containsOnly("12", "125");
    assertThat(query.organizationUuid()).isEqualTo("orga");
    assertThat(query.branchUuid()).isEqualTo("my_branch");
    assertThat(query.createdAfterByProjectUuids()).containsOnly(entry("PROJECT", filterDate));
    assertThat(query.assigned()).isTrue();
    assertThat(query.rules()).containsOnly(rule);
    assertThat(query.createdAfter()).isNotNull();
    assertThat(query.createdBefore()).isNotNull();
    assertThat(query.createdAt()).isNotNull();
    assertThat(query.resolved()).isTrue();
    assertThat(query.sort()).isEqualTo(IssueQuery.SORT_BY_CREATION_DATE);
    assertThat(query.asc()).isTrue();
  }

  @Test
  public void build_query_without_dates() {
    IssueQuery query = IssueQuery.builder()
      .issueKeys(newArrayList("ABCDE"))
      .createdAfter(null)
      .createdBefore(null)
      .createdAt(null)
      .build();

    assertThat(query.issueKeys()).containsOnly("ABCDE");
    assertThat(query.createdAfter()).isNull();
    assertThat(query.createdBefore()).isNull();
    assertThat(query.createdAt()).isNull();
  }

  @Test
  public void throw_exception_if_sort_is_not_valid() {
    try {
      IssueQuery.builder()
        .sort("UNKNOWN")
        .build();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("Bad sort field: UNKNOWN");
    }
  }

  @Test
  public void collection_params_should_not_be_null_but_empty() {
    IssueQuery query = IssueQuery.builder()
      .issueKeys(null)
      .projectUuids(null)
      .componentUuids(null)
      .moduleUuids(null)
      .statuses(null)
      .assigneeUuids(null)
      .resolutions(null)
      .rules(null)
      .severities(null)
      .languages(null)
      .tags(null)
      .types(null)
      .owaspTop10(null)
      .sansTop25(null)
      .cwe(null)
      .createdAfterByProjectUuids(null)
      .build();
    assertThat(query.issueKeys()).isEmpty();
    assertThat(query.projectUuids()).isEmpty();
    assertThat(query.componentUuids()).isEmpty();
    assertThat(query.moduleUuids()).isEmpty();
    assertThat(query.statuses()).isEmpty();
    assertThat(query.assignees()).isEmpty();
    assertThat(query.resolutions()).isEmpty();
    assertThat(query.rules()).isEmpty();
    assertThat(query.severities()).isEmpty();
    assertThat(query.languages()).isEmpty();
    assertThat(query.tags()).isEmpty();
    assertThat(query.types()).isEmpty();
    assertThat(query.owaspTop10()).isEmpty();
    assertThat(query.sansTop25()).isEmpty();
    assertThat(query.cwe()).isEmpty();
    assertThat(query.createdAfterByProjectUuids()).isEmpty();
  }

  @Test
  public void test_default_query() {
    IssueQuery query = IssueQuery.builder().build();
    assertThat(query.issueKeys()).isEmpty();
    assertThat(query.projectUuids()).isEmpty();
    assertThat(query.componentUuids()).isEmpty();
    assertThat(query.moduleUuids()).isEmpty();
    assertThat(query.statuses()).isEmpty();
    assertThat(query.assignees()).isEmpty();
    assertThat(query.resolutions()).isEmpty();
    assertThat(query.rules()).isEmpty();
    assertThat(query.severities()).isEmpty();
    assertThat(query.languages()).isEmpty();
    assertThat(query.tags()).isEmpty();
    assertThat(query.types()).isEmpty();
    assertThat(query.organizationUuid()).isNull();
    assertThat(query.branchUuid()).isNull();
    assertThat(query.assigned()).isNull();
    assertThat(query.createdAfter()).isNull();
    assertThat(query.createdBefore()).isNull();
    assertThat(query.resolved()).isNull();
    assertThat(query.sort()).isNull();
    assertThat(query.createdAfterByProjectUuids()).isEmpty();
  }

  @Test
  public void should_accept_null_sort() {
    IssueQuery query = IssueQuery.builder().sort(null).build();
    assertThat(query.sort()).isNull();
  }
}
