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
package org.sonar.server.permission;

import java.util.Random;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.user.GroupDto;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;

public class GroupIdOrAnyoneTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void from_fails_with_NPE_if_GroupDto_organizationUuid_is_null() {
    GroupDto dto = new GroupDto();

    expectOrganizationUuidNPE();

    GroupIdOrAnyone.from(dto);
  }

  @Test
  public void for_returns_isAnyone_if_id_is_null() {
    String organizationUuid = randomAlphabetic(10);
    GroupDto dto = new GroupDto();
    dto.setOrganizationUuid(organizationUuid);

    GroupIdOrAnyone underTest = GroupIdOrAnyone.from(dto);

    assertThat(underTest.getOrganizationUuid()).isEqualTo(organizationUuid);
    assertThat(underTest.isAnyone()).isTrue();
    assertThat(underTest.getId()).isNull();
  }

  @Test
  public void for_returns_isAnyone_false_if_id_is_not_null() {
    int id = new Random().nextInt(199);
    String organizationUuid = randomAlphabetic(10);
    GroupDto dto = new GroupDto();
    dto.setOrganizationUuid(organizationUuid);
    dto.setId(id);

    GroupIdOrAnyone underTest = GroupIdOrAnyone.from(dto);

    assertThat(underTest.getOrganizationUuid()).isEqualTo(organizationUuid);
    assertThat(underTest.isAnyone()).isFalse();
    assertThat(underTest.getId()).isEqualTo(id);
  }

  @Test
  public void forAnyone_fails_with_NPE_if_arg_is_null() {
    expectOrganizationUuidNPE();

    GroupIdOrAnyone.forAnyone(null);
  }

  @Test
  public void forAnyone_returns_isAnyone_true() {
    String organizationUuid = randomAlphabetic(12);

    GroupIdOrAnyone underTest = GroupIdOrAnyone.forAnyone(organizationUuid);

    assertThat(underTest.isAnyone()).isTrue();
    assertThat(underTest.getOrganizationUuid()).isEqualTo(organizationUuid);
    assertThat(underTest.getId()).isNull();
  }

  private void expectOrganizationUuidNPE() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("organizationUuid can't be null");
  }
}
