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

import java.util.Date;

import static org.fest.assertions.Assertions.assertThat;

public class IssueQueryTest {

  @Test
  public void should_build_query() throws Exception {
    IssueQuery query = IssueQuery.builder()
      .keys(Lists.newArrayList("ABCDE"))
      .severities(Lists.newArrayList(Severity.BLOCKER))
      .statuses(Lists.newArrayList(Issue.STATUS_RESOLVED))
      .resolutions(Lists.newArrayList(Issue.RESOLUTION_FALSE_POSITIVE))
      .components(Lists.newArrayList("org/struts/Action.java"))
      .componentRoots(Lists.newArrayList("org.struts:core"))
      .rules(Lists.newArrayList(RuleKey.of("squid", "AvoidCycle")))
      .userLogins(Lists.newArrayList("crunky"))
      .assignees(Lists.newArrayList("gargantua"))
      .createdAfter(new Date())
      .createdBefore(new Date())
      .limit(10)
      .page(2)
      .build();
    assertThat(query.keys()).containsOnly("ABCDE");
    assertThat(query.severities()).containsOnly(Severity.BLOCKER);
    assertThat(query.statuses()).containsOnly(Issue.STATUS_RESOLVED);
    assertThat(query.resolutions()).containsOnly(Issue.RESOLUTION_FALSE_POSITIVE);
    assertThat(query.components()).containsOnly("org/struts/Action.java");
    assertThat(query.componentRoots()).containsOnly("org.struts:core");
    assertThat(query.userLogins()).containsOnly("crunky");
    assertThat(query.assignees()).containsOnly("gargantua");
    assertThat(query.rules()).containsOnly(RuleKey.of("squid", "AvoidCycle"));
    assertThat(query.createdAfter()).isNotNull();
    assertThat(query.createdBefore()).isNotNull();
    assertThat(query.limit()).isEqualTo(10);
    assertThat(query.page()).isEqualTo(2);
  }
}
