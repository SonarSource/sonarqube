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

public class IssueFilterDaoTest extends AbstractDaoTestCase {

  IssueFilterDao dao;

  @Before
  public void createDao() {
    dao = new IssueFilterDao(getMyBatis());
  }

  @Test
  public void should_select_by_id() {
    setupData("shared");

    IssueFilterDto filter = dao.selectById(1L);

    assertThat(filter.getId()).isEqualTo(1L);
    assertThat(filter.getName()).isEqualTo("Sonar Issues");
    assertThat(filter.isShared()).isTrue();

    assertThat(dao.selectById(123L)).isNull();
  }

  @Test
  public void should_select_by_user() {
    setupData("should_select_by_user");

    List<IssueFilterDto> results = dao.selectByUser("michael");

    assertThat(results).hasSize(2);
  }

  @Test
  public void should_select_by_user_with_only_favorite_filters() {
    setupData("should_select_by_user_with_only_favorite_filters");

    List<IssueFilterDto> results = dao.selectFavoriteFiltersByUser("michael");

    assertThat(results).hasSize(1);
    IssueFilterDto issueFilterDto = results.get(0);
    assertThat(issueFilterDto.getId()).isEqualTo(2L);
  }

  @Test
  public void should_select_shared() {
    setupData("shared");

    assertThat(dao.selectSharedFilters()).hasSize(1);
  }

  @Test
  public void should_select_provided_by_name() {
    setupData("should_select_provided_by_name");

    assertThat(dao.selectProvidedFilterByName("Unresolved Issues").getName()).isEqualTo("Unresolved Issues");
    assertThat(dao.selectProvidedFilterByName("My Unresolved Issues").getName()).isEqualTo("My Unresolved Issues");
    assertThat(dao.selectProvidedFilterByName("Unknown Filter")).isNull();
  }

  @Test
  public void should_insert() {
    setupData("shared");

    IssueFilterDto filterDto = new IssueFilterDto();
    filterDto.setName("Sonar Open issues");
    filterDto.setUserLogin("michael");
    filterDto.setShared(true);
    filterDto.setDescription("All open issues on Sonar");
    filterDto.setData("statuses=OPEN|componentRoots=org.codehaus.sonar");

    dao.insert(filterDto);

    checkTables("should_insert", new String[]{"created_at", "updated_at"}, "issue_filters");
  }

  @Test
  public void should_update() {
    setupData("shared");

    IssueFilterDto filterDto = new IssueFilterDto();
    filterDto.setId(2L);
    filterDto.setName("Closed issues");
    filterDto.setShared(false);
    filterDto.setDescription("All closed issues");
    filterDto.setData("statuses=CLOSED");
    filterDto.setUserLogin("bernard");

    dao.update(filterDto);

    checkTables("should_update", new String[]{"created_at", "updated_at"}, "issue_filters");
  }

  @Test
  public void should_delete() {
    setupData("shared");

    dao.delete(1l);

    checkTables("should_delete", new String[]{"created_at", "updated_at"}, "issue_filters");
  }
}
