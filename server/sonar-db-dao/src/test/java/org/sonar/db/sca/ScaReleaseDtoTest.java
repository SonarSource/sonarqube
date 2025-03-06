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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class ScaReleaseDtoTest {

  @Test
  void test_toBuilder_build_shouldRoundTrip() {
    var scaReleaseDto = new ScaReleaseDto("scaReleaseUuid",
      "componentUuid",
      "packageUrl",
      PackageManager.MAVEN,
      "foo:bar",
      "1.0.0",
      "MIT",
      true,
      1L,
      2L);
    assertThat(scaReleaseDto.toBuilder().build()).isEqualTo(scaReleaseDto);
  }

  @Test
  void test_identity_shouldIgnoreUuidAndUpdatableFields() {
    var scaReleaseDto = new ScaReleaseDto("scaReleaseUuid",
      "componentUuid",
      "packageUrl",
      PackageManager.MAVEN,
      "foo:bar",
      "1.0.0",
      "MIT",
      true,
      1L,
      2L);
    var scaReleaseDtoDifferentButSameIdentity = new ScaReleaseDto("differentUuid",
      "componentUuidDifferent",
      "packageUrl",
      PackageManager.NPM,
      "foo:bar-different",
      "2.0.0",
      "GPL-3.0",
      false,
      10L,
      30L);
    assertThat(scaReleaseDto.identity()).isEqualTo(scaReleaseDtoDifferentButSameIdentity.identity());
    assertThat(scaReleaseDto).isNotEqualTo(scaReleaseDtoDifferentButSameIdentity);
  }
}
