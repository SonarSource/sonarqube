/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.core.issue;

import org.junit.Before;
import org.junit.Test;
import org.sonar.core.persistence.AbstractDaoTestCase;

import java.util.Collection;
import java.util.Date;

import static org.fest.assertions.Assertions.assertThat;

public class IssueChangelogDaoTest extends AbstractDaoTestCase {

  private IssueChangelogDao dao;

  @Before
  public void createDao() {
    dao = new IssueChangelogDao(getMyBatis());
  }

  @Test
  public void should_insert() {
    setupData("insert");

    IssueChangeLogDto dto = new IssueChangeLogDto();
    dto.setIssueUuid("100");
    dto.setUserId(100L);
    dto.setChangeType("type");
    dto.setChangeData("data");
    dto.setMessage("message");

    Date today = new Date();
    dto.setCreatedAt(today);
    dto.setUpdatedAt(today);

    dao.insert(dto);

    checkTables("insert", new String[]{"id", "created_at", "updated_at"}, "issue_changelog");
  }

  @Test
  public void should_find_by_id() {
    setupData("shared");

    IssueChangeLogDto dto = dao.findById(100L);
    assertThat(dto.getId()).isEqualTo(100L);
    assertThat(dto.getIssueUuid()).isEqualTo("100");
    assertThat(dto.getUserId()).isEqualTo(100L);
    assertThat(dto.getChangeType()).isEqualTo("type");
    assertThat(dto.getChangeData()).isEqualTo("data");
    assertThat(dto.getMessage()).isEqualTo("message");
    assertThat(dto.getCreatedAt()).isNull();
    assertThat(dto.getUpdatedAt()).isNull();
  }

  @Test
  public void should_select_by_issue() {
    setupData("shared");

    Collection<IssueChangeLogDto> dtoList = dao.selectByIssue("100");
    assertThat(dtoList).hasSize(2);
  }

}
