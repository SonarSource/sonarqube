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

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;

@Category(DbTests.class)
public class IssueMapperTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  IssueMapper mapper = dbTester.getSession().getMapper(IssueMapper.class);

  @Test
  public void insert() {
    IssueDto dto = new IssueDto();
    dto.setComponentUuid("uuid-123");
    dto.setProjectUuid("uuid-100");
    dto.setRuleId(200);
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

    dto.setIssueCreationTime(1_401_000_000_000L);
    dto.setIssueUpdateTime(1_402_000_000_000L);
    dto.setIssueCloseTime(1_403_000_000_000L);
    dto.setCreatedAt(1_400_000_000_000L);
    dto.setUpdatedAt(1_500_000_000_000L);

    mapper.insert(dto);
    dbTester.getSession().commit();

    dbTester.assertDbUnit(getClass(), "testInsert-result.xml", new String[]{"id"}, "issues");
  }

  @Test
  public void update() {
    dbTester.prepareDbUnit(getClass(), "testUpdate.xml");

    IssueDto dto = new IssueDto();
    dto.setComponentUuid("uuid-123");
    dto.setProjectUuid("uuid-101");
    dto.setRuleId(200);
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

    dto.setIssueCreationTime(1_401_000_000_000L);
    dto.setIssueUpdateTime(1_402_000_000_000L);
    dto.setIssueCloseTime(1_403_000_000_000L);
    dto.setCreatedAt(1_400_000_000_000L);
    dto.setUpdatedAt(1_500_000_000_000L);

    mapper.update(dto);
    dbTester.getSession().commit();

    dbTester.assertDbUnit(getClass(), "testUpdate-result.xml", new String[]{"id"}, "issues");
  }

  @Test
  public void updateBeforeSelectedDate_without_conflict() {
    dbTester.prepareDbUnit(getClass(), "testUpdate.xml");

    IssueDto dto = new IssueDto();
    dto.setComponentUuid("uuid-123");
    dto.setProjectUuid("uuid-101");
    dto.setRuleId(200);
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
    dto.setIssueCreationTime(1_401_000_000_000L);
    dto.setIssueUpdateTime(1_402_000_000_000L);
    dto.setIssueCloseTime(1_403_000_000_000L);
    dto.setCreatedAt(1_400_000_000_000L);
    dto.setUpdatedAt(1_500_000_000_000L);

    // selected after last update -> ok
    dto.setSelectedAt(1500000000000L);

    int count = mapper.updateIfBeforeSelectedDate(dto);
    assertThat(count).isEqualTo(1);
    dbTester.getSession().commit();

    dbTester.assertDbUnit(getClass(), "testUpdate-result.xml", new String[]{"id"}, "issues");
  }

  @Test
  public void updateBeforeSelectedDate_with_conflict() {
    dbTester.prepareDbUnit(getClass(), "updateBeforeSelectedDate_with_conflict.xml");

    IssueDto dto = new IssueDto();
    dto.setComponentUuid("uuid-123");
    dto.setProjectUuid("uuid-101");
    dto.setRuleId(200);
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
    dto.setIssueCreationDate(DateUtils.parseDate("2013-05-18"));
    dto.setIssueUpdateDate(DateUtils.parseDate("2013-05-19"));
    dto.setIssueCloseDate(DateUtils.parseDate("2013-05-20"));
    dto.setCreatedAt(1400000000000L);
    dto.setUpdatedAt(1460000000000L);

    // selected before last update -> ko
    dto.setSelectedAt(1400000000000L);

    int count = mapper.updateIfBeforeSelectedDate(dto);
    assertThat(count).isEqualTo(0);
    dbTester.getSession().commit();

    dbTester.assertDbUnit(getClass(), "updateBeforeSelectedDate_with_conflict-result.xml", new String[]{"id"}, "issues");
  }
}
