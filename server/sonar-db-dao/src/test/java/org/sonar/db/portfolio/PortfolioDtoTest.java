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
package org.sonar.db.portfolio;

import org.junit.jupiter.api.Test;
import org.sonar.db.component.ComponentQualifiers;

import static org.assertj.core.api.Assertions.assertThat;

class PortfolioDtoTest {
  @Test
  void setters_and_getters() {
    PortfolioDto dto = new PortfolioDto();

    dto.setDescription("desc")
      .setName("name")
      .setKee("kee")
      .setParentUuid("parent")
      .setRootUuid("root")
      .setSelectionExpression("exp")
      .setSelectionMode("mode")
      .setCreatedAt(1000L)
      .setUpdatedAt(2000L);

    assertThat(dto.getDescription()).isEqualTo("desc");
    assertThat(dto.getName()).isEqualTo("name");
    assertThat(dto.getKee()).isEqualTo("kee");
    assertThat(dto.getKey()).isEqualTo("kee");
    assertThat(dto.getParentUuid()).isEqualTo("parent");
    assertThat(dto.getRootUuid()).isEqualTo("root");
    assertThat(dto.getSelectionExpression()).isEqualTo("exp");
    assertThat(dto.getSelectionMode()).isEqualTo("mode");
    assertThat(dto.getCreatedAt()).isEqualTo(1000L);
    assertThat(dto.getUpdatedAt()).isEqualTo(2000L);

    dto.setKey("new_key");
    assertThat(dto.getKey()).isEqualTo("new_key");
  }

  @Test
  void getQualifier_whenRoot_shouldReturnVW() {
    PortfolioDto dto = new PortfolioDto();
    assertThat(dto.getQualifier()).isEqualTo(ComponentQualifiers.VIEW);
  }

  @Test
  void getQualifier_whenNotRoot_shouldReturnSVW() {
    PortfolioDto dto = new PortfolioDto().setParentUuid("parent");
    assertThat(dto.getQualifier()).isEqualTo(ComponentQualifiers.SUBVIEW);
  }
}
