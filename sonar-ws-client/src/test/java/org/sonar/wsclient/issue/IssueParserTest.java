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
import org.sonar.wsclient.component.Component;
import org.sonar.wsclient.user.User;

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
    assertThat(first.ruleKey()).isEqualTo("squid:CycleBetweenPackages");
    assertThat(first.severity()).isEqualTo("CRITICAL");
    assertThat(first.line()).isEqualTo(10);
    assertThat(first.resolution()).isEqualTo("FIXED");
    assertThat(first.status()).isEqualTo("OPEN");
    assertThat(first.assignee()).isEqualTo("karadoc");
    assertThat(first.description()).isEqualTo("the desc");
    assertThat(first.effortToFix()).isEqualTo(4.2);
    assertThat(first.userLogin()).isEqualTo("perceval");
    assertThat(first.creationDate()).isNotNull();
    assertThat(first.updateDate()).isNotNull();
    assertThat(first.closeDate()).isNotNull();
    assertThat(first.attribute("JIRA")).isEqualTo("FOO-1234");
    assertThat(first.attribute("OTHER")).isNull();
    assertThat(first.attributes()).hasSize(1);
    assertThat(first.comments()).isEmpty();

    Issue second = list.get(1);
    assertThat(second.key()).isEqualTo("FGHIJ");
    assertThat(second.line()).isNull();
    assertThat(second.effortToFix()).isNull();
    assertThat(second.description()).isNull();
    assertThat(second.attribute("JIRA")).isNull();
    assertThat(second.attributes()).isEmpty();
    assertThat(second.comments()).isEmpty();

    assertThat(issues.rules()).hasSize(2);
    assertThat(issues.rule(first).key()).isEqualTo("squid:CycleBetweenPackages");
    assertThat(issues.rule(first).name()).isEqualTo("Avoid cycle between java packages");
    assertThat(issues.rule(first).description()).contains("When several packages");

    assertThat(issues.paging()).isNotNull();
    Paging paging = issues.paging();
    assertThat(paging.pageIndex()).isEqualTo(1);
    assertThat(paging.pageSize()).isEqualTo(100);
    assertThat(paging.pages()).isEqualTo(1);
    assertThat(paging.total()).isEqualTo(2);

    assertThat(issues.securityExclusions()).isTrue();
  }

  @Test
  public void test_GET_empty_search() throws Exception {
    String json = IOUtils.toString(getClass().getResourceAsStream("/org/sonar/wsclient/issue/IssueParserTest/empty.json"));
    Issues issues = new IssueParser().parseIssues(json);
    assertThat(issues).isNotNull();
    assertThat(issues.list()).isEmpty();
    assertThat(issues.rules()).isEmpty();
  }


  @Test
  public void test_GET_transitions() throws Exception {
    String json = IOUtils.toString(getClass().getResourceAsStream("/org/sonar/wsclient/issue/IssueParserTest/getTransitions.json"));
    List<String> transitions = new IssueParser().parseTransitions(json);

    assertThat(transitions).isNotNull();
    assertThat(transitions).hasSize(2);
    assertThat(transitions).containsOnly("resolve", "falsepositive");
  }

  @Test
  public void should_parse_comments() throws Exception {
    String json = IOUtils.toString(getClass().getResourceAsStream("/org/sonar/wsclient/issue/IssueParserTest/issue-with-comments.json"));
    Issues issues = new IssueParser().parseIssues(json);
    assertThat(issues.size()).isEqualTo(1);

    Issue issue = issues.list().get(0);
    assertThat(issue.comments()).hasSize(2);

    IssueComment firstComment = issue.comments().get(0);
    assertThat(firstComment.key()).isEqualTo("COMMENT-1");
    assertThat(firstComment.login()).isEqualTo("morgan");
    assertThat(firstComment.htmlText()).isEqualTo("the first comment");
    assertThat(firstComment.createdAt().getDate()).isEqualTo(18);

    IssueComment secondComment = issue.comments().get(1);
    assertThat(secondComment.key()).isEqualTo("COMMENT-2");
    assertThat(secondComment.login()).isEqualTo("arthur");
    assertThat(secondComment.htmlText()).isEqualTo("the second comment");
    assertThat(secondComment.createdAt().getDate()).isEqualTo(19);
  }

  @Test
  public void should_parse_users() throws Exception {
    String json = IOUtils.toString(getClass().getResourceAsStream("/org/sonar/wsclient/issue/IssueParserTest/issue-with-users.json"));
    Issues issues = new IssueParser().parseIssues(json);

    assertThat(issues.users()).hasSize(2);

    User morgan = issues.user("morgan");
    assertThat(morgan.login()).isEqualTo("morgan");
    assertThat(morgan.name()).isEqualTo("Morgan");
    assertThat(morgan.active()).isTrue();
    assertThat(morgan.email()).isEqualTo("mor@gan.bzh");

    User arthur = issues.user("arthur");
    assertThat(arthur.login()).isEqualTo("arthur");
    assertThat(arthur.name()).isEqualTo("Arthur");
    assertThat(arthur.active()).isFalse();
    assertThat(arthur.email()).isEqualTo("ar@thur.bzh");
  }

  @Test
  public void should_parse_components() throws Exception {
    String json = IOUtils.toString(getClass().getResourceAsStream("/org/sonar/wsclient/issue/IssueParserTest/issue-with-components.json"));
    Issues issues = new IssueParser().parseIssues(json);

    assertThat(issues.components()).hasSize(1);

    Component component = issues.component(issues.list().get(0));
    assertThat(component.key()).isEqualTo("struts:Action.java");
    assertThat(component.qualifier()).isEqualTo("CLA");
    assertThat(component.name()).isEqualTo("Action");
    assertThat(component.longName()).isEqualTo("org.struts.Action");
  }
}
