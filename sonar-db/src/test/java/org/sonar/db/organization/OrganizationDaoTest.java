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
package org.sonar.db.organization;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import org.apache.ibatis.exceptions.PersistenceException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OrganizationDaoTest {
  private static final long SOME_DATE = 1_200_999L;
  private static final long DATE_1 = 1_999_000L;
  private static final long DATE_2 = 8_999_999L;
  private static final long DATE_3 = 3_999_000L;
  private static final OrganizationDto ORGANIZATION_DTO = new OrganizationDto()
    .setUuid("a uuid")
    .setKey("the_key")
    .setName("the name")
    .setDescription("the description")
    .setUrl("the url")
    .setAvatarUrl("the avatar url");

  private System2 system2 = mock(System2.class);

  @Rule
  public final DbTester dbTester = DbTester.create(system2).setDisableDefaultOrganization(true);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DbClient dbClient = dbTester.getDbClient();
  private DbSession dbSession = dbTester.getSession();

  private OrganizationDao underTest = dbClient.organizationDao();

  @Test
  public void insert_fails_with_NPE_if_OrganizationDto_is_null() {
    expectDtoCanNotBeNull();

    underTest.insert(dbSession, null);
  }

  @Test
  public void insert_populates_createdAt_and_updateAt_with_same_date_from_System2() {
    when(system2.now()).thenReturn(DATE_1, DATE_1 + 1_000_000L);
    insertOrganization(copyOf(ORGANIZATION_DTO)
      .setCreatedAt(1_000L)
      .setUpdatedAt(6_000L));

    Map<String, Object> row = selectSingleRow();
    assertThat(row.get("createdAt")).isEqualTo(DATE_1);
    assertThat(row.get("updatedAt")).isEqualTo(DATE_1);
  }

  @Test
  public void insert_persists_properties_of_OrganizationDto() {
    insertOrganization(ORGANIZATION_DTO);

    Map<String, Object> row = selectSingleRow();
    assertThat(row.get("uuid")).isEqualTo(ORGANIZATION_DTO.getUuid());
    assertThat(row.get("key")).isEqualTo(ORGANIZATION_DTO.getKey());
    assertThat(row.get("name")).isEqualTo(ORGANIZATION_DTO.getName());
    assertThat(row.get("description")).isEqualTo(ORGANIZATION_DTO.getDescription());
    assertThat(row.get("url")).isEqualTo(ORGANIZATION_DTO.getUrl());
    assertThat(row.get("avatarUrl")).isEqualTo(ORGANIZATION_DTO.getAvatarUrl());
    assertThat(row.get("createdAt")).isEqualTo(ORGANIZATION_DTO.getCreatedAt());
    assertThat(row.get("updatedAt")).isEqualTo(ORGANIZATION_DTO.getUpdatedAt());
  }

  @Test
  public void description_url_and_avatarUrl_are_optional() {
    when(system2.now()).thenReturn(SOME_DATE);
    insertOrganization(copyOf(ORGANIZATION_DTO).setDescription(null).setUrl(null).setAvatarUrl(null));

    Map<String, Object> row = selectSingleRow();
    assertThat(row.get("uuid")).isEqualTo(ORGANIZATION_DTO.getUuid());
    assertThat(row.get("key")).isEqualTo(ORGANIZATION_DTO.getKey());
    assertThat(row.get("name")).isEqualTo(ORGANIZATION_DTO.getName());
    assertThat(row.get("description")).isNull();
    assertThat(row.get("url")).isNull();
    assertThat(row.get("avatarUrl")).isNull();
    assertThat(row.get("createdAt")).isEqualTo(SOME_DATE);
    assertThat(row.get("updatedAt")).isEqualTo(SOME_DATE);
  }

  @Test
  public void insert_fails_if_row_with_uuid_already_exists() {
    insertOrganization(ORGANIZATION_DTO);

    OrganizationDto dto = new OrganizationDto()
      .setUuid(ORGANIZATION_DTO.getUuid())
      .setKey("other key")
      .setName("other name")
      .setCreatedAt(2_999_000L)
      .setUpdatedAt(2_888_000L);

    expectedException.expect(PersistenceException.class);

    underTest.insert(dbSession, dto);
  }

  @Test
  public void selectByKey_returns_empty_when_table_is_empty() {
    assertThat(underTest.selectByKey(dbSession, ORGANIZATION_DTO.getKey())).isEmpty();
  }

  @Test
  public void selectByKey_returns_row_data_when_key_exists() {
    insertOrganization(ORGANIZATION_DTO);

    Optional<OrganizationDto> optional = underTest.selectByKey(dbSession, ORGANIZATION_DTO.getKey());
    verify(optional);
  }

  @Test
  public void selectByKey_returns_row_data_of_specified_key() {
    insertOrganization(ORGANIZATION_DTO);

    assertThat(underTest.selectByKey(dbSession, "foo key")).isEmpty();
  }

  @Test
  public void selectByKey_is_case_sensitive() {
    insertOrganization(ORGANIZATION_DTO);

    assertThat(underTest.selectByKey(dbSession, ORGANIZATION_DTO.getKey().toUpperCase(Locale.ENGLISH))).isEmpty();
  }

  @Test
  public void selectByUuid_returns_empty_when_table_is_empty() {
    assertThat(underTest.selectByUuid(dbSession, ORGANIZATION_DTO.getUuid())).isEmpty();
  }

  @Test
  public void selectByUuid_returns_row_data_when_uuid_exists() {
    insertOrganization(ORGANIZATION_DTO);

    Optional<OrganizationDto> optional = underTest.selectByUuid(dbSession, ORGANIZATION_DTO.getUuid());
    verify(optional);
  }

  @Test
  public void selectByUuid_returns_row_data_of_specified_uuid() {
    insertOrganization(ORGANIZATION_DTO);

    assertThat(underTest.selectByUuid(dbSession, "foo uuid")).isEmpty();
  }

  @Test
  public void selectByUuid_is_case_sensitive() {
    insertOrganization(ORGANIZATION_DTO);

    assertThat(underTest.selectByUuid(dbSession, ORGANIZATION_DTO.getUuid().toUpperCase(Locale.ENGLISH))).isEmpty();
  }

  @Test
  public void selectByQuery_returns_empty_when_table_is_empty() {
    assertThat(underTest.selectByQuery(dbSession, 1, 1)).isEmpty();
  }

  @Test
  public void selectByQuery_returns_single_row_of_table_when_requesting_first_page_of_size_1_or_more() {
    insertOrganization(ORGANIZATION_DTO);

    assertThat(underTest.selectByQuery(dbSession, 0, 1))
      .hasSize(1)
      .extracting("uuid")
      .containsOnly(ORGANIZATION_DTO.getUuid());

    assertThat(underTest.selectByQuery(dbSession, 0, 10))
      .hasSize(1)
      .extracting("uuid")
      .containsOnly(ORGANIZATION_DTO.getUuid());
  }

  @Test
  public void selectByQuery_returns_empty_on_table_with_single_row_when_not_requesting_the_first_page() {
    insertOrganization(ORGANIZATION_DTO);

    assertThat(underTest.selectByQuery(dbSession, 1, 1)).isEmpty();
    assertThat(underTest.selectByQuery(dbSession, Math.abs(new Random().nextInt(10)) + 1, 1)).isEmpty();
    assertThat(underTest.selectByQuery(dbSession, 1, 10)).isEmpty();
  }

  @Test
  public void selectByQuery_returns_rows_ordered_by_createdAt_descending_applying_requested_paging() {
    long time = 1_999_999L;
    when(system2.now()).thenReturn(time, time + 1_000, time + 2_000, time + 3_000, time + 5_000);
    insertOrganization(copyOf(ORGANIZATION_DTO).setUuid("uuid3").setKey("key-3"));
    insertOrganization(copyOf(ORGANIZATION_DTO).setUuid("uuid1").setKey("key-1"));
    insertOrganization(copyOf(ORGANIZATION_DTO).setUuid("uuid2").setKey("key-2"));
    insertOrganization(copyOf(ORGANIZATION_DTO).setUuid("uuid5").setKey("key-5"));
    insertOrganization(copyOf(ORGANIZATION_DTO).setUuid("uuid4").setKey("key-4"));

    assertThat(underTest.selectByQuery(dbSession, 0, 1))
      .extracting("uuid", "key")
      .containsExactly(tuple("uuid4", "key-4"));
    assertThat(underTest.selectByQuery(dbSession, 1, 1))
      .extracting("uuid", "key")
      .containsExactly(tuple("uuid5", "key-5"));
    assertThat(underTest.selectByQuery(dbSession, 2, 1))
      .extracting("uuid", "key")
      .containsExactly(tuple("uuid2", "key-2"));
    assertThat(underTest.selectByQuery(dbSession, 3, 1))
      .extracting("uuid", "key")
      .containsExactly(tuple("uuid1", "key-1"));
    assertThat(underTest.selectByQuery(dbSession, 4, 1))
      .extracting("uuid", "key")
      .containsExactly(tuple("uuid3", "key-3"));
    assertThat(underTest.selectByQuery(dbSession, 5, 1))
      .isEmpty();

    assertThat(underTest.selectByQuery(dbSession, 0, 5))
      .extracting("uuid")
      .containsExactly("uuid4", "uuid5", "uuid2", "uuid1", "uuid3");
    assertThat(underTest.selectByQuery(dbSession, 5, 5))
      .isEmpty();
    assertThat(underTest.selectByQuery(dbSession, 0, 3))
      .extracting("uuid")
      .containsExactly("uuid4", "uuid5", "uuid2");
    assertThat(underTest.selectByQuery(dbSession, 3, 3))
      .extracting("uuid")
      .containsExactly("uuid1", "uuid3");
  }

  @Test
  public void update_fails_with_NPE_if_OrganizationDto_is_null() {
    expectDtoCanNotBeNull();

    underTest.update(dbSession, null);
  }

  @Test
  public void update_does_not_fail_if_specified_row_does_not_exist() {
    underTest.update(dbSession, ORGANIZATION_DTO);
  }

  @Test
  public void update_with_same_information_succeeds_but_has_no_effect() {
    insertOrganization(ORGANIZATION_DTO);

    underTest.update(dbSession, ORGANIZATION_DTO);
    dbSession.commit();

    Map<String, Object> row = selectSingleRow();
    assertThat(row.get("uuid")).isEqualTo(ORGANIZATION_DTO.getUuid());
    assertThat(row.get("key")).isEqualTo(ORGANIZATION_DTO.getKey());
    assertThat(row.get("name")).isEqualTo(ORGANIZATION_DTO.getName());
    assertThat(row.get("description")).isEqualTo(ORGANIZATION_DTO.getDescription());
    assertThat(row.get("url")).isEqualTo(ORGANIZATION_DTO.getUrl());
    assertThat(row.get("avatarUrl")).isEqualTo(ORGANIZATION_DTO.getAvatarUrl());
    assertThat(row.get("createdAt")).isEqualTo(ORGANIZATION_DTO.getCreatedAt());
    assertThat(row.get("updatedAt")).isEqualTo(ORGANIZATION_DTO.getUpdatedAt());
  }

  @Test
  public void update_populates_updatedAt_with_date_from_System2() {
    when(system2.now()).thenReturn(DATE_1);
    insertOrganization(ORGANIZATION_DTO);

    when(system2.now()).thenReturn(DATE_2);
    underTest.update(dbSession, copyOf(ORGANIZATION_DTO)
      .setUpdatedAt(2_000L));
    dbSession.commit();

    Map<String, Object> row = selectSingleRow();
    assertThat(row.get("createdAt")).isEqualTo(DATE_1);
    assertThat(row.get("updatedAt")).isEqualTo(DATE_2);
  }

  @Test
  public void update_does_not_update_key_nor_createdAt() {
    when(system2.now()).thenReturn(DATE_1);
    insertOrganization(ORGANIZATION_DTO);

    when(system2.now()).thenReturn(DATE_3);
    underTest.update(dbSession, new OrganizationDto()
      .setUuid(ORGANIZATION_DTO.getUuid())
      .setKey("new key")
      .setName("new name")
      .setDescription("new description")
      .setUrl("new url")
      .setAvatarUrl("new avatar url")
      .setCreatedAt(2_000L)
      .setUpdatedAt(3_000L));
    dbSession.commit();

    Map<String, Object> row = selectSingleRow();
    assertThat(row.get("uuid")).isEqualTo(ORGANIZATION_DTO.getUuid());
    assertThat(row.get("key")).isEqualTo(ORGANIZATION_DTO.getKey());
    assertThat(row.get("name")).isEqualTo("new name");
    assertThat(row.get("description")).isEqualTo("new description");
    assertThat(row.get("url")).isEqualTo("new url");
    assertThat(row.get("avatarUrl")).isEqualTo("new avatar url");
    assertThat(row.get("createdAt")).isEqualTo(DATE_1);
    assertThat(row.get("updatedAt")).isEqualTo(DATE_3);
  }

  @Test
  public void update_fails_if_name_is_null() {
    insertOrganization(ORGANIZATION_DTO);

    expectedException.expect(PersistenceException.class);

    underTest.update(dbSession, copyOf(ORGANIZATION_DTO).setName(null));
  }

  @Test
  public void deleteByUuid_does_not_fail_on_empty_table() {
    assertThat(underTest.deleteByUuid(dbSession, "uuid")).isEqualTo(0);
    dbSession.commit();
  }

  @Test
  public void deleteByUuid_does_not_fail_on_non_existing_row() {
    insertOrganization(ORGANIZATION_DTO);

    assertThat(underTest.deleteByUuid(dbSession, "uuid")).isEqualTo(0);
    dbSession.commit();
  }

  @Test
  public void deleteByUuid_deletes_row_with_specified_uuid() {
    insertOrganization(ORGANIZATION_DTO);
    String anotherUuid = "uuid";
    insertOrganization(copyOf(ORGANIZATION_DTO).setUuid(anotherUuid).setKey("key"));

    assertThat(dbTester.countRowsOfTable("organizations")).isEqualTo(2);
    assertThat(underTest.deleteByUuid(dbSession, anotherUuid)).isEqualTo(1);
    dbSession.commit();

    assertThat(underTest.selectByUuid(dbSession, anotherUuid)).isEmpty();
    assertThat(underTest.selectByUuid(dbSession, ORGANIZATION_DTO.getUuid())).isNotEmpty();
    assertThat(dbTester.countRowsOfTable("organizations")).isEqualTo(1);

    assertThat(underTest.deleteByUuid(dbSession, anotherUuid)).isEqualTo(0);
    assertThat(underTest.deleteByUuid(dbSession, ORGANIZATION_DTO.getUuid())).isEqualTo(1);
    dbSession.commit();

    assertThat(underTest.selectByUuid(dbSession, ORGANIZATION_DTO.getUuid())).isEmpty();
    assertThat(dbTester.countRowsOfTable("organizations")).isEqualTo(0);
  }

  @Test
  public void deleteByKey_does_not_fail_on_empty_table() {
    assertThat(underTest.deleteByKey(dbSession, "key")).isEqualTo(0);
    dbSession.commit();
  }

  @Test
  public void deleteByKey_does_not_fail_on_non_existing_row() {
    insertOrganization(ORGANIZATION_DTO);

    assertThat(underTest.deleteByKey(dbSession, "key")).isEqualTo(0);
    dbSession.commit();
  }

  @Test
  public void deleteByUuid_deletes_row_with_specified_key() {
    insertOrganization(ORGANIZATION_DTO);
    String anotherKey = "key";
    insertOrganization(copyOf(ORGANIZATION_DTO).setUuid("uuid").setKey(anotherKey));

    assertThat(dbTester.countRowsOfTable("organizations")).isEqualTo(2);
    assertThat(underTest.deleteByKey(dbSession, anotherKey)).isEqualTo(1);
    dbSession.commit();

    assertThat(underTest.selectByKey(dbSession, anotherKey)).isEmpty();
    assertThat(underTest.selectByKey(dbSession, ORGANIZATION_DTO.getKey())).isNotEmpty();
    assertThat(dbTester.countRowsOfTable("organizations")).isEqualTo(1);

    assertThat(underTest.deleteByKey(dbSession, anotherKey)).isEqualTo(0);
    assertThat(underTest.deleteByKey(dbSession, ORGANIZATION_DTO.getKey())).isEqualTo(1);
    dbSession.commit();

    assertThat(underTest.selectByKey(dbSession, ORGANIZATION_DTO.getKey())).isEmpty();
    assertThat(dbTester.countRowsOfTable("organizations")).isEqualTo(0);
  }

  private void expectDtoCanNotBeNull() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("OrganizationDto can't be null");
  }

  private void insertOrganization(OrganizationDto dto) {
    underTest.insert(dbSession, dto);
    dbSession.commit();
  }

  private void verify(Optional<OrganizationDto> optional) {
    assertThat(optional).isNotEmpty();
    OrganizationDto dto = optional.get();
    assertThat(dto.getUuid()).isEqualTo(ORGANIZATION_DTO.getUuid());
    assertThat(dto.getKey()).isEqualTo(ORGANIZATION_DTO.getKey());
    assertThat(dto.getName()).isEqualTo(ORGANIZATION_DTO.getName());
    assertThat(dto.getDescription()).isEqualTo(ORGANIZATION_DTO.getDescription());
    assertThat(dto.getUrl()).isEqualTo(ORGANIZATION_DTO.getUrl());
    assertThat(dto.getAvatarUrl()).isEqualTo(ORGANIZATION_DTO.getAvatarUrl());
    assertThat(dto.getCreatedAt()).isEqualTo(ORGANIZATION_DTO.getCreatedAt());
    assertThat(dto.getUpdatedAt()).isEqualTo(ORGANIZATION_DTO.getUpdatedAt());
  }

  private Map<String, Object> selectSingleRow() {
    List<Map<String, Object>> rows = dbTester.select("select" +
      " uuid as \"uuid\", kee as \"key\", name as \"name\",  description as \"description\", url as \"url\", avatar_url as \"avatarUrl\"," +
      " created_at as \"createdAt\", updated_at as \"updatedAt\"" +
      " from organizations");
    assertThat(rows).hasSize(1);
    return rows.get(0);
  }

  private static OrganizationDto copyOf(OrganizationDto organizationDto) {
    return new OrganizationDto()
      .setUuid(organizationDto.getUuid())
      .setKey(organizationDto.getKey())
      .setName(organizationDto.getName())
      .setDescription(organizationDto.getDescription())
      .setUrl(organizationDto.getUrl())
      .setAvatarUrl(organizationDto.getAvatarUrl());
  }
}
