/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.api.qualitymodel;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class CharacteristicPropertyTest {

  @Test
  public void testNullValue() {
    CharacteristicProperty property = CharacteristicProperty.create("foo");
    assertThat(property.getValue(), nullValue());
    assertThat(property.getValueAsDouble(), nullValue());
    assertThat(property.getValueAsBoolean(), nullValue());
    assertThat(property.getValueAsLong(), nullValue());
  }

  @Test
  public void testBooleanValue() {
    CharacteristicProperty property = CharacteristicProperty.create("foo");
    property.setValue(true);
    assertThat(property.getValue(), is("true"));//stored in the text_value column
    assertThat(property.getValueAsDouble(), nullValue());
    assertThat(property.getValueAsBoolean(), is(true));
    assertThat(property.getValueAsLong(), nullValue());

    property.setValue(false);
    assertThat(property.getValue(), is("false"));
    assertThat(property.getValueAsBoolean(), is(false));
  }

  @Test
  public void testNumericValue() {
    CharacteristicProperty property = CharacteristicProperty.create("foo");
    property.setValue(3.14);
    assertThat(property.getValueAsDouble(), is(3.14));//stored in the value column
    assertThat(property.getValueAsLong(), is(3L));
    assertThat(property.getValue(), nullValue());
    assertThat(property.getValueAsBoolean(), nullValue());
  }

  @Test
  public void testStringValue() {
    CharacteristicProperty property = CharacteristicProperty.create("foo");
    property.setValue("bar");
    assertThat(property.getValue(), is("bar"));
    assertThat(property.getValueAsDouble(), nullValue());
    assertThat(property.getValueAsBoolean(), is(false));
    assertThat(property.getValueAsLong(), nullValue());
  }
}
