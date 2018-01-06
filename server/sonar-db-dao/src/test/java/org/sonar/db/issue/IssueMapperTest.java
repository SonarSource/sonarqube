/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleTesting;

import static org.assertj.core.api.Assertions.assertThat;

public class IssueMapperTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  DbSession dbSession = dbTester.getSession();

  IssueMapper underTest = dbSession.getMapper(IssueMapper.class);

  ComponentDto project, file, file2;
  RuleDto rule;

  @Before
  public void setUp() throws Exception {
    OrganizationDto organizationDto = dbTester.organizations().insert();
    project = ComponentTesting.newPrivateProjectDto(organizationDto);
    dbTester.getDbClient().componentDao().insert(dbSession, project);
    file = ComponentTesting.newFileDto(project, null);
    dbTester.getDbClient().componentDao().insert(dbSession, file);
    file2 = ComponentTesting.newFileDto(project, null).setUuid("file2 uuid");
    dbTester.getDbClient().componentDao().insert(dbSession, file2);
    rule = RuleTesting.newXooX1();
    dbTester.rules().insertRule(rule);
    dbSession.commit();
  }

  @Test
  public void insert() {
    underTest.insert(newIssue());
    dbTester.getSession().commit();

    IssueDto result = underTest.selectByKey("ABCDE");
    assertThat(result).isNotNull();
    assertThat(result.getId()).isNotNull();
    assertThat(result.getKey()).isEqualTo("ABCDE");
    assertThat(result.getComponentUuid()).isEqualTo(file.uuid());
    assertThat(result.getProjectUuid()).isEqualTo(project.uuid());
    assertThat(result.getRuleId()).isEqualTo(rule.getId());
    assertThat(result.getType()).isEqualTo(2);
    assertThat(result.getLine()).isEqualTo(500);
    assertThat(result.getGap()).isEqualTo(3.14d);
    assertThat(result.getEffort()).isEqualTo(10L);
    assertThat(result.getResolution()).isEqualTo("FIXED");
    assertThat(result.getStatus()).isEqualTo("RESOLVED");
    assertThat(result.getSeverity()).isEqualTo("BLOCKER");
    assertThat(result.getAuthorLogin()).isEqualTo("morgan");
    assertThat(result.getAssignee()).isEqualTo("karadoc");
    assertThat(result.getIssueAttributes()).isEqualTo("JIRA=FOO-1234");
    assertThat(result.getChecksum()).isEqualTo("123456789");
    assertThat(result.getMessage()).isEqualTo("the message");
    assertThat(result.getIssueCreationTime()).isEqualTo(1_401_000_000_000L);
    assertThat(result.getIssueUpdateTime()).isEqualTo(1_402_000_000_000L);
    assertThat(result.getIssueCloseTime()).isEqualTo(1_403_000_000_000L);
    assertThat(result.getCreatedAt()).isEqualTo(1_400_000_000_000L);
    assertThat(result.getUpdatedAt()).isEqualTo(1_500_000_000_000L);
  }

  @Test
  public void update() {
    underTest.insert(newIssue());

    dbTester.getSession().commit();

    IssueDto update = new IssueDto();
    update.setKee("ABCDE");
    update.setComponentUuid("other component uuid");
    update.setProjectUuid(project.uuid());
    update.setRuleId(rule.getId());
    update.setType(3);
    update.setLine(500);
    update.setGap(3.14);
    update.setEffort(10L);
    update.setResolution("FIXED");
    update.setStatus("RESOLVED");
    update.setSeverity("BLOCKER");
    update.setAuthorLogin("morgan");
    update.setAssignee("karadoc");
    update.setIssueAttributes("JIRA=FOO-1234");
    update.setChecksum("123456789");
    update.setMessage("the message");

    update.setIssueCreationTime(1_550_000_000_000L);
    update.setIssueUpdateTime(1_550_000_000_000L);
    update.setIssueCloseTime(1_550_000_000_000L);
    // Should not change
    update.setCreatedAt(1_400_123_456_789L);
    update.setUpdatedAt(1_550_000_000_000L);

    underTest.update(update);
    dbTester.getSession().commit();

    IssueDto result = underTest.selectByKey("ABCDE");
    assertThat(result).isNotNull();
    assertThat(result.getId()).isNotNull();
    assertThat(result.getKey()).isEqualTo("ABCDE");
    assertThat(result.getComponentUuid()).isEqualTo(file.uuid());
    assertThat(result.getProjectUuid()).isEqualTo(project.uuid());
    assertThat(result.getRuleId()).isEqualTo(rule.getId());
    assertThat(result.getType()).isEqualTo(3);
    assertThat(result.getLine()).isEqualTo(500);
    assertThat(result.getGap()).isEqualTo(3.14d);
    assertThat(result.getEffort()).isEqualTo(10L);
    assertThat(result.getResolution()).isEqualTo("FIXED");
    assertThat(result.getStatus()).isEqualTo("RESOLVED");
    assertThat(result.getSeverity()).isEqualTo("BLOCKER");
    assertThat(result.getAuthorLogin()).isEqualTo("morgan");
    assertThat(result.getAssignee()).isEqualTo("karadoc");
    assertThat(result.getIssueAttributes()).isEqualTo("JIRA=FOO-1234");
    assertThat(result.getChecksum()).isEqualTo("123456789");
    assertThat(result.getMessage()).isEqualTo("the message");
    assertThat(result.getIssueCreationTime()).isEqualTo(1_550_000_000_000L);
    assertThat(result.getIssueUpdateTime()).isEqualTo(1_550_000_000_000L);
    assertThat(result.getIssueCloseTime()).isEqualTo(1_550_000_000_000L);
    assertThat(result.getCreatedAt()).isEqualTo(1_400_000_000_000L);
    assertThat(result.getUpdatedAt()).isEqualTo(1_550_000_000_000L);
  }

  @Test
  public void updateBeforeSelectedDate_without_conflict() {
    underTest.insert(newIssue());

    IssueDto dto = newIssue()
      .setComponentUuid(file2.uuid())
      .setType(3)
      .setLine(600)
      .setGap(1.12d)
      .setEffort(50L)
      .setIssueUpdateTime(1_600_000_000_000L)
      .setUpdatedAt(1_600_000_000_000L);

    // selected after last update -> ok
    dto.setSelectedAt(1500000000000L);

    int count = underTest.updateIfBeforeSelectedDate(dto);
    assertThat(count).isEqualTo(1);
    dbTester.getSession().commit();

    IssueDto result = underTest.selectByKey("ABCDE");
    assertThat(result).isNotNull();
    assertThat(result.getComponentUuid()).isEqualTo(file2.uuid());
    assertThat(result.getType()).isEqualTo(3);
    assertThat(result.getLine()).isEqualTo(600);
    assertThat(result.getGap()).isEqualTo(1.12d);
    assertThat(result.getEffort()).isEqualTo(50L);
    assertThat(result.getIssueUpdateTime()).isEqualTo(1_600_000_000_000L);
    assertThat(result.getUpdatedAt()).isEqualTo(1_600_000_000_000L);
  }

  @Test
  public void updateBeforeSelectedDate_with_conflict() {
    underTest.insert(newIssue());

    IssueDto dto = newIssue()
        .setComponentUuid(file2.uuid())
      .setType(3)
      .setLine(600)
      .setGap(1.12d)
      .setEffort(50L)
      .setIssueUpdateTime(1_600_000_000_000L)
      .setUpdatedAt(1_600_000_000_000L);

    // selected before last update -> ko
    dto.setSelectedAt(1400000000000L);

    int count = underTest.updateIfBeforeSelectedDate(dto);
    assertThat(count).isEqualTo(0);
    dbTester.getSession().commit();

    // No change
    IssueDto result = underTest.selectByKey("ABCDE");
    assertThat(result).isNotNull();
    assertThat(result.getComponentUuid()).isEqualTo(file.uuid());
    assertThat(result.getType()).isEqualTo(2);
    assertThat(result.getLine()).isEqualTo(500);
    assertThat(result.getGap()).isEqualTo(3.14d);
    assertThat(result.getEffort()).isEqualTo(10L);
    assertThat(result.getIssueUpdateTime()).isEqualTo(1_402_000_000_000L);
    assertThat(result.getUpdatedAt()).isEqualTo(1_500_000_000_000L);
  }

  private IssueDto newIssue() {
    return new IssueDto()
      .setKee("ABCDE")
      .setComponentUuid(file.uuid())
      .setProjectUuid(project.uuid())
      .setRuleId(rule.getId())
      .setType(2)
      .setLine(500)
      .setGap(3.14)
      .setEffort(10L)
      .setResolution("FIXED")
      .setStatus("RESOLVED")
      .setSeverity("BLOCKER")
      .setAuthorLogin("morgan")
      .setAssignee("karadoc")
      .setIssueAttributes("JIRA=FOO-1234")
      .setChecksum("123456789")
      .setMessage("the message")
      .setIssueCreationTime(1_401_000_000_000L)
      .setIssueUpdateTime(1_402_000_000_000L)
      .setIssueCloseTime(1_403_000_000_000L)
      .setCreatedAt(1_400_000_000_000L)
      .setUpdatedAt(1_500_000_000_000L);
  }
}
