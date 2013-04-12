/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.core.issue;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueFinder;
import org.sonar.api.issue.IssueQuery;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceDto;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultIssueFinderTest {

  private DefaultIssueFinder finder;
  private IssueDao issueDao;
  private ResourceDao resourceDao;
  private RuleFinder ruleFinder;

  @Before
  public void before() {
    issueDao = mock(IssueDao.class);
    resourceDao = mock(ResourceDao.class);
    ruleFinder = mock(RuleFinder.class);
    finder = new DefaultIssueFinder(issueDao, resourceDao, ruleFinder);
  }

  @Test
  public void should_find_issues() {
    IssueQuery issueQuery = mock(IssueQuery.class);

    IssueDto issue1 = new IssueDto().setId(1L).setRuleId(50).setResourceId(123);
    IssueDto issue2 = new IssueDto().setId(2L).setRuleId(50).setResourceId(123);
    List<IssueDto> dtoList = newArrayList(issue1, issue2);
    when(issueDao.select(issueQuery)).thenReturn(dtoList);
    Rule rule = Rule.create("repo", "key");
    rule.setId(50);
    when(ruleFinder.findById(anyInt())).thenReturn(rule);
    when(resourceDao.getResource(anyInt())).thenReturn(new ResourceDto().setKey("componentKey").setId(123L));

    IssueFinder.Results results = finder.find(issueQuery);
    assertThat(results.issues()).hasSize(2);
    Issue issue = results.issues().iterator().next();
    assertThat(issue.componentKey()).isEqualTo("componentKey");
    assertThat(issue.ruleKey()).isEqualTo("key");
    assertThat(issue.ruleRepositoryKey()).isEqualTo("repo");
  }

  @Test
  public void should_find_by_key() {
    IssueDto issueDto = new IssueDto().setId(1L).setRuleId(1).setResourceId(1);
    when(issueDao.selectByKey("key")).thenReturn(issueDto);
    when(ruleFinder.findById(anyInt())).thenReturn(Rule.create("repo", "key"));
    when(resourceDao.getResource(anyInt())).thenReturn(new ResourceDto().setKey("componentKey"));

    Issue issue = finder.findByKey("key");
    assertThat(issue).isNotNull();
    assertThat(issue.componentKey()).isEqualTo("componentKey");
    assertThat(issue.ruleKey()).isEqualTo("key");
    assertThat(issue.ruleRepositoryKey()).isEqualTo("repo");
  }
}
