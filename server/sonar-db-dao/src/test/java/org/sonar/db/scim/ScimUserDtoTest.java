/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.db.scim;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

class ScimUserDtoTest {


  public static Object[][] testEqualsParameters() {
    return new Object[][]{
      {new ScimUserDto("uuidA", "userIdA"), new ScimUserDto("uuidA", "userIdA"), true},
      {new ScimUserDto("uuidA", "userIdA"), new ScimUserDto("uuidA", "userIdB"), false},
      {new ScimUserDto("uuidA", "userIdA"), new ScimUserDto("uuidB", "userIdA"), false},
      {new ScimUserDto("uuidA", "userIdA"), new ScimUserDto("uuidB", "userIdB"), false},
    };
  }

  @ParameterizedTest
  @MethodSource("testEqualsParameters")
  void testEquals(ScimUserDto scimUserDtoA, ScimUserDto scimUserDtoB, boolean expectedResult) {
    assertThat(scimUserDtoA.equals(scimUserDtoB)).isEqualTo(expectedResult);
  }
}
