/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.config;

import org.junit.Test;
import org.sonar.api.PropertyType;

import static org.fest.assertions.Assertions.assertThat;

public class PropertySetFieldTest {
  @Test
  public void should_set_name_and_type() {
    PropertySetField field = PropertySetField.create("name", PropertyType.STRING);

    assertThat(field.getName()).isEqualTo("name");
    assertThat(field.getType()).isEqualTo(PropertyType.STRING);
    assertThat(field.getDefaultValue()).isEmpty();
    assertThat(field.getDescription()).isEmpty();
  }

  @Test
  public void should_set_optional_characteristics() {
    PropertySetField field = PropertySetField.create("name", PropertyType.STRING);

    field.setDefaultValue("default");
    field.setDescription("description");

    assertThat(field.getDefaultValue()).isEqualTo("default");
    assertThat(field.getDescription()).isEqualTo("description");
  }
}
