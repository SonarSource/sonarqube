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

package org.sonar.db.user;

import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.user.GroupTesting.newGroupDto;

@Category(DbTests.class)
public class GroupDaoTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  DbSession dbSession = db.getSession();
  DbClient dbClient = db.getDbClient();
  System2 system2 = mock(System2.class);

  GroupDao underTest = new GroupDao(system2);

  @Test
  public void select_by_key() {
    db.prepareDbUnit(getClass(), "select_by_key.xml");

    GroupDto group = underTest.selectOrFailByName(dbSession, "sonar-users");

    assertThat(group).isNotNull();
    assertThat(group.getId()).isEqualTo(1L);
    assertThat(group.getName()).isEqualTo("sonar-users");
    assertThat(group.getDescription()).isEqualTo("Sonar Users");
    assertThat(group.getCreatedAt()).isEqualTo(DateUtils.parseDate("2014-09-07"));
    assertThat(group.getUpdatedAt()).isEqualTo(DateUtils.parseDate("2014-09-08"));
  }

  @Test
  public void select_by_id() {
    db.prepareDbUnit(getClass(), "select_by_key.xml");

    GroupDto group = underTest.selectOrFailById(dbSession, 1L);

    assertThat(group).isNotNull();
    assertThat(group.getId()).isEqualTo(1L);
    assertThat(group.getName()).isEqualTo("sonar-users");
    assertThat(group.getDescription()).isEqualTo("Sonar Users");
    assertThat(group.getCreatedAt()).isEqualTo(DateUtils.parseDate("2014-09-07"));
    assertThat(group.getUpdatedAt()).isEqualTo(DateUtils.parseDate("2014-09-08"));
  }

  @Test
  public void find_by_user_login() {
    db.prepareDbUnit(getClass(), "find_by_user_login.xml");

    assertThat(underTest.selectByUserLogin(dbSession, "john")).hasSize(2);
    assertThat(underTest.selectByUserLogin(dbSession, "max")).isEmpty();
  }

  @Test
  public void insert() {
    when(system2.now()).thenReturn(DateUtils.parseDate("2014-09-08").getTime());
    db.prepareDbUnit(getClass(), "empty.xml");
    GroupDto dto = new GroupDto()
      .setId(1L)
      .setName("sonar-users")
      .setDescription("Sonar Users");

    underTest.insert(dbSession, dto);
    dbSession.commit();

    db.assertDbUnit(getClass(), "insert-result.xml", "groups");
  }

  @Test
  public void update() {
    when(system2.now()).thenReturn(DateUtils.parseDate("2013-07-25").getTime());
    db.prepareDbUnit(getClass(), "update.xml");
    GroupDto dto = new GroupDto()
      .setId(1L)
      .setName("new-name")
      .setDescription("New Description");

    underTest.update(dbSession, dto);
    dbSession.commit();

    db.assertDbUnit(getClass(), "update-result.xml", "groups");
  }

  @Test
  public void select_by_query() {
    db.prepareDbUnit(getClass(), "select_by_query.xml");

    /*
     * Ordering and paging are not fully tested, case insensitive sort is broken on MySQL
     */

    // Null query
    assertThat(underTest.selectByQuery(dbSession, null, 0, 10))
      .hasSize(5)
      .extracting("name").containsOnly("customers-group1", "customers-group2", "customers-group3", "SONAR-ADMINS", "sonar-users");

    // Empty query
    assertThat(underTest.selectByQuery(dbSession, "", 0, 10))
      .hasSize(5)
      .extracting("name").containsOnly("customers-group1", "customers-group2", "customers-group3", "SONAR-ADMINS", "sonar-users");

    // Filter on name
    assertThat(underTest.selectByQuery(dbSession, "sonar", 0, 10))
      .hasSize(2)
      .extracting("name").containsOnly("SONAR-ADMINS", "sonar-users");

    // Pagination
    assertThat(underTest.selectByQuery(dbSession, null, 0, 3))
      .hasSize(3);
    assertThat(underTest.selectByQuery(dbSession, null, 3, 3))
      .hasSize(2);
    assertThat(underTest.selectByQuery(dbSession, null, 6, 3)).isEmpty();
    assertThat(underTest.selectByQuery(dbSession, null, 0, 5))
      .hasSize(5);
    assertThat(underTest.selectByQuery(dbSession, null, 5, 5)).isEmpty();
  }

  @Test
  public void select_by_query_with_special_characters() {
    String groupNameWithSpecialCharacters = "group%_%/name";
    underTest.insert(dbSession, newGroupDto().setName(groupNameWithSpecialCharacters));
    db.commit();

    List<GroupDto> result = underTest.selectByQuery(dbSession, "roup%_%/nam", 0, 10);
    int resultCount = underTest.countByQuery(dbSession, "roup%_%/nam");

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo(groupNameWithSpecialCharacters);
    assertThat(resultCount).isEqualTo(1);
  }

  @Test
  public void count_by_query() {
    db.prepareDbUnit(getClass(), "select_by_query.xml");

    // Null query
    assertThat(underTest.countByQuery(dbSession, null)).isEqualTo(5);

    // Empty query
    assertThat(underTest.countByQuery(dbSession, "")).isEqualTo(5);

    // Filter on name
    assertThat(underTest.countByQuery(dbSession, "sonar")).isEqualTo(2);
  }

  @Test
  public void delete_by_id() {
    db.prepareDbUnit(getClass(), "select_by_key.xml");

    GroupDao groupDao = underTest;
    groupDao.deleteById(dbSession, 1L);
    dbSession.commit();

    assertThat(groupDao.countByQuery(dbSession, null)).isZero();
  }

}
