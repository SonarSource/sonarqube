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
package org.sonar.db.component;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ProjectLinkDtoTest {

  @Test
  public void test_getters_and_setters() {
    ProjectLinkDto dto = new ProjectLinkDto()
      .setUuid("ABCD")
      .setProjectUuid("EFGH")
      .setType("homepage")
      .setName("Home")
      .setHref("http://www.sonarqube.org")
      .setCreatedAt(1_000_000_000L)
      .setUpdatedAt(5_000_000_000L);

    assertThat(dto.getUuid()).isEqualTo("ABCD");
    assertThat(dto.getProjectUuid()).isEqualTo("EFGH");
    assertThat(dto.getType()).isEqualTo("homepage");
    assertThat(dto.getName()).isEqualTo("Home");
    assertThat(dto.getHref()).isEqualTo("http://www.sonarqube.org");
    assertThat(dto.getCreatedAt()).isEqualTo(1_000_000_000L);
    assertThat(dto.getUpdatedAt()).isEqualTo(5_000_000_000L);
  }

  @Test
  public void test_provided_types() {
    assertThat(ProjectLinkDto.PROVIDED_TYPES).hasSize(5);
  }
}
