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

import org.apache.ibatis.session.SqlSession;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.persistence.AbstractDaoTestCase;

public class IssueMapperTest extends AbstractDaoTestCase {

  SqlSession session;
  IssueMapper mapper;

  @Before
  public void setUp() {
    session = getMyBatis().openSession();
    mapper = session.getMapper(IssueMapper.class);
  }

  @Test
  public void testInsert() throws Exception {
    IssueDto dto = new IssueDto();
    dto.setComponentId(123l);
    dto.setRootComponentId(100l);
    dto.setRuleId(200);
    dto.setKee("ABCDE");
    dto.setLine(500);
    dto.setEffortToFix(3.14);
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
    dto.setCreatedAt(DateUtils.parseDate("2013-05-21"));
    dto.setUpdatedAt(DateUtils.parseDate("2013-05-22"));

    mapper.insert(dto);
    session.commit();

    checkTables("testInsert", new String[]{"id"}, "issues");
  }

  @Test
  public void testUpdate() throws Exception {
    setupData("testUpdate");

    IssueDto dto = new IssueDto();
    dto.setComponentId(123l);
    dto.setRootComponentId(100l);
    dto.setRuleId(200);
    dto.setKee("ABCDE");
    dto.setLine(500);
    dto.setEffortToFix(3.14);
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
    dto.setCreatedAt(DateUtils.parseDate("2013-05-21"));
    dto.setUpdatedAt(DateUtils.parseDate("2013-05-22"));

    mapper.update(dto);
    session.commit();

    checkTables("testUpdate", new String[]{"id"}, "issues");
  }
}
