/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.db.user;

import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserQueryTest {

  @Test
  void copyWithNewRangeOfUserUuids_copyAllFieldsCorrectly() {
    UserQuery original = createUserQueryWithAllFieldsSet();

    Set<String> newRangeOfUsers = Set.of("user1");
    UserQuery copy = UserQuery.copyWithNewRangeOfUserUuids(original, newRangeOfUsers);

    assertThat(copy)
      .usingRecursiveComparison()
      .ignoringFields("userUuids")
      .isEqualTo(original);

    assertThat(copy.getUserUuids()).isEqualTo(newRangeOfUsers);
  }

  private static UserQuery createUserQueryWithAllFieldsSet() {
    OffsetDateTime offsetDateTime = OffsetDateTime.of(2024, Month.JANUARY.getValue(), 1, 0, 0, 0, 0, ZoneOffset.UTC);
    return UserQuery.builder()
      .userUuids(Set.of("user1", "user2"))
      .searchText("search text")
      .isActive(true)
      .isManagedClause("is managed clause")
      .lastConnectionDateFrom(offsetDateTime.minusDays(1))
      .lastConnectionDateTo(offsetDateTime.plus(1, ChronoUnit.DECADES))
      .sonarLintLastConnectionDateFrom(offsetDateTime.plusDays(2))
      .sonarLintLastConnectionDateTo(offsetDateTime.minus(2, ChronoUnit.DECADES))
      .externalLogin("externalLogin")
      .build();
  }
}
