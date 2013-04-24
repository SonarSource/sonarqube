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

package org.sonar.core.issue;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.issue.IssueQuery;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.persistence.AbstractDaoTestCase;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;


public class IssueDaoTest extends AbstractDaoTestCase {

  private IssueDao dao;

  @Before
  public void createDao() {
    dao = new IssueDao(getMyBatis());
  }

  @Test
  public void should_insert() {
    setupData("insert");

    IssueDto issueDto = new IssueDto();
    issueDto.setKey("100");
    issueDto.setResourceId(400);
    issueDto.setRuleId(12);
    issueDto.setSeverity("BLOCKER");
    issueDto.setLine(200);
    issueDto.setStatus("OPEN");
    issueDto.setAssignee("user");
    issueDto.setDescription("the description");
    issueDto.setCost(10.0);
    issueDto.setChecksum("checksum");
    issueDto.setAuthorLogin("arthur");

    Date today = new Date();
    issueDto.setCreatedAt(today);
    issueDto.setUpdatedAt(today);
    issueDto.setClosedAt(today);

    dao.insert(issueDto);

    checkTables("insert", new String[]{"id", "created_at", "updated_at", "closed_at"}, "issues");
  }

  @Test
  public void update() {
    setupData("shared", "update");
    Collection<IssueDto> issues = newArrayList(dao.selectById(100L));
    IssueDto issue = issues.iterator().next();
    issue.setLine(1000);
    issue.setResolution("NEW_RESOLUTION");
    issue.setStatus("NEW_STATUS");
    issue.setSeverity("NEW_SEV");
    issue.setAssignee("new_user");
    issue.setManualSeverity(true);
    issue.setManualIssue(false);
    issue.setCreatedAt(DateUtils.parseDate("2012-05-18"));
    issue.setUpdatedAt(DateUtils.parseDate("2012-07-01"));
    issue.setAttributes("big=bang");

    dao.update(issues);

    checkTables("update", "issues");
  }

  @Test
  public void should_select_by_id() {
    setupData("shared", "should_select_by_id");
    IssueDto issue = dao.selectById(100L);
    assertThat(issue.getId()).isEqualTo(100L);
    assertThat(issue.getKey()).isEqualTo("ABCDE");
    assertThat(issue.getResourceId()).isEqualTo(400);
    assertThat(issue.getRuleId()).isEqualTo(500);
    assertThat(issue.getSeverity()).isEqualTo("BLOCKER");
    assertThat(issue.isManualSeverity()).isFalse();
    assertThat(issue.isManualIssue()).isFalse();
    assertThat(issue.getDescription()).isNull();
    assertThat(issue.getLine()).isEqualTo(200);
    assertThat(issue.getCost()).isEqualTo(4.2);
    assertThat(issue.getStatus()).isEqualTo("OPEN");
    assertThat(issue.getResolution()).isEqualTo("FIXED");
    assertThat(issue.getChecksum()).isEqualTo("XXX");
    assertThat(issue.getAuthorLogin()).isEqualTo("pierre");
    assertThat(issue.getUserLogin()).isEqualTo("arthur");
    assertThat(issue.getAssignee()).isEqualTo("perceval");
    assertThat(issue.getAttributes()).isEqualTo("JIRA=FOO-1234");
    assertThat(issue.getCreatedAt()).isNotNull();
    assertThat(issue.getUpdatedAt()).isNotNull();
    assertThat(issue.getClosedAt()).isNotNull();
    assertThat(issue.getRuleRepo()).isEqualTo("squid");
    assertThat(issue.getRule()).isEqualTo("AvoidCycle");
    assertThat(issue.getComponentKey()).isEqualTo("Action.java");
  }

  @Test
  public void should_select_by_key() {
    setupData("shared", "should_select_by_key");

    IssueDto issue = dao.selectByKey("ABCDE");
    assertThat(issue.getKey()).isEqualTo("ABCDE");
    assertThat(issue.getId()).isEqualTo(100);
    assertThat(issue.getRuleRepo()).isEqualTo("squid");
    assertThat(issue.getRule()).isEqualTo("AvoidCycle");
    assertThat(issue.getComponentKey()).isEqualTo("Action.java");
  }

  @Test
  public void should_select_by_query() {
    setupData("shared", "should_select_by_query");

    IssueQuery query = IssueQuery.builder()
      .keys(newArrayList("ABCDE"))
      .userLogins(newArrayList("arthur", "otherguy"))
      .assignees(newArrayList("perceval", "otherguy"))
      .components(newArrayList("Action.java"))
      .resolutions(newArrayList("FIXED"))
      .severities(newArrayList("BLOCKER"))
      .rules(newArrayList(RuleKey.of("squid", "AvoidCycle")))
      .build();

    assertThat(dao.select(query)).hasSize(1);

    IssueDto issue = dao.select(query).get(0);
    assertThat(issue.getId()).isEqualTo(100);
    assertThat(issue.getRuleRepo()).isEqualTo("squid");
    assertThat(issue.getRule()).isEqualTo("AvoidCycle");
    assertThat(issue.getComponentKey()).isEqualTo("Action.java");
  }

  @Test
  public void should_select_by_rules() {
    setupData("shared", "should_select_by_rules");

    IssueQuery query = IssueQuery.builder().rules(newArrayList(RuleKey.of("squid", "AvoidCycle"))).build();
    assertThat(dao.select(query)).hasSize(2);

    query = IssueQuery.builder().rules(newArrayList(RuleKey.of("squid", "AvoidCycle"), RuleKey.of("squid", "NullRef"))).build();
    assertThat(dao.select(query)).hasSize(3);

    query = IssueQuery.builder().rules(newArrayList(RuleKey.of("squid", "Other"))).build();
    assertThat(dao.select(query)).isEmpty();
  }

  @Test
  public void should_select_by_date_creation() {
    setupData("shared", "should_select_by_date_creation");

    IssueQuery query = IssueQuery.builder().createdAfter(DateUtils.parseDate("2013-04-15")).build();
    assertThat(dao.select(query)).hasSize(1);

    query = IssueQuery.builder().createdBefore(DateUtils.parseDate("2013-04-17")).build();
    assertThat(dao.select(query)).hasSize(1);
  }

  @Test
  public void should_select_by_component_root() {
    setupData("shared", "should_select_by_component_root");

    IssueQuery query = IssueQuery.builder().componentRoots(newArrayList("struts")).build();
    List<IssueDto> issues = newArrayList(dao.select(query));
    assertThat(issues).hasSize(2);
    assertThat(issues.get(0).getId()).isEqualTo(100);
    assertThat(issues.get(1).getId()).isEqualTo(101);
  }

  @Test
  public void should_select_all() {
    setupData("shared", "should_select_all");

    IssueQuery query = IssueQuery.builder().build();
    assertThat(dao.select(query)).hasSize(3);
  }

  @Test
  public void should_select_issue_ids_and_components_ids() {
    setupData("shared", "should_select_issue_ids_and_components_ids");

    IssueQuery query = IssueQuery.builder().build();
    List<IssueDto> results = dao.selectIssueIdsAndComponentsId(query, getMyBatis().openSession());
    assertThat(results).hasSize(3);
  }

  @Test
  public void should_select_open_issues() {
    setupData("shared", "should_select_open_issues");

    List<IssueDto> dtos = dao.selectOpenIssues(399);
    assertThat(dtos).hasSize(2);

    IssueDto issue = dtos.get(0);
    assertThat(issue.getRuleRepo()).isNotNull();
    assertThat(issue.getRule()).isNotNull();
    assertThat(issue.getComponentKey()).isNotNull();
  }

  @Test
  public void should_select_by_ids() {
    setupData("shared", "should_select_by_ids");

    Collection<IssueDto> results = dao.selectByIds(newArrayList(100l, 101l, 102l), getMyBatis().openSession());
    assertThat(results).hasSize(3);
  }

}
