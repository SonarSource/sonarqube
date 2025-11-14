/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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


import org.junit.jupiter.api.Test;

import static com.google.common.base.Strings.repeat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.test.TestUtils.hasOnlyPrivateConstructors;

class ComponentValidatorTest {

  @Test
  void check_name() {
    String name = repeat("a", 500);

    assertThat(ComponentValidator.checkComponentName(name)).isEqualTo(name);
  }

  @Test
  void fail_when_name_longer_than_500_characters() {
    assertThatThrownBy(() -> ComponentValidator.checkComponentName(repeat("a", 500 + 1)))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Component name length");
  }

  @Test
  void check_key() {
    String key = repeat("a", 400);

    assertThat(ComponentValidator.checkComponentKey(key)).isEqualTo(key);
  }

  @Test
  void fail_when_key_longer_than_400_characters() {
    assertThatThrownBy(() -> {
      String key = repeat("a", 400 + 1);
      ComponentValidator.checkComponentKey(key);
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Component key length");
  }

  @Test
  void check_qualifier() {
    String qualifier = repeat("a", 10);

    assertThat(ComponentValidator.checkComponentQualifier(qualifier)).isEqualTo(qualifier);
  }

  @Test
  void fail_when_qualifier_is_longer_than_10_characters() {
    assertThatThrownBy(() -> ComponentValidator.checkComponentQualifier(repeat("a", 10 + 1)))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Component qualifier length");
  }

  @Test
  void private_constructor() {
    assertThat(hasOnlyPrivateConstructors(ComponentValidator.class)).isTrue();
  }
}
