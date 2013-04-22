/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.qualitymodel;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class CharacteristicPropertyTest {

  @Test
  public void testNullValues() {
    CharacteristicProperty property = CharacteristicProperty.create("foo");
    assertThat(property.getTextValue(), nullValue());
    assertThat(property.getValue(), nullValue());
    assertThat(property.getValueAsLong(), nullValue());
  }

  @Test
  public void testNumericValue() {
    CharacteristicProperty property = CharacteristicProperty.create("foo");
    property.setValue(3.14);
    assertThat(property.getValue(), is(3.14));//stored in the value column
    assertThat(property.getValueAsLong(), is(3L));
    assertThat(property.getTextValue(), nullValue());
  }

  @Test
  public void testTextValue() {
    CharacteristicProperty property = CharacteristicProperty.create("foo");
    property.setTextValue("bar");
    assertThat(property.getTextValue(), is("bar"));
    assertThat(property.getValue(), nullValue());
    assertThat(property.getValueAsLong(), nullValue());
  }
}
