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
package org.sonar.core.config;

import java.util.List;
import org.junit.Test;
import org.sonar.api.config.PropertyDefinition;

import static org.assertj.core.api.Assertions.assertThat;

public class CorePropertyDefinitionsTest {

  @Test
  public void all() {
    List<PropertyDefinition> defs = CorePropertyDefinitions.all();
    assertThat(defs).hasSize(52);
  }

  @Test
  public void all_includes_scanner_properties() {
    List<PropertyDefinition> defs = CorePropertyDefinitions.all();

    assertThat(defs.stream()
      .filter(def -> def.key().equals(ScannerProperties.BRANCH_NAME))
      .findFirst()).isPresent();
  }
}
