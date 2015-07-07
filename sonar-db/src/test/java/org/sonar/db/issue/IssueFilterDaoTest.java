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

import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;

@Category(DbTests.class)
public class IssueFilterDaoTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  IssueFilterDao dao = dbTester.getDbClient().issueFilterDao();

  @Test
  public void should_select_by_id() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    IssueFilterDto filter = dao.selectById(1L);

    assertThat(filter.getId()).isEqualTo(1L);
    assertThat(filter.getName()).isEqualTo("Sonar Issues");
    assertThat(filter.isShared()).isTrue();

    assertThat(dao.selectById(123L)).isNull();
  }

  @Test
  public void should_select_by_user() {
    dbTester.prepareDbUnit(getClass(), "should_select_by_user.xml");

    List<IssueFilterDto> results = dao.selectByUser("michael");

    assertThat(results).hasSize(2);
  }

  @Test
  public void should_select_by_user_with_only_favorite_filters() {
    dbTester.prepareDbUnit(getClass(), "should_select_by_user_with_only_favorite_filters.xml");

    List<IssueFilterDto> results = dao.selectFavoriteFiltersByUser("michael");

    assertThat(results).hasSize(1);
    IssueFilterDto issueFilterDto = results.get(0);
    assertThat(issueFilterDto.getId()).isEqualTo(2L);
  }

  @Test
  public void should_select_shared() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    assertThat(dao.selectSharedFilters()).hasSize(1);
  }

  @Test
  public void should_select_provided_by_name() {
    dbTester.prepareDbUnit(getClass(), "should_select_provided_by_name.xml");

    assertThat(dao.selectProvidedFilterByName("Unresolved Issues").getName()).isEqualTo("Unresolved Issues");
    assertThat(dao.selectProvidedFilterByName("My Unresolved Issues").getName()).isEqualTo("My Unresolved Issues");
    assertThat(dao.selectProvidedFilterByName("Unknown Filter")).isNull();
  }

  @Test
  public void should_insert() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    IssueFilterDto filterDto = new IssueFilterDto();
    filterDto.setName("Sonar Open issues");
    filterDto.setUserLogin("michael");
    filterDto.setShared(true);
    filterDto.setDescription("All open issues on Sonar");
    filterDto.setData("statuses=OPEN|componentRoots=org.codehaus.sonar");

    dao.insert(filterDto);

    dbTester.assertDbUnit(getClass(), "should_insert-result.xml", new String[]{"created_at", "updated_at"}, "issue_filters");
  }

  @Test
  public void should_update() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    IssueFilterDto filterDto = new IssueFilterDto();
    filterDto.setId(2L);
    filterDto.setName("Closed issues");
    filterDto.setShared(false);
    filterDto.setDescription("All closed issues");
    filterDto.setData("statuses=CLOSED");
    filterDto.setUserLogin("bernard");

    dao.update(filterDto);

    dbTester.assertDbUnit(getClass(), "should_update-result.xml", new String[]{"created_at", "updated_at"}, "issue_filters");
  }

  @Test
  public void should_delete() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    dao.delete(1l);

    dbTester.assertDbUnit(getClass(), "should_delete-result.xml", new String[] {"created_at", "updated_at"}, "issue_filters");
  }
}
