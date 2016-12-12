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
package org.sonar.server.platform.db.migration.version.v62;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CreateDefaultOrganizationTest {
  private static final String DEFAULT_ORGANIZATION_KEY = "default-organization";
  private static final String DEFAULT_ORGANIZATION_NAME = "Default Organization";
  private static final String INTERNAL_PROPERTY_ORGANIZATION_DEFAULT = "organization.default";

  private System2 system2 = mock(System2.class);

  @Rule
  public final DbTester dbTester = DbTester.createForSchema(system2, CreateDefaultOrganizationTest.class, "organizations_and_internal_properties.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private UuidFactory uuidFactory = mock(UuidFactory.class);
  private CreateDefaultOrganization underTest = new CreateDefaultOrganization(dbTester.database(), system2, uuidFactory);

  @Test
  public void execute_insert_data_in_organizations_and_internal_properties_when_it_does_not_exist() throws SQLException {
    long now = 1_222_999L;
    String uuid = "a uuid";
    when(system2.now()).thenReturn(now);
    when(uuidFactory.create()).thenReturn(uuid);

    underTest.execute();

    OrganizationDto organizationDto = dbTester.getDbClient().organizationDao().selectByKey(dbTester.getSession(), DEFAULT_ORGANIZATION_KEY).get();
    assertThat(organizationDto.getUuid()).isEqualTo(uuid);
    assertThat(organizationDto.getKey()).isEqualTo(DEFAULT_ORGANIZATION_KEY);
    assertThat(organizationDto.getName()).isEqualTo(DEFAULT_ORGANIZATION_NAME);
    assertThat(organizationDto.getDescription()).isNull();
    assertThat(organizationDto.getUrl()).isNull();
    assertThat(organizationDto.getAvatarUrl()).isNull();
    assertThat(organizationDto.getCreatedAt()).isEqualTo(now);
    assertThat(organizationDto.getUpdatedAt()).isEqualTo(now);

    verifyInternalProperty(uuid);
  }

  @Test
  public void execute_inserts_internal_property_if_default_organization_already_exists() throws SQLException {
    long past = 2_999_033L;
    String uuid = "uuidAAAA";
    insertExistingOrganization(uuid, past);
    verifyExistingOrganization(uuid, past);

    underTest.execute();

    // existing organization is unchanged
    verifyExistingOrganization(uuid, past);

    // internal property created
    verifyInternalProperty(uuid);
  }

  private void verifyInternalProperty(String expectedUuid) {
    assertThat(dbTester.getDbClient().internalPropertiesDao().selectByKey(dbTester.getSession(), INTERNAL_PROPERTY_ORGANIZATION_DEFAULT))
      .contains(expectedUuid);
  }

  @Test
  public void execute_has_no_effect_if_organization_and_internal_property_already_exist() throws SQLException {
    long past = 2_999_033L;
    String uuid = "uuidAAAA";
    insertExistingOrganization(uuid, past);
    insertExistingInternalProperty(uuid);
    verifyExistingOrganization(uuid, past);
    verifyInternalProperty(uuid);

    underTest.execute();

    verifyExistingOrganization(uuid, past);
    verifyInternalProperty(uuid);
  }

  @Test
  public void execute_is_reentrant() throws SQLException {
    when(system2.now()).thenReturn(1_222_999L);
    when(uuidFactory.create()).thenReturn("a uuid");

    underTest.execute();

    underTest.execute();
  }

  private void insertExistingInternalProperty(String uuid) {
    dbTester.getDbClient().internalPropertiesDao().save(dbTester.getSession(), INTERNAL_PROPERTY_ORGANIZATION_DEFAULT, uuid);
    dbTester.commit();
  }

  private void insertExistingOrganization(String uuid, long past) {
    when(system2.now()).thenReturn(past);
    dbTester.getDbClient().organizationDao().insert(dbTester.getSession(),
      new OrganizationDto()
        .setUuid(uuid)
        .setKey(DEFAULT_ORGANIZATION_KEY)
        .setName("whatever"));
    dbTester.commit();
  }

  private void verifyExistingOrganization(String uuid, long past) {
    OrganizationDto organizationDto = dbTester.getDbClient().organizationDao().selectByKey(dbTester.getSession(), DEFAULT_ORGANIZATION_KEY).get();
    assertThat(organizationDto.getUuid()).isEqualTo(uuid);
    assertThat(organizationDto.getKey()).isEqualTo(DEFAULT_ORGANIZATION_KEY);
    assertThat(organizationDto.getName()).isEqualTo("whatever");
    assertThat(organizationDto.getDescription()).isNull();
    assertThat(organizationDto.getUrl()).isNull();
    assertThat(organizationDto.getAvatarUrl()).isNull();
    assertThat(organizationDto.getCreatedAt()).isEqualTo(past);
    assertThat(organizationDto.getUpdatedAt()).isEqualTo(past);
  }
}
