/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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

import org.apache.ibatis.executor.result.DefaultResultHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.persistence.DbSession;

import static org.fest.assertions.Assertions.assertThat;

public class IssueDaoTest extends AbstractDaoTestCase {

  DbSession session;

  IssueDao dao;

  @Before
  public void createDao() {
    session = getMyBatis().openSession(false);
    dao = new IssueDao(getMyBatis());
  }

  @After
  public void tearDown() throws Exception {
    session.close();
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
  public void select_non_closed_issues_by_module() {
    setupData("shared", "should_select_non_closed_issues_by_module");

    // 400 is a non-root module, we should find 2 issues from classes and one on itself
    DefaultResultHandler handler = new DefaultResultHandler();
    dao.selectNonClosedIssuesByModule(400, handler);
    assertThat(handler.getResultList()).hasSize(3);

    IssueDto issue = (IssueDto) handler.getResultList().get(0);
    assertThat(issue.getRuleRepo()).isNotNull();
    assertThat(issue.getRule()).isNotNull();
    assertThat(issue.getComponentKey()).isNotNull();
    assertThat(issue.getRootComponentKey()).isEqualTo("struts");

    // 399 is the root module, we should only find 1 issue on itself
    handler = new DefaultResultHandler();
    dao.selectNonClosedIssuesByModule(399, handler);
    assertThat(handler.getResultList()).hasSize(1);

    issue = (IssueDto) handler.getResultList().get(0);
    assertThat(issue.getComponentKey()).isEqualTo("struts");
    assertThat(issue.getRootComponentKey()).isEqualTo("struts");
  }

  /**
   * SONAR-5218
   */
  @Test
  public void select_non_closed_issues_by_module_on_removed_project() {
    // All issues are linked on a project that is not existing anymore

    setupData("shared", "should_select_non_closed_issues_by_module_on_removed_project");

    // 400 is a non-root module, we should find 2 issues from classes and one on itself
    DefaultResultHandler handler = new DefaultResultHandler();
    dao.selectNonClosedIssuesByModule(400, handler);
    assertThat(handler.getResultList()).hasSize(3);

    IssueDto issue = (IssueDto) handler.getResultList().get(0);
    assertThat(issue.getRuleRepo()).isNotNull();
    assertThat(issue.getRule()).isNotNull();
    assertThat(issue.getComponentKey()).isNotNull();
    assertThat(issue.getRootComponentKey()).isNull();
  }

  @Test
  public void find_rules_by_component() {
    setupData("shared", "find_rules_by_component");

    assertThat(dao.findRulesByComponent("Action.java", null, session)).hasSize(3);
    assertThat(dao.findRulesByComponent("Action.java", DateUtils.parseDate("2013-04-17"), session)).hasSize(2);
  }

  @Test
  public void find_severities_by_component() {
    setupData("shared", "find_severities_by_component");

    assertThat(dao.findSeveritiesByComponent("Action.java", null, session)).containsExactly(Severity.BLOCKER, Severity.MAJOR, Severity.BLOCKER);
    assertThat(dao.findSeveritiesByComponent("Action.java", DateUtils.parseDate("2013-04-17"), session)).containsExactly(Severity.MAJOR);
  }

}
