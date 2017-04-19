/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.db.issue;

import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.RowNotFoundException;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleTesting;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class IssueDaoTest {

  private static final String PROJECT_UUID = "prj_uuid";
  private static final String PROJECT_KEY = "prj_key";
  private static final String FILE_UUID = "file_uuid";
  private static final String FILE_KEY = "file_key";
  private static final RuleDto RULE = RuleTesting.newXooX1();
  private static final String ISSUE_KEY1 = "I1";
  private static final String ISSUE_KEY2 = "I2";

  @Rule
  public ExpectedException thrown = ExpectedException.none();
  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private IssueDao underTest = dbTester.getDbClient().issueDao();

  @Test
  public void selectByKeyOrFail() {
    prepareTables();

    IssueDto issue = underTest.selectOrFailByKey(dbTester.getSession(), ISSUE_KEY1);
    assertThat(issue.getKee()).isEqualTo(ISSUE_KEY1);
    assertThat(issue.getId()).isGreaterThan(0L);
    assertThat(issue.getComponentUuid()).isEqualTo(FILE_UUID);
    assertThat(issue.getProjectUuid()).isEqualTo(PROJECT_UUID);
    assertThat(issue.getRuleId()).isEqualTo(RULE.getId());
    assertThat(issue.getLanguage()).isEqualTo(RULE.getLanguage());
    assertThat(issue.getSeverity()).isEqualTo("BLOCKER");
    assertThat(issue.getType()).isEqualTo(2);
    assertThat(issue.isManualSeverity()).isFalse();
    assertThat(issue.getMessage()).isEqualTo("the message");
    assertThat(issue.getLine()).isEqualTo(500);
    assertThat(issue.getEffort()).isEqualTo(10L);
    assertThat(issue.getGap()).isEqualTo(3.14);
    assertThat(issue.getStatus()).isEqualTo("RESOLVED");
    assertThat(issue.getResolution()).isEqualTo("FIXED");
    assertThat(issue.getChecksum()).isEqualTo("123456789");
    assertThat(issue.getAuthorLogin()).isEqualTo("morgan");
    assertThat(issue.getAssignee()).isEqualTo("karadoc");
    assertThat(issue.getIssueAttributes()).isEqualTo("JIRA=FOO-1234");
    assertThat(issue.getIssueCreationDate()).isNotNull();
    assertThat(issue.getIssueUpdateDate()).isNotNull();
    assertThat(issue.getIssueCloseDate()).isNotNull();
    assertThat(issue.getCreatedAt()).isEqualTo(1_440_000_000_000L);
    assertThat(issue.getUpdatedAt()).isEqualTo(1_440_000_000_000L);
    assertThat(issue.getRuleRepo()).isEqualTo(RULE.getRepositoryKey());
    assertThat(issue.getRule()).isEqualTo(RULE.getRuleKey());
    assertThat(issue.getComponentKey()).isEqualTo(FILE_KEY);
    assertThat(issue.getProjectKey()).isEqualTo(PROJECT_KEY);
    assertThat(issue.getLocations()).isNull();
    assertThat(issue.parseLocations()).isNull();
  }

  @Test
  public void selectByKeyOrFail_fails_if_key_not_found() {
    thrown.expect(RowNotFoundException.class);
    thrown.expectMessage("Issue with key 'DOES_NOT_EXIST' does not exist");

    prepareTables();

    underTest.selectOrFailByKey(dbTester.getSession(), "DOES_NOT_EXIST");
  }

  @Test
  public void selectByKeys() {
    // contains I1 and I2
    prepareTables();

    List<IssueDto> issues = underTest.selectByKeys(dbTester.getSession(), asList("I1", "I2", "I3"));
    // results are not ordered, so do not use "containsExactly"
    assertThat(issues).extracting("key").containsOnly("I1", "I2");
  }

  @Test
  public void selectByOrderedKeys() {
    // contains I1 and I2
    prepareTables();

    Iterable<IssueDto> issues = underTest.selectByOrderedKeys(dbTester.getSession(), asList("I1", "I2", "I3"));
    assertThat(issues).extracting("key").containsExactly("I1", "I2");

    issues = underTest.selectByOrderedKeys(dbTester.getSession(), asList("I2", "I3", "I1"));
    assertThat(issues).extracting("key").containsExactly("I2", "I1");
  }

  private static IssueDto newIssueDto(String key) {
    IssueDto dto = new IssueDto();
    dto.setComponent(new ComponentDto().setKey("struts:Action").setId(123L).setUuid("component-uuid"));
    dto.setProject(new ComponentDto().setKey("struts").setId(100L).setUuid("project-uuid"));
    dto.setRule(RuleTesting.newRule(RuleKey.of("squid", "S001")).setId(200));
    dto.setKee(key);
    dto.setType(2);
    dto.setLine(500);
    dto.setGap(3.14);
    dto.setEffort(10L);
    dto.setResolution("FIXED");
    dto.setStatus("RESOLVED");
    dto.setSeverity("BLOCKER");
    dto.setAuthorLogin("morgan");
    dto.setAssignee("karadoc");
    dto.setIssueAttributes("JIRA=FOO-1234");
    dto.setChecksum("123456789");
    dto.setMessage("the message");
    dto.setCreatedAt(1_440_000_000_000L);
    dto.setUpdatedAt(1_440_000_000_000L);
    dto.setIssueCreationTime(1_450_000_000_000L);
    dto.setIssueUpdateTime(1_450_000_000_000L);
    dto.setIssueCloseTime(1_450_000_000_000L);
    return dto;
  }

  private void prepareTables() {
    dbTester.rules().insertRule(RULE);
    OrganizationDto organizationDto = dbTester.organizations().insert();
    ComponentDto projectDto = dbTester.components().insertPrivateProject(organizationDto, (t) -> t.setUuid(PROJECT_UUID).setKey(PROJECT_KEY));
    dbTester.components().insertComponent(ComponentTesting.newFileDto(projectDto).setUuid(FILE_UUID).setKey(FILE_KEY));
    underTest.insert(dbTester.getSession(), newIssueDto(ISSUE_KEY1)
      .setMessage("the message")
      .setRuleId(RULE.getId())
      .setComponentUuid(FILE_UUID)
      .setProjectUuid(PROJECT_UUID));
    underTest.insert(dbTester.getSession(), newIssueDto(ISSUE_KEY2)
      .setRuleId(RULE.getId())
      .setComponentUuid(FILE_UUID)
      .setProjectUuid(PROJECT_UUID));
    dbTester.getSession().commit();
  }
}
