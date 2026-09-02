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
package org.sonar.db.issue;

import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IssueCountDimensionDtoTest {

  @Test
  void constructor_setsAllFields() {
    var dto = new IssueCountDimensionDto(
      1,
      "MAJOR",
      "OPEN",
      "CONFIRMED",
      "SAFE",
      "MAIN",
      "java:S1234",
      "HIGH",
      "MEDIUM",
      "LOW",
      3);

    assertThat(dto.issueType()).isEqualTo(1);
    assertThat(dto.severity()).isEqualTo("MAJOR");
    assertThat(dto.issueStatus()).isEqualTo("OPEN");
    assertThat(dto.status()).isEqualTo("CONFIRMED");
    assertThat(dto.hotspotResolution()).isEqualTo("SAFE");
    assertThat(dto.codeScope()).isEqualTo("MAIN");
    assertThat(dto.ruleKey()).isEqualTo("java:S1234");
    assertThat(dto.issueCount()).isEqualTo(3);
    assertThat(dto.effectiveImpacts()).containsOnly(
      Map.entry("MAINTAINABILITY", "HIGH"),
      Map.entry("SECURITY", "MEDIUM"),
      Map.entry("RELIABILITY", "LOW"));
  }

  @Test
  void constructor_withNullableFields_returnsNulls() {
    var dto = new IssueCountDimensionDto(
      2,
      "MINOR",
      null,
      null,
      null,
      "TEST",
      "java:S5678",
      null,
      null,
      null,
      0);

    assertThat(dto.issueStatus()).isNull();
    assertThat(dto.status()).isNull();
    assertThat(dto.hotspotResolution()).isNull();
    assertThat(dto.effectiveImpacts()).isEmpty();
  }

  @Test
  void withMethods_returnACopyWithOnlyThatFieldChanged() {
    var dto = new IssueCountDimensionDto(1, "MAJOR", "OPEN", "OPEN", null, "MAIN", "java:S1234", null, null, null, 0);

    assertThat(dto.withIssueCount(42).issueCount()).isEqualTo(42);
    assertThat(dto.withCodeScope("TEST").codeScope()).isEqualTo("TEST");
    assertThat(dto.withSeverity("BLOCKER").severity()).isEqualTo("BLOCKER");
    assertThat(dto.issueCount()).isZero();
    assertThat(dto.codeScope()).isEqualTo("MAIN");
    assertThat(dto.severity()).isEqualTo("MAJOR");
  }

  @Test
  void effectiveImpacts_returnsOnlyNonNullImpacts() {
    var dto = new IssueCountDimensionDto(
      1,
      "MAJOR",
      "OPEN",
      "CONFIRMED",
      null,
      "MAIN",
      "java:S1234",
      "HIGH",
      null,
      "LOW",
      1);

    assertThat(dto.effectiveImpacts()).containsOnly(
      Map.entry("MAINTAINABILITY", "HIGH"),
      Map.entry("RELIABILITY", "LOW"));
  }

}
