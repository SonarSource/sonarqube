/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.db.user;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.user.GroupTesting.newGroupDto;

public class GroupDaoTest {

  private static final long NOW = 1_500_000L;
  private static final String MISSING_UUID = "unknown";

  private System2 system2 = mock(System2.class);

  @Rule
  public DbTester db = DbTester.create(system2);
  private DbSession dbSession = db.getSession();
  private GroupDao underTest = db.getDbClient().groupDao();

  // not static as group uuid is changed in each test
  private final GroupDto aGroup = new GroupDto()
    .setUuid("uuid")
    .setName("the-name")
    .setDescription("the description");

  @Before
  public void setUp() {
    when(system2.now()).thenReturn(NOW);
  }

  @Test
  public void selectByName() {
    db.getDbClient().groupDao().insert(dbSession, aGroup);

    GroupDto group = underTest.selectByName(dbSession, null, aGroup.getName()).get();

    assertThat(group.getUuid()).isNotNull();
    assertThat(group.getName()).isEqualTo(aGroup.getName());
    assertThat(group.getDescription()).isEqualTo(aGroup.getDescription());
    assertThat(group.getCreatedAt()).isEqualTo(new Date(NOW));
    assertThat(group.getUpdatedAt()).isEqualTo(new Date(NOW));
  }

  @Test
  public void selectByName_returns_absent() {
    Optional<GroupDto> group = underTest.selectByName(dbSession, null, "missing");

    assertThat(group).isNotPresent();
  }

  @Test
  public void selectByUserLogin() {
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    GroupDto group3 = db.users().insertGroup();
    UserDto user = db.users().insertUser();
    db.users().insertMember(group1, user);
    db.users().insertMember(group2, user);

    assertThat(underTest.selectByUserLogin(dbSession, user.getLogin())).hasSize(2);
    assertThat(underTest.selectByUserLogin(dbSession, "unknown")).isEmpty();
  }

  @Test
  public void selectByNames() {
    GroupDto group1 = underTest.insert(dbSession, newGroupDto().setName("group1"));
    GroupDto group2 = underTest.insert(dbSession, newGroupDto().setName("group2"));

    dbSession.commit();

    assertThat(underTest.selectByNames(dbSession, null, asList("group1", "group2", "group3", "missingGroup"))).extracting(GroupDto::getUuid)
      .containsOnly(group1.getUuid(), group2.getUuid());
    assertThat(underTest.selectByNames(dbSession, null, Collections.emptyList())).isEmpty();
  }

  @Test
  public void selectByUuids() {
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    GroupDto group3 = db.users().insertGroup();

    assertThat(underTest.selectByUuids(dbSession, asList(group1.getUuid(), group2.getUuid())))
      .extracting(GroupDto::getUuid).containsOnly(group1.getUuid(), group2.getUuid());

    assertThat(underTest.selectByUuids(dbSession, asList(group1.getUuid(), MISSING_UUID)))
      .extracting(GroupDto::getUuid).containsOnly(group1.getUuid());

    assertThat(underTest.selectByUuids(dbSession, Collections.emptyList())).isEmpty();
  }

  @Test
  public void update() {
    db.getDbClient().groupDao().insert(dbSession, aGroup);
    GroupDto dto = new GroupDto()
      .setUuid(aGroup.getUuid())
      .setName("new-name")
      .setDescription("New description")
      .setCreatedAt(new Date(NOW + 1_000L));

    underTest.update(dbSession, dto);

    GroupDto reloaded = underTest.selectByUuid(dbSession, aGroup.getUuid());

    // verify mutable fields
    assertThat(reloaded.getName()).isEqualTo("new-name");
    assertThat(reloaded.getDescription()).isEqualTo("New description");

    // immutable fields --> to be ignored
    assertThat(reloaded.getCreatedAt()).isEqualTo(aGroup.getCreatedAt());
  }

  @Test
  public void selectByQuery() {
    db.users().insertGroup("sonar-users");
    db.users().insertGroup("SONAR-ADMINS");
    db.users().insertGroup("customers-group1");
    db.users().insertGroup("customers-group2");
    db.users().insertGroup("customers-group3");

    /*
     * Ordering and paging are not fully tested, case insensitive sort is broken on MySQL
     */

    // Null query
    assertThat(underTest.selectByQuery(dbSession, null, null, 0, 10))
      .hasSize(5)
      .extracting("name").containsOnly("customers-group1", "customers-group2", "customers-group3", "SONAR-ADMINS", "sonar-users");

    // Empty query
    assertThat(underTest.selectByQuery(dbSession, null, "", 0, 10))
      .hasSize(5)
      .extracting("name").containsOnly("customers-group1", "customers-group2", "customers-group3", "SONAR-ADMINS", "sonar-users");

    // Filter on name
    assertThat(underTest.selectByQuery(dbSession, null, "sonar", 0, 10))
      .hasSize(2)
      .extracting("name").containsOnly("SONAR-ADMINS", "sonar-users");

    // Pagination
    assertThat(underTest.selectByQuery(dbSession, null, null, 0, 3))
      .hasSize(3);
    assertThat(underTest.selectByQuery(dbSession, null, null, 3, 3))
      .hasSize(2);
    assertThat(underTest.selectByQuery(dbSession, null, null, 6, 3)).isEmpty();
    assertThat(underTest.selectByQuery(dbSession, null, null, 0, 5))
      .hasSize(5);
    assertThat(underTest.selectByQuery(dbSession, null, null, 5, 5)).isEmpty();
  }

  @Test
  public void select_by_query_with_special_characters() {
    String groupNameWithSpecialCharacters = "group%_%/name";
    underTest.insert(dbSession, newGroupDto().setName(groupNameWithSpecialCharacters));
    db.commit();

    List<GroupDto> result = underTest.selectByQuery(dbSession, null, "roup%_%/nam", 0, 10);
    int resultCount = underTest.countByQuery(dbSession, null, "roup%_%/nam");

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo(groupNameWithSpecialCharacters);
    assertThat(resultCount).isOne();
  }

  @Test
  public void countByQuery() {
    db.users().insertGroup("sonar-users");
    db.users().insertGroup("SONAR-ADMINS");
    db.users().insertGroup("customers-group1");
    db.users().insertGroup("customers-group2");
    db.users().insertGroup("customers-group3");

    // Null query
    assertThat(underTest.countByQuery(dbSession, null, null)).isEqualTo(5);

    // Empty query
    assertThat(underTest.countByQuery(dbSession, null, "")).isEqualTo(5);

    // Filter on name
    assertThat(underTest.countByQuery(dbSession, null, "sonar")).isEqualTo(2);
  }

  @Test
  public void deleteByUuid() {
    db.getDbClient().groupDao().insert(dbSession, aGroup);

    underTest.deleteByUuid(dbSession, aGroup.getUuid(), aGroup.getName());

    assertThat(db.countRowsOfTable(dbSession, "groups")).isZero();
  }
}
