/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.db.component;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ComponentLinkDtoTest {

  @Test
  public void test_getters_and_setters() {
    ComponentLinkDto dto = new ComponentLinkDto()
      .setId(1L)
      .setComponentUuid("ABCD")
      .setType("homepage")
      .setName("Home")
      .setHref("http://www.sonarqube.org");

    assertThat(dto.getId()).isEqualTo(1L);
    assertThat(dto.getComponentUuid()).isEqualTo("ABCD");
    assertThat(dto.getType()).isEqualTo("homepage");
    assertThat(dto.getName()).isEqualTo("Home");
    assertThat(dto.getHref()).isEqualTo("http://www.sonarqube.org");
  }

  @Test
  public void test_provided_types() {
    assertThat(ComponentLinkDto.PROVIDED_TYPES).hasSize(5);
  }
}
