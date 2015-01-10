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

import org.junit.Before;
import org.junit.Test;
import org.sonar.core.persistence.AbstractDaoTestCase;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class IssueFilterFavouriteDaoTest extends AbstractDaoTestCase {

  IssueFilterFavouriteDao dao;

  @Before
  public void createDao() {
    dao = new IssueFilterFavouriteDao(getMyBatis());
  }

  @Test
  public void should_select_by_id() {
    setupData("shared");

    IssueFilterFavouriteDto dto = dao.selectById(1L);
    assertThat(dto.getId()).isEqualTo(1L);
    assertThat(dto.getUserLogin()).isEqualTo("stephane");
    assertThat(dto.getIssueFilterId()).isEqualTo(10L);
    assertThat(dto.getCreatedAt()).isNotNull();

    assertThat(dao.selectById(999L)).isNull();
  }

  @Test
  public void should_select_by_filter_id() {
    setupData("shared");

    List<IssueFilterFavouriteDto> dtos = dao.selectByFilterId(11L);
    assertThat(dtos).hasSize(1);
    IssueFilterFavouriteDto dto = dtos.get(0);
    assertThat(dto.getId()).isEqualTo(2L);
    assertThat(dto.getUserLogin()).isEqualTo("stephane");
    assertThat(dto.getIssueFilterId()).isEqualTo(11L);
    assertThat(dto.getCreatedAt()).isNotNull();

    assertThat(dao.selectByFilterId(10L)).hasSize(2);
    assertThat(dao.selectByFilterId(999L)).isEmpty();
  }

  @Test
  public void should_insert() {
    setupData("shared");

    IssueFilterFavouriteDto dto = new IssueFilterFavouriteDto();
    dto.setUserLogin("arthur");
    dto.setIssueFilterId(11L);

    dao.insert(dto);

    checkTables("should_insert", new String[]{"created_at"}, "issue_filter_favourites");
  }

  @Test
  public void should_delete() {
    setupData("shared");

    dao.delete(3l);

    checkTables("should_delete", new String[]{"created_at"}, "issue_filter_favourites");
  }

  @Test
  public void should_delete_by_issue_filter_id() {
    setupData("shared");

    dao.deleteByFilterId(10l);

    checkTables("should_delete_by_issue_filter_id", new String[]{"created_at"}, "issue_filter_favourites");
  }

}
