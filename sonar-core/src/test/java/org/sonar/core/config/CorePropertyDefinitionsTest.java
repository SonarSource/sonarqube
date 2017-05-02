/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import java.util.Optional;
import org.junit.Test;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.database.DatabaseProperties.PROP_PASSWORD;

public class CorePropertyDefinitionsTest {

  @Test
  public void all() {
    List<PropertyDefinition> defs = CorePropertyDefinitions.all();
    assertThat(defs).hasSize(56);
  }

  @Test
  public void jdbc_password_property_has_password_type() {
    List<PropertyDefinition> defs = CorePropertyDefinitions.all();

    Optional<PropertyDefinition> prop = defs.stream().filter(def -> PROP_PASSWORD.equals(def.key())).findFirst();
    assertThat(prop.get().type()).isEqualTo(PropertyType.PASSWORD);
  }

}
