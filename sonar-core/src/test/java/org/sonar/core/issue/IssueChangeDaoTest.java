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
import org.sonar.core.persistence.AbstractDaoTestCase;

import java.util.Collection;
import java.util.Date;

import static org.fest.assertions.Assertions.assertThat;

public class IssueChangeDaoTest extends AbstractDaoTestCase {

  private IssueChangeDao dao;

  @Before
  public void createDao() {
    dao = new IssueChangeDao(getMyBatis());
  }

  @Test
  public void should_insert() {
    setupData("insert");

    IssueChangeDto dto = new IssueChangeDto();
    dto.setIssueKey("100");
    dto.setUserLogin("arthur");
    dto.setChangeType("type");
    dto.setChangeData("data");
    dto.setMessage("some message");

    Date today = new Date();
    dto.setCreatedAt(today);
    dto.setUpdatedAt(today);

    dao.insert(dto);

    checkTables("insert", new String[]{"id", "created_at", "updated_at"}, "issue_changes");
  }

  @Test
  public void should_find_by_id() {
    setupData("shared");

    IssueChangeDto dto = dao.findById(100L);
    assertThat(dto.getId()).isEqualTo(100L);
    assertThat(dto.getIssueKey()).isEqualTo("100");
    assertThat(dto.getUserLogin()).isEqualTo("arthur");
    assertThat(dto.getChangeType()).isEqualTo("type");
    assertThat(dto.getChangeData()).isEqualTo("data");
    assertThat(dto.getMessage()).isEqualTo("some message");
    assertThat(dto.getCreatedAt()).isNull();
    assertThat(dto.getUpdatedAt()).isNull();
  }

  @Test
  public void should_select_by_issue() {
    setupData("shared");

    Collection<IssueChangeDto> dtoList = dao.selectByIssue("100");
    assertThat(dtoList).hasSize(2);
  }

}
