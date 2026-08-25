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
package org.sonar.core.config;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;

import static org.assertj.core.api.Assertions.assertThat;

class NewsPropertiesTest {

  @Test
  void all_shouldGetProperties() {
    assertThat(NewsProperties.all()).hasSize(1);
  }

  @Test
  void all_shouldDefineVisibleGlobalBooleanProperty() {
    PropertyDefinition definition = NewsProperties.all().get(0);

    assertThat(definition.key()).isEqualTo(NewsProperties.NEWS_ENABLED);
    assertThat(definition.type()).isEqualTo(PropertyType.BOOLEAN);
    assertThat(definition.defaultValue()).isEqualTo(Boolean.toString(true));
    assertThat(definition.category()).isEqualTo(CoreProperties.CATEGORY_GENERAL);
    assertThat(definition.subCategory()).isEqualTo(NewsProperties.NEWS_SUBCATEGORY);
  }

  @Test
  void all_shouldBeRegisteredInCorePropertyDefinitions() {
    List<PropertyDefinition> defs = CorePropertyDefinitions.all();

    assertThat(defs.stream().filter(def -> def.key().equals(NewsProperties.NEWS_ENABLED)).findFirst()).isPresent();
  }
}
