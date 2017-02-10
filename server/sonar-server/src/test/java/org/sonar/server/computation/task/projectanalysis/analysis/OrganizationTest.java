/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.analysis;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.organization.OrganizationDto;

import static org.assertj.core.api.Assertions.assertThat;

public class OrganizationTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private OrganizationDto underTest = new OrganizationDto();

  @Test
  public void build_throws_NPE_if_dto_is_null() {
    expectedException.expect(NullPointerException.class);

    Organization.from(null);
  }

  @Test
  public void build_throws_NPE_if_uuid_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("uuid can't be null");

    Organization.from(underTest);
  }

  @Test
  public void build_throws_NPE_if_key_is_null() {
    underTest.setUuid("uuid");
    
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("key can't be null");

    Organization.from(underTest);
  }

  @Test
  public void build_throws_NPE_if_name_is_null() {
    underTest.setUuid("uuid").setKey("key");
    
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("name can't be null");

    Organization.from(underTest);
  }

  @Test
  public void verify_getters() {
    Organization organization = Organization.from(underTest.setUuid("uuid").setKey("key").setName("name"));

    assertThat(organization.getUuid()).isEqualTo("uuid");
    assertThat(organization.getKey()).isEqualTo("key");
    assertThat(organization.getName()).isEqualTo("name");
  }

  @Test
  public void verify_toString() {
    Organization organization = Organization.from(underTest.setUuid("uuid").setKey("key").setName("name"));

    assertThat(organization.toString()).isEqualTo("Organization{uuid='uuid', key='key', name='name'}");
  }

  @Test
  public void equals_is_based_on_uuid_only() {
    Organization organization = Organization.from(underTest.setUuid("uuid").setKey("key").setName("name"));

    assertThat(organization).isEqualTo(Organization.from(underTest.setUuid("uuid").setKey("key").setName("name")));
    assertThat(organization).isEqualTo(Organization.from(underTest.setUuid("uuid").setKey("other key").setName("name")));
    assertThat(organization).isEqualTo(Organization.from(underTest.setUuid("uuid").setKey("key").setName("other name")));
    assertThat(organization).isNotEqualTo(Organization.from(underTest.setUuid("other uuid").setKey("key").setName("name")));
    assertThat(organization).isNotEqualTo(null);
    assertThat(organization).isNotEqualTo("toto");
  }

  @Test
  public void hashcode_is_based_on_uuid_only() {
    Organization organization = Organization.from(underTest.setUuid("uuid").setKey("key").setName("name"));

    assertThat(organization.hashCode()).isEqualTo("uuid".hashCode());
  }
}
