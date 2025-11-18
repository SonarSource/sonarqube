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
package org.sonar.db.jira.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AtlassianAuthenticationDetailsDtoTest {

  @Test
  void toString_masks_secret() {
    var dto = new AtlassianAuthenticationDetailsDto()
      .setClientId("test-client-id")
      .setSecret("super-secret-value")
      .setCreatedAt(1000L)
      .setUpdatedAt(2000L);

    var toString = dto.toString();

    assertThat(toString)
      .contains("test-client-id")
      .contains("secret=******")
      .contains("1000")
      .contains("2000")
      .doesNotContain("super-secret-value");
  }

  @Test
  void toString_masks_secret_even_when_null() {
    var dto = new AtlassianAuthenticationDetailsDto()
      .setClientId("test-client-id")
      .setSecret(null);

    var toString = dto.toString();

    assertThat(toString)
      .contains("test-client-id")
      .contains("secret=******");
  }
}
