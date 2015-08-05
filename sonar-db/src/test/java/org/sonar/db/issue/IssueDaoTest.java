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

package org.sonar.db.issue;

import java.util.List;
import org.apache.ibatis.executor.result.DefaultResultHandler;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.RowNotFoundException;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.test.DbTests;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@Category(DbTests.class)
public class IssueDaoTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  IssueDao dao = dbTester.getDbClient().issueDao();

  @Test
  public void select_non_closed_issues_by_module() {
    dbTester.prepareDbUnit(getClass(), "shared.xml", "should_select_non_closed_issues_by_module.xml");

    // 400 is a non-root module, we should find 2 issues from classes and one on itself
    DefaultResultHandler handler = new DefaultResultHandler();
    dao.selectNonClosedIssuesByModule(400, handler);
    assertThat(handler.getResultList()).hasSize(3);

    IssueDto issue = (IssueDto) handler.getResultList().get(0);
    assertThat(issue.getRuleRepo()).isNotNull();
    assertThat(issue.getRule()).isNotNull();
    assertThat(issue.getComponentKey()).isNotNull();
    assertThat(issue.getProjectKey()).isEqualTo("struts");

    // 399 is the root module, we should only find 1 issue on itself
    handler = new DefaultResultHandler();
    dao.selectNonClosedIssuesByModule(399, handler);
    assertThat(handler.getResultList()).hasSize(1);

    issue = (IssueDto) handler.getResultList().get(0);
    assertThat(issue.getComponentKey()).isEqualTo("struts");
    assertThat(issue.getProjectKey()).isEqualTo("struts");
  }

  /**
   * SONAR-5218
   */
  @Test
  public void select_non_closed_issues_by_module_on_removed_project() {
    // All issues are linked on a project that is not existing anymore

    dbTester.prepareDbUnit(getClass(), "shared.xml", "should_select_non_closed_issues_by_module_on_removed_project.xml");

    // 400 is a non-root module, we should find 2 issues from classes and one on itself
    DefaultResultHandler handler = new DefaultResultHandler();
    dao.selectNonClosedIssuesByModule(400, handler);
    assertThat(handler.getResultList()).hasSize(3);

    IssueDto issue = (IssueDto) handler.getResultList().get(0);
    assertThat(issue.getRuleRepo()).isNotNull();
    assertThat(issue.getRule()).isNotNull();
    assertThat(issue.getComponentKey()).isNotNull();
    assertThat(issue.getProjectKey()).isNull();
  }

  @Test
  public void selectByKeyOrFail() {
    dbTester.prepareDbUnit(getClass(), "shared.xml", "get_by_key.xml");

    IssueDto issue = dao.selectOrFailByKey(dbTester.getSession(), "I1");
    assertThat(issue.getKee()).isEqualTo("I1");
    assertThat(issue.getId()).isEqualTo(1L);
    assertThat(issue.getComponentUuid()).isEqualTo("CDEF");
    assertThat(issue.getProjectUuid()).isEqualTo("ABCD");
    assertThat(issue.getRuleId()).isEqualTo(500);
    assertThat(issue.getLanguage()).isEqualTo("java");
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
    assertThat(issue.getCreatedAt()).isEqualTo(1400000000000L);
    assertThat(issue.getUpdatedAt()).isEqualTo(1450000000000L);
    assertThat(issue.getRuleRepo()).isEqualTo("squid");
    assertThat(issue.getRule()).isEqualTo("AvoidCycle");
    assertThat(issue.getComponentKey()).isEqualTo("Action.java");
    assertThat(issue.getProjectKey()).isEqualTo("struts");
    assertThat(issue.getLocations()).isNull();
    assertThat(issue.parseLocations()).isNull();
  }

  @Test
  public void selectByKeyOrFail_fails_if_key_not_found() {
    thrown.expect(RowNotFoundException.class);
    thrown.expectMessage("Issue with key 'DOES_NOT_EXIST' does not exist");

    dbTester.prepareDbUnit(getClass(), "shared.xml", "get_by_key.xml");

    dao.selectOrFailByKey(dbTester.getSession(), "DOES_NOT_EXIST");
  }

  @Test
  public void selectByKeys() {
    dbTester.prepareDbUnit(getClass(), "shared.xml", "get_by_key.xml");

    List<IssueDto> issues = dao.selectByKeys(dbTester.getSession(), asList("I1", "I2", "I3"));
    // results are not ordered, so do not use "containsExactly"
    assertThat(issues).extracting("key").containsOnly("I1", "I2");
  }

  @Test
  public void selectByOrderedKeys() {
    dbTester.prepareDbUnit(getClass(), "shared.xml", "get_by_key.xml");

    Iterable<IssueDto> issues = dao.selectByOrderedKeys(dbTester.getSession(), asList("I1", "I2", "I3"));
    assertThat(issues).extracting("key").containsExactly("I1", "I2");

    issues = dao.selectByOrderedKeys(dbTester.getSession(), asList("I2", "I3", "I1"));
    assertThat(issues).extracting("key").containsExactly("I2", "I1");
  }

  @Test
  public void selectByActionPlan() {
    dbTester.prepareDbUnit(getClass(), "shared.xml", "find_by_action_plan.xml");

    List<IssueDto> issues = dao.selectByActionPlan(dbTester.getSession(), "AP-1");
    assertThat(issues).hasSize(1);

    IssueDto issue = issues.get(0);
    assertThat(issue.getKee()).isEqualTo("ABCDE");
    assertThat(issue.getActionPlanKey()).isEqualTo("AP-1");
    assertThat(issue.getComponentUuid()).isEqualTo("CDEF");
    assertThat(issue.getProjectUuid()).isEqualTo("ABCD");
    assertThat(issue.getRuleId()).isEqualTo(500);
    assertThat(issue.getLanguage()).isEqualTo("java");
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
    assertThat(issue.getProjectKey()).isEqualTo("struts");
  }

  @Test
  public void insert() {
    dbTester.truncateTables();

    IssueDto dto = new IssueDto();
    dto.setComponent(new ComponentDto().setKey("struts:Action").setId(123L).setUuid("component-uuid"));
    dto.setProject(new ComponentDto().setKey("struts").setId(100L).setUuid("project-uuid"));
    dto.setRule(RuleTesting.newDto(RuleKey.of("squid", "S001")).setId(200));
    dto.setKee("ABCDE");
    dto.setLine(500);
    dto.setEffortToFix(3.14);
    dto.setDebt(10L);
    dto.setResolution("FIXED");
    dto.setStatus("RESOLVED");
    dto.setSeverity("BLOCKER");
    dto.setReporter("emmerik");
    dto.setAuthorLogin("morgan");
    dto.setAssignee("karadoc");
    dto.setActionPlanKey("current_sprint");
    dto.setIssueAttributes("JIRA=FOO-1234");
    dto.setChecksum("123456789");
    dto.setMessage("the message");

    dto.setIssueCreationTime(1_500_000_000_000L);
    dto.setIssueUpdateTime(1_500_000_000_001L);
    dto.setIssueCloseTime(1_500_000_000_002L);
    dto.setCreatedAt(1_400_000_000_000L);
    dto.setUpdatedAt(1_450_000_000_000L);

    dao.insert(dbTester.getSession(), dto);
    dbTester.getSession().commit();

    dbTester.assertDbUnit(getClass(), "insert-result.xml", new String[] {"id"}, "issues");
  }
}
