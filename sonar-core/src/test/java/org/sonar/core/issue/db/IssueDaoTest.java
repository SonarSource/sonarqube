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

package org.sonar.core.issue.db;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import org.apache.ibatis.executor.result.DefaultResultHandler;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.issue.IssueQuery;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.persistence.AbstractDaoTestCase;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;


public class IssueDaoTest extends AbstractDaoTestCase {

  IssueDao dao;

  @Before
  public void createDao() {
    dao = new IssueDao(getMyBatis());
  }

  @Test
  public void should_select_by_key() {
    setupData("shared", "should_select_by_key");

    IssueDto issue = dao.selectByKey("ABCDE");
    assertThat(issue.getKee()).isEqualTo("ABCDE");
    assertThat(issue.getId()).isEqualTo(100L);
    assertThat(issue.getComponentId()).isEqualTo(401);
    assertThat(issue.getRootComponentId()).isEqualTo(399);
    assertThat(issue.getRuleId()).isEqualTo(500);
    assertThat(issue.getSeverity()).isEqualTo("BLOCKER");
    assertThat(issue.isManualSeverity()).isFalse();
    assertThat(issue.getMessage()).isNull();
    assertThat(issue.getLine()).isEqualTo(200);
    assertThat(issue.getEffortToFix()).isEqualTo(4.2);
    assertThat(issue.getStatus()).isEqualTo("OPEN");
    assertThat(issue.getResolution()).isEqualTo("FIXED");
    assertThat(issue.getChecksum()).isEqualTo("XXX");
    assertThat(issue.getAuthorLogin()).isEqualTo("karadoc");
    assertThat(issue.getReporter()).isEqualTo("arthur");
    assertThat(issue.getAssignee()).isEqualTo("perceval");
    assertThat(issue.getIssueAttributes()).isEqualTo("JIRA=FOO-1234");
    assertThat(issue.getIssueCreationDate()).isNotNull();
    assertThat(issue.getIssueUpdateDate()).isNotNull();
    assertThat(issue.getIssueCloseDate()).isNotNull();
    assertThat(issue.getCreatedAt()).isNotNull();
    assertThat(issue.getUpdatedAt()).isNotNull();
    assertThat(issue.getRuleRepo()).isEqualTo("squid");
    assertThat(issue.getRule()).isEqualTo("AvoidCycle");
    assertThat(issue.getComponentKey()).isEqualTo("Action.java");
    assertThat(issue.getRootComponentKey()).isEqualTo("struts");
  }

  @Test
  public void should_select_all() {
    setupData("shared", "should_select_all");

    IssueQuery query = IssueQuery.builder().requiredRole("user").build();

    List<IssueDto> results = dao.selectIssueIds(query);
    assertThat(results).hasSize(3);
    IssueDto issue = results.get(0);
    assertThat(issue.getId()).isNotNull();
  }

  @Test
  public void should_select_all_without_authorisation() {
    setupData("should_select_all");

    IssueQuery query = IssueQuery.builder().requiredRole(null).build();

    List<IssueDto> results = dao.selectIssueIds(query);
    assertThat(results).hasSize(3);
    IssueDto issue = results.get(0);
    assertThat(issue.getId()).isNotNull();
  }

  @Test
  public void should_select_by_rules() {
    setupData("shared", "should_select_by_rules");

    IssueQuery query = IssueQuery.builder().rules(newArrayList(RuleKey.of("squid", "AvoidCycle"))).requiredRole("user").build();
    assertThat(dao.selectIssueIds(query)).hasSize(2);

    query = IssueQuery.builder().rules(newArrayList(RuleKey.of("squid", "AvoidCycle"), RuleKey.of("squid", "NullRef"))).requiredRole("user").build();
    assertThat(dao.selectIssueIds(query)).hasSize(3);

    query = IssueQuery.builder().rules(newArrayList(RuleKey.of("squid", "Other"))).requiredRole("user").build();
    assertThat(dao.selectIssueIds(query)).isEmpty();
  }

  @Test
  public void should_select_by_date_creation() {
    setupData("shared", "should_select_by_date_creation");

    // created after, strictly
    IssueQuery query = IssueQuery.builder().createdAfter(DateUtils.parseDate("2013-04-15")).build();
    assertThat(dao.selectIssueIds(query)).hasSize(1);
    assertThat(dao.selectIssueIds(query).get(0).getId()).isEqualTo(100L);

    query = IssueQuery.builder().createdAfter(DateUtils.parseDate("2022-01-01")).build();
    assertThat(dao.selectIssueIds(query)).isEmpty();


    // created at a given date
    query = IssueQuery.builder().createdAt(DateUtils.parseDate("2013-04-16")).build();
    assertThat(dao.selectIssueIds(query)).hasSize(1);
    assertThat(dao.selectIssueIds(query).get(0).getId()).isEqualTo(100L);

    query = IssueQuery.builder().createdAt(DateUtils.parseDate("2010-01-01")).build();
    assertThat(dao.selectIssueIds(query)).isEmpty();


    // created before
    query = IssueQuery.builder().createdBefore(DateUtils.parseDate("2013-04-14")).build();
    assertThat(dao.selectIssueIds(query)).hasSize(1);
    assertThat(dao.selectIssueIds(query).get(0).getId()).isEqualTo(101L);

    query = IssueQuery.builder().createdBefore(DateUtils.parseDate("2010-01-01")).build();
    assertThat(dao.selectIssueIds(query)).isEmpty();
  }

  @Test
  public void should_select_by_component() {
    setupData("shared", "should_select_by_component");

    IssueQuery query = IssueQuery.builder().components(newArrayList("Action.java")).requiredRole("user").build();
    List<IssueDto> issues = newArrayList(dao.selectIssueIds(query));
    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).getId()).isEqualTo(100);

    query = IssueQuery.builder().components(newArrayList("Filter.java")).requiredRole("user").build();
    issues = newArrayList(dao.selectIssueIds(query));
    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).getId()).isEqualTo(101);

    query = IssueQuery.builder().components(newArrayList("struts-core")).requiredRole("user").build();
    issues = newArrayList(dao.selectIssueIds(query));
    assertThat(issues).isEmpty();

    query = IssueQuery.builder().components(newArrayList("struts")).requiredRole("user").build();
    issues = newArrayList(dao.selectIssueIds(query));
    assertThat(issues).isEmpty();
  }

  @Test
  public void should_select_by_component_root() {
    setupData("shared", "should_select_by_component_root");

    IssueQuery query = IssueQuery.builder().componentRoots(newArrayList("struts")).requiredRole("user").build();
    List<IssueDto> issues = newArrayList(dao.selectIssueIds(query));
    assertThat(issues).hasSize(2);
    assertThat(getIssueIds(issues)).containsOnly(100l, 101l);

    query = IssueQuery.builder().componentRoots(newArrayList("struts-core")).requiredRole("user").build();
    issues = newArrayList(dao.selectIssueIds(query));
    assertThat(issues).hasSize(2);
    assertThat(getIssueIds(issues)).containsOnly(100l, 101l);

    query = IssueQuery.builder().componentRoots(newArrayList("Filter.java")).requiredRole("user").build();
    issues = newArrayList(dao.selectIssueIds(query));
    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).getId()).isEqualTo(101);

    query = IssueQuery.builder().componentRoots(newArrayList("not-found")).requiredRole("user").build();
    issues = newArrayList(dao.selectIssueIds(query));
    assertThat(issues).isEmpty();
  }

  @Test
  public void should_select_by_assigned() {
    setupData("shared", "should_select_by_assigned");

    IssueQuery query = IssueQuery.builder().assigned(true).requiredRole("user").build();
    List<IssueDto> issues = newArrayList(dao.selectIssueIds(query));
    assertThat(issues).hasSize(2);

    query = IssueQuery.builder().assigned(false).requiredRole("user").build();
    issues = newArrayList(dao.selectIssueIds(query));
    assertThat(issues).hasSize(1);

    query = IssueQuery.builder().assigned(null).requiredRole("user").build();
    issues = newArrayList(dao.selectIssueIds(query));
    assertThat(issues).hasSize(3);
  }

  @Test
  public void should_select_by_planned() {
    setupData("shared", "should_select_by_planned");

    IssueQuery query = IssueQuery.builder().planned(true).requiredRole("user").build();
    List<IssueDto> issues = newArrayList(dao.selectIssueIds(query));
    assertThat(issues).hasSize(2);

    query = IssueQuery.builder().planned(false).requiredRole("user").build();
    issues = newArrayList(dao.selectIssueIds(query));
    assertThat(issues).hasSize(1);

    query = IssueQuery.builder().planned(null).requiredRole("user").build();
    issues = newArrayList(dao.selectIssueIds(query));
    assertThat(issues).hasSize(3);
  }

  @Test
  public void should_select_by_resolved() {
    setupData("shared", "should_select_by_resolved");

    IssueQuery query = IssueQuery.builder().resolved(true).requiredRole("user").build();
    List<IssueDto> issues = newArrayList(dao.selectIssueIds(query));
    assertThat(issues).hasSize(2);

    query = IssueQuery.builder().resolved(false).requiredRole("user").build();
    issues = newArrayList(dao.selectIssueIds(query));
    assertThat(issues).hasSize(1);

    query = IssueQuery.builder().resolved(null).requiredRole("user").build();
    issues = newArrayList(dao.selectIssueIds(query));
    assertThat(issues).hasSize(3);
  }

  @Test
  public void should_select_by_action_plans() {
    setupData("shared", "should_select_by_action_plans");

    IssueQuery query = IssueQuery.builder().actionPlans(newArrayList("ABC")).requiredRole("user").build();
    assertThat(dao.selectIssueIds(query)).hasSize(2);

    query = IssueQuery.builder().actionPlans(newArrayList("ABC", "DEF")).requiredRole("user").build();
    assertThat(dao.selectIssueIds(query)).hasSize(3);

    query = IssueQuery.builder().actionPlans(newArrayList("<Unkown>")).requiredRole("user").build();
    assertThat(dao.selectIssueIds(query)).isEmpty();
  }

  @Test
  public void should_select_issues_for_authorized_projects() {
    setupData("should_select_issues_for_authorized_projects");

    IssueQuery query = IssueQuery.builder().requiredRole("user").build();
    List<IssueDto> results = dao.selectIssueIds(query, 100, 10);
    assertThat(results).hasSize(2);

    results = dao.selectIssueIds(query, null, 10);
    assertThat(results).isEmpty();
  }

  @Test
  public void should_select_issues_return_limited_results() {
    setupData("shared", "should_select_issues_return_limited_results");

    IssueQuery query = IssueQuery.builder().requiredRole("user").build();
    List<IssueDto> results = dao.selectIssueIds(query, null, 2);
    assertThat(results).hasSize(2);
  }

  @Test
  public void should_select_issues_with_sort_column() {
    setupData("shared", "should_select_issues_with_sort_column");

    IssueQuery query = IssueQuery.builder().sort(IssueQuery.SORT_BY_ASSIGNEE).requiredRole("user").build();
    List<IssueDto> results = dao.selectIssueIds(query);
    assertThat(results.get(0).getAssignee()).isNotNull();

    query = IssueQuery.builder().sort(IssueQuery.SORT_BY_SEVERITY).requiredRole("user").build();
    results = dao.selectIssueIds(query);
    assertThat(results.get(0).getSeverity()).isNotNull();

    query = IssueQuery.builder().sort(IssueQuery.SORT_BY_STATUS).requiredRole("user").build();
    results = dao.selectIssueIds(query);
    assertThat(results.get(0).getStatus()).isNotNull();

    query = IssueQuery.builder().sort(IssueQuery.SORT_BY_CREATION_DATE).requiredRole("user").build();
    results = dao.selectIssueIds(query);
    assertThat(results.get(0).getIssueCreationDate()).isNotNull();

    query = IssueQuery.builder().sort(IssueQuery.SORT_BY_UPDATE_DATE).requiredRole("user").build();
    results = dao.selectIssueIds(query);
    assertThat(results.get(0).getIssueUpdateDate()).isNotNull();

    query = IssueQuery.builder().sort(IssueQuery.SORT_BY_CLOSE_DATE).requiredRole("user").build();
    results = dao.selectIssueIds(query);
    assertThat(results.get(0).getIssueCloseDate()).isNotNull();
  }

  @Test
  public void should_select_non_closed_issues_by_module() {
    setupData("shared", "should_select_non_closed_issues_by_module");

    // 400 is a non-root module, we should find 2 issues from classes and one on itself
    DefaultResultHandler handler = new DefaultResultHandler();
    dao.selectNonClosedIssuesByModule(400, handler);
    assertThat(handler.getResultList()).hasSize(3);

    IssueDto issue = (IssueDto) handler.getResultList().get(0);
    assertThat(issue.getRuleRepo()).isNotNull();
    assertThat(issue.getRule()).isNotNull();
    assertThat(issue.getComponentKey()).isNotNull();

    // 399 is the root module, we should only find 1 issue on itself
    handler = new DefaultResultHandler();
    dao.selectNonClosedIssuesByModule(399, handler);
    assertThat(handler.getResultList()).hasSize(1);
  }

  @Test
  public void should_select_by_ids() {
    setupData("shared", "should_select_by_ids");

    List<IssueDto> results = newArrayList(dao.selectByIds(newArrayList(100l, 101l, 102l)));
    assertThat(results).hasSize(3);
  }

  @Test
  public void should_select_by_ids_with_huge_number_of_ids() {
    setupData("shared");

    List<Long> hugeNbOfIssues = newArrayList();
    for (long i=0; i<4500; i++) {
      hugeNbOfIssues.add(i);
    }
    List<IssueDto> results = dao.selectByIds(hugeNbOfIssues);

    // The goal of this test is only to check that the query do no fail, not to check the number of results
    assertThat(results).isEmpty();
  }

  @Test
  public void should_select_issues_all_columns() {
    setupData("shared", "should_return_all_columns");

    IssueQuery query = IssueQuery.builder().requiredRole("user").build();

    List<IssueDto> results = dao.selectIssues(query);
    assertThat(results).hasSize(1);
    IssueDto issue = results.get(0);
    assertThat(issue.getKee()).isEqualTo("ABCDE");
    assertThat(issue.getId()).isEqualTo(100L);
    assertThat(issue.getComponentId()).isEqualTo(401);
    assertThat(issue.getRootComponentId()).isEqualTo(399);
    assertThat(issue.getRuleId()).isEqualTo(500);
    assertThat(issue.getSeverity()).isEqualTo("BLOCKER");
    assertThat(issue.isManualSeverity()).isFalse();
    assertThat(issue.getMessage()).isNull();
    assertThat(issue.getLine()).isEqualTo(200);
    assertThat(issue.getEffortToFix()).isEqualTo(4.2);
    assertThat(issue.getStatus()).isEqualTo("OPEN");
    assertThat(issue.getResolution()).isEqualTo("FIXED");
    assertThat(issue.getChecksum()).isEqualTo("XXX");
    assertThat(issue.getAuthorLogin()).isEqualTo("karadoc");
    assertThat(issue.getReporter()).isEqualTo("arthur");
    assertThat(issue.getAssignee()).isEqualTo("perceval");
    assertThat(issue.getIssueAttributes()).isEqualTo("JIRA=FOO-1234");
    assertThat(issue.getIssueCreationDate()).isNotNull();
    assertThat(issue.getIssueUpdateDate()).isNotNull();
    assertThat(issue.getIssueCloseDate()).isNotNull();
    assertThat(issue.getCreatedAt()).isNotNull();
    assertThat(issue.getUpdatedAt()).isNotNull();
    assertThat(issue.getRuleRepo()).isEqualTo("squid");
    assertThat(issue.getRule()).isEqualTo("AvoidCycle");
    assertThat(issue.getComponentKey()).isEqualTo("Action.java");
    assertThat(issue.getRootComponentKey()).isEqualTo("struts");
  }

  private List<Long> getIssueIds(List<IssueDto> issues) {
    return newArrayList(Iterables.transform(issues, new Function<IssueDto, Long>() {
      @Override
      public Long apply(IssueDto input) {
        return input.getId();
      }
    }));
  }
}
