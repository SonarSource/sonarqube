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

import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;

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
    long createdAt = 10L;
    when(system2.now()).thenReturn(createdAt);

    underTest.insertOrUpdate(dbSession, SOME_COMPONENT, SOME_KEY, SOME_VALUE);

    InternalComponentPropertyDto dto = underTest.selectByComponentUuidAndKey(dbSession, SOME_COMPONENT, SOME_KEY).get();

    assertThat(dto.getComponentUuid()).isEqualTo(SOME_COMPONENT);
    assertThat(dto.getKey()).isEqualTo(SOME_KEY);
    assertThat(dto.getValue()).isEqualTo(SOME_VALUE);
    assertThat(dto.getUpdatedAt()).isEqualTo(createdAt);
    assertThat(dto.getCreatedAt()).isEqualTo(createdAt);
  }

  @Test
  public void insertOrUpdate_update_property_if_it_already_exists() {
    long createdAt = 10L;
    when(system2.now()).thenReturn(createdAt);

    InternalComponentPropertyDto dto = saveDto();

    long updatedAt = 20L;
    when(system2.now()).thenReturn(updatedAt);

    String newValue = "newValue";
    underTest.insertOrUpdate(dbSession, dto.getComponentUuid(), dto.getKey(), newValue);

    InternalComponentPropertyDto updatedDto = underTest.selectByComponentUuidAndKey(dbSession, dto.getComponentUuid(), dto.getKey()).get();

    assertThat(updatedDto.getComponentUuid()).isEqualTo(SOME_COMPONENT);
    assertThat(updatedDto.getKey()).isEqualTo(SOME_KEY);
    assertThat(updatedDto.getValue()).isEqualTo(newValue);
    assertThat(updatedDto.getUpdatedAt()).isEqualTo(updatedAt);
    assertThat(updatedDto.getCreatedAt()).isEqualTo(createdAt);
  }

  @Test
  public void replaceValue_sets_to_newValue_if_oldValue_matches_expected() {
    long createdAt = 10L;
    when(system2.now()).thenReturn(createdAt);
    InternalComponentPropertyDto dto = saveDto();

    long updatedAt = 20L;
    when(system2.now()).thenReturn(updatedAt);

    String newValue = "other value";
    underTest.replaceValue(dbSession, SOME_COMPONENT, SOME_KEY, SOME_VALUE, newValue);

    InternalComponentPropertyDto updatedDto = underTest.selectByComponentUuidAndKey(dbSession, dto.getComponentUuid(), dto.getKey()).get();

    assertThat(updatedDto.getValue()).isEqualTo(newValue);
    assertThat(updatedDto.getUpdatedAt()).isEqualTo(updatedAt);
    assertThat(updatedDto.getCreatedAt()).isEqualTo(createdAt);
  }

  @Test
  public void replaceValue_does_not_replace_if_oldValue_does_not_match_expected() {
    long createdAt = 10L;
    when(system2.now()).thenReturn(createdAt);
    InternalComponentPropertyDto dto = saveDto();

    long updatedAt = 20L;
    when(system2.now()).thenReturn(updatedAt);

    underTest.replaceValue(dbSession, SOME_COMPONENT, SOME_KEY, SOME_VALUE + "foo", "other value");

    InternalComponentPropertyDto updatedDto = underTest.selectByComponentUuidAndKey(dbSession, dto.getComponentUuid(), dto.getKey()).get();

    assertThat(updatedDto.getValue()).isEqualTo(SOME_VALUE);
    assertThat(updatedDto.getUpdatedAt()).isEqualTo(createdAt);
    assertThat(updatedDto.getCreatedAt()).isEqualTo(createdAt);
  }

  @Test
  public void replaceValue_does_not_insert_if_record_does_not_exist() {
    underTest.replaceValue(dbSession, SOME_COMPONENT, SOME_KEY, SOME_VALUE, "other value");
    assertThat(underTest.selectByComponentUuidAndKey(dbSession, SOME_COMPONENT, SOME_KEY)).isEmpty();
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
  public void delete_by_component_uuid_deletes_all_properties_with_given_componentUuid() {
    underTest.insertOrUpdate(dbSession, SOME_COMPONENT, SOME_KEY, SOME_VALUE);
    underTest.insertOrUpdate(dbSession, SOME_COMPONENT, "other_key", "foo");
    underTest.insertOrUpdate(dbSession, "other_component", SOME_KEY, SOME_VALUE);

    assertThat(underTest.deleteByComponentUuid(dbSession, SOME_COMPONENT)).isEqualTo(2);
    assertThat(underTest.selectByComponentUuidAndKey(dbSession, SOME_COMPONENT, SOME_KEY)).isEmpty();
    assertThat(underTest.selectByComponentUuidAndKey(dbSession, "other_component", SOME_KEY)).isNotEmpty();
  }

  @Test
  public void delete_by_component_uuid_and_key_does_nothing_if_property_doesnt_exist() {
    saveDto();

    assertThat(underTest.deleteByComponentUuid(dbSession, "other_component")).isEqualTo(0);
    assertThat(underTest.selectByComponentUuidAndKey(dbSession, SOME_COMPONENT, SOME_KEY)).isNotEmpty();
  }

  @Test
  public void loadDbKey_loads_dbKeys_for_all_components_with_given_property_and_value() {
    OrganizationDto organizationDto = dbTester.organizations().insert();
    ComponentDto portfolio1 = dbTester.components().insertPublicPortfolio(organizationDto);
    ComponentDto portfolio2 = dbTester.components().insertPublicPortfolio(organizationDto);
    ComponentDto portfolio3 = dbTester.components().insertPublicPortfolio(organizationDto);
    ComponentDto portfolio4 = dbTester.components().insertPublicPortfolio(organizationDto);

    underTest.insertOrUpdate(dbSession, portfolio1.uuid(), SOME_KEY, SOME_VALUE);
    underTest.insertOrUpdate(dbSession, portfolio2.uuid(), SOME_KEY, "bar");
    underTest.insertOrUpdate(dbSession, portfolio3.uuid(), "foo", SOME_VALUE);

    assertThat(underTest.selectDbKeys(dbSession, SOME_KEY, SOME_VALUE)).containsOnly(portfolio1.getDbKey());
  }

  private InternalComponentPropertyDto saveDto() {
    underTest.insertOrUpdate(dbSession, SOME_COMPONENT, SOME_KEY, SOME_VALUE);
    return underTest.selectByComponentUuidAndKey(dbSession, SOME_COMPONENT, SOME_KEY).get();
  }

}
