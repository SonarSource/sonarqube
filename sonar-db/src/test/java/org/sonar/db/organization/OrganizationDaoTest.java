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
import org.apache.ibatis.exceptions.PersistenceException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class OrganizationDaoTest {
  private static final OrganizationDto ORGANIZATION_DTO = new OrganizationDto()
    .setUuid("a uuid")
    .setKey("the_key")
    .setName("the name")
    .setDescription("the description")
    .setUrl("the url")
    .setAvatarUrl("the avatar url")
    .setCreatedAt(1_999_000L)
    .setUpdatedAt(1_888_000L);

  @Rule
  public final DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DbClient dbClient = dbTester.getDbClient();
  private DbSession dbSession = dbTester.getSession();

  private OrganizationDao underTest = dbClient.organizationDao();

  @Test
  public void insert_fails_with_NPE_if_OrganizationDto_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("OrganizationDto can't be null");

    underTest.insert(dbSession, null);
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
    insertOrganization(ORGANIZATION_DTO.setDescription(null).setUrl(null).setAvatarUrl(null));

    Map<String, Object> row = selectSingleRow();
    assertThat(row.get("uuid")).isEqualTo(ORGANIZATION_DTO.getUuid());
    assertThat(row.get("key")).isEqualTo(ORGANIZATION_DTO.getKey());
    assertThat(row.get("name")).isEqualTo(ORGANIZATION_DTO.getName());
    assertThat(row.get("description")).isNull();
    assertThat(row.get("url")).isNull();
    assertThat(row.get("avatarUrl")).isNull();
    assertThat(row.get("createdAt")).isEqualTo(ORGANIZATION_DTO.getCreatedAt());
    assertThat(row.get("updatedAt")).isEqualTo(ORGANIZATION_DTO.getUpdatedAt());
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
  public void selectByLKey_is_case_sensitive() {
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
}
