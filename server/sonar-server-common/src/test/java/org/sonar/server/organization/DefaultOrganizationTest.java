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
package org.sonar.server.organization;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultOrganizationTest {
  private static final long DATE_2 = 2_000_000L;
  private static final long DATE_1 = 1_000_000L;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DefaultOrganization.Builder populatedBuilder = new DefaultOrganization.Builder()
    .setUuid("uuid")
    .setKey("key")
    .setName("name")
    .setCreatedAt(DATE_1)
    .setUpdatedAt(DATE_2);

  @Test
  public void build_fails_if_uuid_is_null() {
    populatedBuilder.setUuid(null);

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("uuid can't be null");

    populatedBuilder.build();
  }

  @Test
  public void build_fails_if_key_is_null() {
    populatedBuilder.setKey(null);

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("key can't be null");

    populatedBuilder.build();
  }

  @Test
  public void build_fails_if_name_is_null() {
    populatedBuilder.setName(null);

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("name can't be null");

    populatedBuilder.build();
  }

  @Test
  public void build_fails_if_createdAt_not_set() {
    DefaultOrganization.Builder underTest = new DefaultOrganization.Builder()
      .setUuid("uuid")
      .setKey("key")
      .setName("name")
      .setUpdatedAt(DATE_2);

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("createdAt can't be null");

    underTest.build();
  }

  @Test
  public void build_fails_if_updatedAt_not_set() {
    DefaultOrganization.Builder underTest = new DefaultOrganization.Builder()
      .setUuid("uuid")
      .setKey("key")
      .setName("name")
      .setCreatedAt(DATE_1);

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("updatedAt can't be null");

    underTest.build();
  }

  @Test
  public void verify_toString() {
    assertThat(populatedBuilder.build().toString())
      .isEqualTo("DefaultOrganization{uuid='uuid', key='key', name='name', createdAt=1000000, updatedAt=2000000}");
  }

  @Test
  public void verify_getters() {
    DefaultOrganization underTest = populatedBuilder.build();

    assertThat(underTest.getUuid()).isEqualTo("uuid");
    assertThat(underTest.getKey()).isEqualTo("key");
    assertThat(underTest.getName()).isEqualTo("name");
    assertThat(underTest.getCreatedAt()).isEqualTo(DATE_1);
    assertThat(underTest.getUpdatedAt()).isEqualTo(DATE_2);
  }
}
