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
package org.sonar.wsclient.issue;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class IssueParserTest {
  @Test
  public void test_GET_search() throws Exception {
    String json = IOUtils.toString(getClass().getResourceAsStream("/org/sonar/wsclient/issue/IssueParserTest/search.json"));
    Issues issues = new IssueParser().parseIssues(json);
    assertThat(issues).isNotNull();
    List<Issue> list = issues.list();
    assertThat(list).hasSize(2);
    Issue first = list.get(0);
    assertThat(first.key()).isEqualTo("ABCDE");
    assertThat(first.componentKey()).isEqualTo("Action.java");
    assertThat(first.ruleKey()).isEqualTo("squid:AvoidCycle");
    assertThat(first.severity()).isEqualTo("CRITICAL");
    assertThat(first.line()).isEqualTo(10);
    assertThat(first.resolution()).isEqualTo("FIXED");
    assertThat(first.status()).isEqualTo("OPEN");
    assertThat(first.assignee()).isEqualTo("karadoc");
    assertThat(first.description()).isEqualTo("the desc");
    assertThat(first.cost()).isEqualTo(4.2);
    assertThat(first.userLogin()).isEqualTo("perceval");
    assertThat(first.createdAt()).isNotNull();
    assertThat(first.updatedAt()).isNotNull();
    assertThat(first.closedAt()).isNotNull();
    assertThat(first.attribute("JIRA")).isEqualTo("FOO-1234");
    assertThat(first.attribute("OTHER")).isNull();
    assertThat(first.attributes()).hasSize(1);

    Issue second = list.get(1);
    assertThat(second.key()).isEqualTo("FGHIJ");
    assertThat(second.line()).isNull();
    assertThat(second.cost()).isNull();
    assertThat(second.description()).isNull();
    assertThat(second.attribute("JIRA")).isNull();
    assertThat(second.attributes()).isEmpty();

    assertThat(issues.paging()).isNotNull();
    Paging paging = issues.paging();
    assertThat(paging.pageIndex()).isEqualTo(1);
    assertThat(paging.pageSize()).isEqualTo(100);
    assertThat(paging.pages()).isEqualTo(1);
    assertThat(paging.total()).isEqualTo(2);

    assertThat(issues.securityExclusions()).isTrue();
  }

  @Test
  public void test_GET_transitions() throws Exception {
    String json = IOUtils.toString(getClass().getResourceAsStream("/org/sonar/wsclient/issue/IssueParserTest/getTransitions.json"));
    List<String> transitions = new IssueParser().parseTransitions(json);

    assertThat(transitions).isNotNull();
    assertThat(transitions).hasSize(2);
    assertThat(transitions).containsOnly("resolve", "falsepositive");
  }

}
