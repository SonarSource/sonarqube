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
package org.sonar.duplications.token;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class TokenTest {

  @Test
  public void test_equals() {
    Token token = new Token("value_1", 1, 2);

    Assertions.assertThat(token)
      .isEqualTo(token)
      .isNotEqualTo(null)
      .isNotEqualTo(new Object())
      .isNotEqualTo(new Token("value_1", 1, 0))
      .isNotEqualTo(new Token("value_1", 0, 2))
      .isNotEqualTo(new Token("value_2", 1, 2))
      .isEqualTo(new Token("value_1", 1, 2));
  }
}
