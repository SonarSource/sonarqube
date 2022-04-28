/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.db.rule;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class RuleDescriptionSectionDtoTest {
  private static final RuleDescriptionSectionDto SECTION = RuleDescriptionSectionDto.builder()
    .key("key")
    .uuid("uuid")
    .content("desc").build();

  @Test
  public void testEquals() {

    Assertions.assertThat(RuleDescriptionSectionDto.builder()
      .key("key")
      .uuid("uuid")
      .content("desc")
      .build())
      .isEqualTo(SECTION);

    Assertions.assertThat(SECTION).isEqualTo(SECTION);
  }

  @Test
  public void testToString() {
    Assertions.assertThat(SECTION).hasToString("RuleDescriptionSectionDto[uuid='uuid', key='key', content='desc']");
  }
}
