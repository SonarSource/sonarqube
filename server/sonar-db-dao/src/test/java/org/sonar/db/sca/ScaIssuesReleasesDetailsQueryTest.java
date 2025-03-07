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

import java.util.List;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScaIssuesReleasesDetailsQueryTest {

  @Test
  void test_toBuilder_build_shouldRoundTrip() {
    var query = new ScaIssuesReleasesDetailsQuery("branchUuid", ScaIssuesReleasesDetailsQuery.Sort.IDENTITY_ASC,
      "vulnerabilityIdSubstring", "packageNameSubstring", true,
      List.of(ScaIssueType.VULNERABILITY), List.of(ScaSeverity.BLOCKER), List.of(PackageManager.NPM));
    AssertionsForClassTypes.assertThat(query.toBuilder().build()).isEqualTo(query);
  }

  @Test
  void test_queryParameterValues() {
    for (var value : ScaIssuesReleasesDetailsQuery.Sort.values()) {
      var queryParameterValue = value.queryParameterValue();
      var fromQueryParameterValue = ScaIssuesReleasesDetailsQuery.Sort.fromQueryParameterValue(queryParameterValue);
      assertThat(fromQueryParameterValue).contains(value);
      assertThat((queryParameterValue.startsWith("+") && value.name().endsWith("_ASC")) ||
        (queryParameterValue.startsWith("-") && value.name().endsWith("_DESC")))
          .as("+/- prefix and ASC/DESC suffix line up")
          .isTrue();
    }
  }

  @Test
  void test_whenValueIsInvalid_fromQueryParameterValue() {
    assertThat(ScaIssuesReleasesDetailsQuery.Sort.fromQueryParameterValue("invalid")).isEmpty();
  }
}
