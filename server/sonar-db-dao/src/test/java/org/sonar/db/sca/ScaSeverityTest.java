/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.db.sca;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.sonar.api.issue.impact.Severity;

import static org.assertj.core.api.Assertions.assertThat;

class ScaSeverityTest {
  private static void assertSortOrder(ScaSeverity lower, ScaSeverity higher) {
    assertThat(lower.databaseSortKey())
      .as(lower + " sorts below " + higher)
      .isLessThan(higher.databaseSortKey());
  }

  @Test
  void test_maxLength() {
    for (ScaSeverity severity : ScaSeverity.values()) {
      assertThat(severity.name().length()).as(severity.name() + " is short enough")
        .isLessThanOrEqualTo(ScaSeverity.MAX_NAME_LENGTH);
    }
  }

  @Test
  void test_sortKeysInOrder() {
    assertSortOrder(ScaSeverity.INFO, ScaSeverity.LOW);
    assertSortOrder(ScaSeverity.LOW, ScaSeverity.MEDIUM);
    assertSortOrder(ScaSeverity.MEDIUM, ScaSeverity.HIGH);
    assertSortOrder(ScaSeverity.HIGH, ScaSeverity.BLOCKER);
  }

  @Test
  void test_matchesImpactSeverity() {
    assertThat(Stream.of(ScaSeverity.values()).map(Enum::name).toList())
      .as("ScaSeverity has the same values in the same order as impact.Severity")
      .isEqualTo(Stream.of(Severity.values()).map(Enum::name).toList());
  }
}
