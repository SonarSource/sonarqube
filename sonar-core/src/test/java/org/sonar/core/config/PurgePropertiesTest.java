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
package org.sonar.core.config;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.junit.Test;
import org.sonar.api.config.PropertyDefinition;

import static org.assertj.core.api.Assertions.assertThat;

public class PurgePropertiesTest {

  @Test
  public void daysBeforeDeletingScannerCache_must_be_global_and_have_7_as_default() {
    List<PropertyDefinition> properties = PurgeProperties.all();

    Optional<PropertyDefinition> property = properties.stream()
      .filter(p -> Objects.equals(p.key(), PurgeConstants.DAYS_BEFORE_DELETING_SCANNER_CACHE))
      .findFirst();
    assertThat(property).isNotEmpty();
    assertThat(property.get().defaultValue()).isEqualTo("7");
    assertThat(property.get().configScopes()).isEmpty();
  }
}
