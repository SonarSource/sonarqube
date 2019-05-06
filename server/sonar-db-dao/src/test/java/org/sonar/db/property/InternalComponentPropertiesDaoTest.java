/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.db.property;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang.math.RandomUtils;
import org.assertj.core.api.AbstractAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InternalComponentPropertiesDaoTest {

  private static final String SOME_KEY = "key1";
  private static final String SOME_COMPONENT = "component1";
  private static final String SOME_VALUE = "value";

  private System2 system2 = mock(System2.class);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester dbTester = DbTester.create(system2);
  private DbSession dbSession = dbTester.getSession();
  private UuidFactory uuidFactory = UuidFactoryFast.getInstance();
  private InternalComponentPropertiesDao underTest = new InternalComponentPropertiesDao(system2, uuidFactory);

  @Test
  public void insertOrUpdate_insert_property_if_it_doesnt_already_exist() {
    InternalComponentPropertyDto dto = new InternalComponentPropertyDto()
      .setKey(SOME_KEY)
      .setComponentUuid(SOME_COMPONENT)
      .setValue(SOME_VALUE);

    long now = RandomUtils.nextLong();
    when(system2.now()).thenReturn(now);

    underTest.insertOrUpdate(dbSession, dto);

    assertThatInternalProperty(dto.getUuid())
      .hasComponentUuid(SOME_COMPONENT)
      .hasKey(SOME_KEY)
      .hasValue(SOME_VALUE)
      .hasUpdatedAt(now)
      .hasCreatedAt(now);
  }

  @Test
  public void insertOrUpdate_update_property_if_it_already_exists() {
    long creationDate = 10L;
    when(system2.now()).thenReturn(creationDate);

    InternalComponentPropertyDto dto = saveDto();

    long updateDate = 20L;
    when(system2.now()).thenReturn(updateDate);

    dto.setValue("other value");

    underTest.insertOrUpdate(dbSession, dto);

    assertThatInternalProperty(dto.getUuid())
      .hasUpdatedAt(updateDate)
      .hasValue("other value")
      .hasCreatedAt(creationDate);
  }

  @Test
  public void select_by_component_uuid_and_key_returns_property() {
    saveDto();

    Optional<InternalComponentPropertyDto> result = underTest.selectByComponentUuidAndKey(dbSession, SOME_COMPONENT, SOME_KEY);
    assertThat(result.get())
      .extracting("componentUuid", "key", "value")
      .contains(SOME_COMPONENT, SOME_KEY, SOME_VALUE);
  }

  @Test
  public void select_by_component_uuid_and_key_returns_empty_when_it_doesnt_exist() {
    saveDto();

    assertThat(underTest.selectByComponentUuidAndKey(dbSession, "other_component", SOME_KEY)).isEmpty();
    assertThat(underTest.selectByComponentUuidAndKey(dbSession, SOME_COMPONENT, "other_key")).isEmpty();
  }

  @Test
  public void delete_by_component_uuid_and_key_deletes_property() {
    saveDto();

    assertThat(underTest.deleteByComponentUuidAndKey(dbSession, SOME_COMPONENT, SOME_KEY)).isEqualTo(1);
    assertThat(underTest.selectByComponentUuidAndKey(dbSession, SOME_COMPONENT, SOME_KEY)).isEmpty();
  }

  @Test
  public void delete_by_component_uuid_and_key_does_nothing_if_property_doesnt_exist() {
    saveDto();

    assertThat(underTest.deleteByComponentUuidAndKey(dbSession, SOME_COMPONENT, "other_key")).isEqualTo(0);
    assertThat(underTest.deleteByComponentUuidAndKey(dbSession, "other_component", SOME_KEY)).isEqualTo(0);
    assertThat(underTest.selectByComponentUuidAndKey(dbSession, SOME_COMPONENT, SOME_KEY)).isNotEmpty();
  }

  private InternalComponentPropertyDto saveDto() {
    InternalComponentPropertyDto dto = new InternalComponentPropertyDto()
      .setKey(SOME_KEY)
      .setComponentUuid(SOME_COMPONENT)
      .setValue(SOME_VALUE);

    underTest.insertOrUpdate(dbSession, dto);
    return dto;
  }

  private InternalComponentPropertyAssert assertThatInternalProperty(String uuid) {
    return new InternalComponentPropertiesDaoTest.InternalComponentPropertyAssert(dbTester, dbSession, uuid);
  }

  private static class InternalComponentPropertyAssert extends AbstractAssert<InternalComponentPropertiesDaoTest.InternalComponentPropertyAssert, InternalComponentPropertyDto> {

    public InternalComponentPropertyAssert(DbTester dbTester, DbSession dbSession, String uuid) {
      super(asInternalProperty(dbTester, dbSession, uuid), InternalComponentPropertyAssert.class);
    }

    private static InternalComponentPropertyDto asInternalProperty(DbTester dbTester, DbSession dbSession, String uuid) {
      Map<String, Object> row = dbTester.selectFirst(
        dbSession,
        "select" +
          " uuid as \"uuid\", component_uuid as \"componentUuid\", kee as \"key\", value as \"value\", updated_at as \"updatedAt\", created_at as \"createdAt\"" +
          " from internal_component_props" +
          " where uuid='" + uuid+ "'");
      return new InternalComponentPropertyDto()
        .setUuid((String) row.get("uuid"))
        .setComponentUuid((String) row.get("componentUuid"))
        .setKey((String) row.get("key"))
        .setValue((String) row.get("value"))
        .setUpdatedAt((Long) row.get("updatedAt"))
        .setCreatedAt((Long) row.get("createdAt"));
    }

    public void doesNotExist() {
      isNull();
    }

    public InternalComponentPropertiesDaoTest.InternalComponentPropertyAssert hasKey(String expected) {
      isNotNull();

      if (!Objects.equals(actual.getKey(), expected)) {
        failWithMessage("Expected Internal property to have column KEY to be <%s> but was <%s>", true, actual.getKey());
      }

      return this;
    }

    public InternalComponentPropertiesDaoTest.InternalComponentPropertyAssert hasComponentUuid(String expected) {
      isNotNull();

      if (!Objects.equals(actual.getComponentUuid(), expected)) {
        failWithMessage("Expected Internal property to have column COMPONENT_UUID to be <%s> but was <%s>", true, actual.getComponentUuid());
      }

      return this;
    }

    public InternalComponentPropertiesDaoTest.InternalComponentPropertyAssert hasValue(String expected) {
      isNotNull();

      if (!Objects.equals(actual.getValue(), expected)) {
        failWithMessage("Expected Internal property to have column VALUE to be <%s> but was <%s>", true, actual.getValue());
      }

      return this;
    }

    public InternalComponentPropertiesDaoTest.InternalComponentPropertyAssert hasCreatedAt(long expected) {
      isNotNull();

      if (!Objects.equals(actual.getCreatedAt(), expected)) {
        failWithMessage("Expected Internal property to have column CREATED_AT to be <%s> but was <%s>", expected, actual.getCreatedAt());
      }

      return this;
    }

    public InternalComponentPropertiesDaoTest.InternalComponentPropertyAssert hasUpdatedAt(long expected) {
      isNotNull();

      if (!Objects.equals(actual.getUpdatedAt(), expected)) {
        failWithMessage("Expected Internal property to have column UPDATED_AT to be <%s> but was <%s>", expected, actual.getUpdatedAt());
      }

      return this;
    }

  }

}
