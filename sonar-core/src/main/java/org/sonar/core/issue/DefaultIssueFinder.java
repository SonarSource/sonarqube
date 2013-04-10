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

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueFinder;
import org.sonar.api.issue.IssueQuery;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.core.resource.ResourceDao;

import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * @since 3.6
 */
public class DefaultIssueFinder implements IssueFinder {

  private final IssueDao issueDao;
  private final ResourceDao resourceDao;
  private final RuleFinder ruleFinder;

  public DefaultIssueFinder(IssueDao issueDao, ResourceDao resourceDao, RuleFinder ruleFinder) {
    this.issueDao = issueDao;
    this.resourceDao = resourceDao;
    this.ruleFinder = ruleFinder;
  }

  public List<Issue> find(IssueQuery issueQuery) {
    Collection<IssueDto> dtoList = issueDao.select(issueQuery);
    return newArrayList(Iterables.transform(dtoList, new Function<IssueDto, Issue>() {
      @Override
      public Issue apply(IssueDto input) {
        return toIssue(input);
      }
    }));
  }

  private Issue toIssue(IssueDto issueDto) {
    // FIXME N+1 SQL requests
    Rule rule = ruleFinder.findById(issueDto.getRuleId());
    DefaultIssue issue = new DefaultIssue();
    issue.setStatus(issueDto.getStatus());
    issue.setResolution(issueDto.getResolution());
    issue.setMessage(issueDto.getMessage());
    issue.setCost(issueDto.getCost());
    issue.setLine(issueDto.getLine());
    issue.setUserLogin(issueDto.getUserLogin());
    issue.setAssigneeLogin(issueDto.getAssigneeLogin());
    // FIXME N+1 SQL requests
    issue.setComponentKey(resourceDao.getResource(issueDto.getResourceId()).getKey());
    issue.setRuleKey(rule.getKey());
    issue.setRuleRepositoryKey(rule.getRepositoryKey());
    return issue;
  }

}
