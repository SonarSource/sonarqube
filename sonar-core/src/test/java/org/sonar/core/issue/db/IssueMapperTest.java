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

import org.apache.ibatis.session.SqlSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.persistence.MyBatis;

import static org.assertj.core.api.Assertions.assertThat;

public class IssueMapperTest extends AbstractDaoTestCase {

  SqlSession session;
  IssueMapper mapper;

  @Before
  public void setUp() {
    session = getMyBatis().openSession();
    mapper = session.getMapper(IssueMapper.class);
  }

  @After
  public void tearDown() {
    MyBatis.closeQuietly(session);
  }

  @Test
  public void insert() throws Exception {
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
    session.commit();

    checkTables("testInsert", new String[] {"id"}, "issues");
  }

  @Test
  public void update() throws Exception {
    setupData("testUpdate");

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
    session.commit();

    checkTables("testUpdate", new String[] {"id"}, "issues");
  }

  @Test
  public void updateBeforeSelectedDate_without_conflict() throws Exception {
    setupData("testUpdate");

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
    session.commit();

    checkTables("testUpdate", new String[] {"id"}, "issues");
  }

  @Test
  public void updateBeforeSelectedDate_with_conflict() throws Exception {
    setupData("updateBeforeSelectedDate_with_conflict");

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
    session.commit();

    checkTables("updateBeforeSelectedDate_with_conflict", new String[] {"id"}, "issues");
  }
}
