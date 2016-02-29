/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;


public class IssueFilterFavouriteDaoTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  IssueFilterFavouriteDao dao = dbTester.getDbClient().issueFilterFavouriteDao();

  @Test
  public void should_select_by_id() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    IssueFilterFavouriteDto dto = dao.selectById(1L);
    assertThat(dto.getId()).isEqualTo(1L);
    assertThat(dto.getUserLogin()).isEqualTo("stephane");
    assertThat(dto.getIssueFilterId()).isEqualTo(10L);
    assertThat(dto.getCreatedAt()).isNotNull();

    assertThat(dao.selectById(999L)).isNull();
  }

  @Test
  public void should_select_by_filter_id() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

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
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    IssueFilterFavouriteDto dto = new IssueFilterFavouriteDto();
    dto.setUserLogin("arthur");
    dto.setIssueFilterId(11L);

    dao.insert(dto);

    dbTester.assertDbUnit(getClass(), "should_insert-result.xml", new String[]{"created_at"}, "issue_filter_favourites");
  }

  @Test
  public void should_delete() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    dao.delete(3l);

    dbTester.assertDbUnit(getClass(), "should_delete-result.xml", new String[]{"created_at"}, "issue_filter_favourites");
  }

  @Test
  public void should_delete_by_issue_filter_id() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    dao.deleteByFilterId(10l);

    dbTester.assertDbUnit(getClass(), "should_delete_by_issue_filter_id-result.xml", new String[]{"created_at"}, "issue_filter_favourites");
  }

}
