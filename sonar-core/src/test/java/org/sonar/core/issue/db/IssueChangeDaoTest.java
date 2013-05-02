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

import org.junit.Before;
import org.junit.Test;
import org.sonar.core.persistence.AbstractDaoTestCase;

import java.util.Date;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class IssueChangeDaoTest extends AbstractDaoTestCase {

  private IssueChangeDao dao;

  @Before
  public void createDao() {
    dao = new IssueChangeDao(getMyBatis());
  }

  @Test
  public void should_select_by_id() {
    setupData("shared");

    IssueChangeDto dto = dao.selectById(100L);
    assertThat(dto.getId()).isEqualTo(100L);
    assertThat(dto.getIssueKey()).isEqualTo("1000");
    assertThat(dto.getUserLogin()).isEqualTo("arthur");
    assertThat(dto.getChangeType()).isEqualTo("comment");
    assertThat(dto.getChangeData()).isEqualTo("this is a comment");
    assertThat(dto.getCreatedAt()).isNotNull();
    assertThat(dto.getUpdatedAt()).isNotNull();
  }

  @Test
  public void should_select_by_issue() {
    setupData("shared");

    List<IssueChangeDto> ordered = dao.selectByIssue("1000");
    assertThat(ordered).hasSize(2);
    assertThat(ordered.get(0).getId()).isEqualTo(101);
    assertThat(ordered.get(1).getId()).isEqualTo(100);
  }

}
