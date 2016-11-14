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
package org.sonar.db.qualityprofile;

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class QProfileChangeDaoTest {

  private static final long A_DATE = 1_500_000_000_000L;

  private System2 system2 = mock(System2.class);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public DbTester dbTester = DbTester.create(system2);

  private DbSession dbSession = dbTester.getSession();
  private UuidFactory uuidFactory = mock(UuidFactory.class);
  private QProfileChangeDao underTest = new QProfileChangeDao(system2, uuidFactory);

  @Test
  public void test_insert_without_null_fields() {
    when(system2.now()).thenReturn(A_DATE);
    when(uuidFactory.create()).thenReturn("C1");

    String profileKey = "P1";
    String login = "marcel";
    String type = "ACTIVATED";
    String data = "some_data";
    insertChange(profileKey, type, login, data);

    Map<String, Object> row = selectChangeByKey("C1");
    assertThat(row.get("qprofileKey")).isEqualTo(profileKey);
    assertThat(row.get("createdAt")).isEqualTo(A_DATE);
    assertThat(row.get("login")).isEqualTo(login);
    assertThat(row.get("changeType")).isEqualTo(type);
    assertThat(row.get("changeData")).isEqualTo(data);
  }

  /**
   * user_login and data can be null
   */
  @Test
  public void test_insert_with_nullable_fields() {
    when(system2.now()).thenReturn(A_DATE);
    when(uuidFactory.create()).thenReturn("C1");

    insertChange("P1", "ACTIVATED", null, null);

    Map<String, Object> row = selectChangeByKey("C1");
    assertThat(row.get("qprofileKey")).isEqualTo("P1");
    assertThat(row.get("createdAt")).isEqualTo(A_DATE);
    assertThat(row.get("changeType")).isEqualTo("ACTIVATED");
    assertThat(row.get("login")).isNull();
    assertThat(row.get("changeData")).isNull();
  }

  @Test
  public void insert_throws_ISE_if_key_is_already_set() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Key of QProfileChangeDto must be set by DAO only. Got C1.");

    underTest.insert(dbSession, new QProfileChangeDto().setKey("C1"));
  }

  @Test
  public void insert_throws_ISE_if_date_is_already_set() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Date of QProfileChangeDto must be set by DAO only. Got 123.");

    underTest.insert(dbSession, new QProfileChangeDto().setCreatedAt(123L));
  }

  @Test
  public void selectByQuery_returns_empty_list_if_no_profile_changes() {
    List<QProfileChangeDto> changes = underTest.selectByQuery(dbSession, new QProfileChangeQuery("P1"));
    assertThat(changes).isEmpty();
  }

  @Test
  public void selectByQuery_returns_changes_ordered_by_descending_date() {
    when(system2.now()).thenReturn(A_DATE, A_DATE + 1, A_DATE + 2);
    when(uuidFactory.create()).thenReturn("C1", "C2", "C3");

    // profile P1
    insertChange("P1", "ACTIVATED", null, null);// key: C1
    insertChange("P1", "ACTIVATED", null, null);// key: C2
    // profile P2: C3
    insertChange("P2", "ACTIVATED", null, null);// key: C3

    List<QProfileChangeDto> changes = underTest.selectByQuery(dbSession, new QProfileChangeQuery("P1"));
    assertThat(changes).extracting(QProfileChangeDto::getKey).containsExactly("C2", "C1");
  }

  @Test
  public void selectByQuery_supports_pagination_of_changes() {
    when(system2.now()).thenReturn(A_DATE, A_DATE + 10, A_DATE + 20, A_DATE + 30);
    when(uuidFactory.create()).thenReturn("C1", "C2", "C3", "C4");
    insertChange("P1", "ACTIVATED", null, null);// key: C1
    insertChange("P1", "ACTIVATED", null, null);// key: C2
    insertChange("P1", "ACTIVATED", null, null);// key: C3
    insertChange("P1", "ACTIVATED", null, null);// key: C4

    QProfileChangeQuery query = new QProfileChangeQuery("P1");
    query.setOffset(2);
    query.setLimit(1);
    List<QProfileChangeDto> changes = underTest.selectByQuery(dbSession, query);
    assertThat(changes).extracting(QProfileChangeDto::getKey).containsExactly("C2");
  }

  @Test
  public void selectByQuery_returns_changes_after_given_date() {
    when(system2.now()).thenReturn(A_DATE, A_DATE + 10, A_DATE + 20);
    when(uuidFactory.create()).thenReturn("C1", "C2", "C3", "C4");
    insertChange("P1", "ACTIVATED", null, null);// key: C1
    insertChange("P1", "ACTIVATED", null, null);// key: C2
    insertChange("P1", "ACTIVATED", null, null);// key: C3

    QProfileChangeQuery query = new QProfileChangeQuery("P1");
    query.setFromIncluded(A_DATE + 10);
    List<QProfileChangeDto> changes = underTest.selectByQuery(dbSession, query);
    assertThat(changes).extracting(QProfileChangeDto::getKey).containsExactly("C3", "C2");
  }

  @Test
  public void selectByQuery_returns_changes_before_given_date() {
    when(system2.now()).thenReturn(A_DATE, A_DATE + 10, A_DATE + 20);
    when(uuidFactory.create()).thenReturn("C1", "C2", "C3", "C4");
    insertChange("P1", "ACTIVATED", null, null);// key: C1
    insertChange("P1", "ACTIVATED", null, null);// key: C2
    insertChange("P1", "ACTIVATED", null, null);// key: C3

    QProfileChangeQuery query = new QProfileChangeQuery("P1");
    query.setToExcluded(A_DATE + 12);
    List<QProfileChangeDto> changes = underTest.selectByQuery(dbSession, query);
    assertThat(changes).extracting(QProfileChangeDto::getKey).containsExactly("C2", "C1");
  }

  @Test
  public void selectByQuery_returns_changes_in_a_range_of_dates() {
    when(system2.now()).thenReturn(A_DATE, A_DATE + 10, A_DATE + 20, A_DATE + 30);
    when(uuidFactory.create()).thenReturn("C1", "C2", "C3", "C4");
    insertChange("P1", "ACTIVATED", null, null);// key: C1
    insertChange("P1", "ACTIVATED", null, null);// key: C2
    insertChange("P1", "ACTIVATED", null, null);// key: C3
    insertChange("P1", "ACTIVATED", null, null);// key: C4

    QProfileChangeQuery query = new QProfileChangeQuery("P1");
    query.setFromIncluded(A_DATE + 8);
    query.setToExcluded(A_DATE + 22);
    List<QProfileChangeDto> changes = underTest.selectByQuery(dbSession, query);
    assertThat(changes).extracting(QProfileChangeDto::getKey).containsExactly("C3", "C2");
  }

  @Test
  public void selectByQuery_mapping() {
    when(system2.now()).thenReturn(A_DATE);
    when(uuidFactory.create()).thenReturn("C1");
    insertChange("P1", "ACTIVATED", "Oscar", "data");

    List<QProfileChangeDto> result = underTest.selectByQuery(dbSession, new QProfileChangeQuery("P1"));

    assertThat(result).hasSize(1);
    QProfileChangeDto change = result.get(0);
    assertThat(change.getProfileKey()).isEqualTo("P1");
    assertThat(change.getLogin()).isEqualTo("Oscar");
    assertThat(change.getData()).isEqualTo("data");
    assertThat(change.getChangeType()).isEqualTo("ACTIVATED");
    assertThat(change.getKey()).isEqualTo("C1");
    assertThat(change.getCreatedAt()).isEqualTo(A_DATE);
  }

  @Test
  public void test_countForProfileKey() {
    when(system2.now()).thenReturn(A_DATE, A_DATE + 10);
    when(uuidFactory.create()).thenReturn("C1", "C2");

    insertChange("P1", "ACTIVATED", null, null);// key: C1
    insertChange("P1", "ACTIVATED", null, null);// key: C2

    assertThat(underTest.countForProfileKey(dbSession, "P1")).isEqualTo(2);
    assertThat(underTest.countForProfileKey(dbSession, "P2")).isEqualTo(0);
  }

  private void insertChange(String profileKey, String type, @Nullable String login, @Nullable String data) {
    QProfileChangeDto dto = new QProfileChangeDto()
      .setProfileKey(profileKey)
      .setLogin(login)
      .setChangeType(type)
      .setData(data);
    underTest.insert(dbSession, dto);
  }

  private Map<String, Object> selectChangeByKey(String key) {
    return dbTester.selectFirst(dbSession,
      "select qprofile_key as \"qprofileKey\", created_at as \"createdAt\", user_login as \"login\", change_type as \"changeType\", change_data as \"changeData\" from qprofile_changes where kee='"
        + key + "'");
  }
}
