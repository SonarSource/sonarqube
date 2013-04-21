/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.qualitymodel;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class CharacteristicTest {

  @Test
  public void testStringProperties() {
    Characteristic characteristic = Characteristic.create();
    characteristic.setProperty("foo", "bar");

    assertThat(characteristic.getProperty("foo")).isNotNull();
    assertThat(characteristic.getPropertyTextValue("foo", null)).isEqualTo("bar");
    assertThat(characteristic.getPropertyValue("foo", null)).isNull();

    assertThat(characteristic.getProperty("unknown")).isNull();
    assertThat(characteristic.getPropertyTextValue("unknown", null)).isNull();
  }

  @Test
  public void testDoubleProperties() {
    Characteristic characteristic = Characteristic.create();
    characteristic.setProperty("foo", 3.1);

    assertThat(characteristic.getProperty("foo")).isNotNull();
    assertThat(characteristic.getPropertyValue("foo", null)).isEqualTo(3.1);
    assertThat(characteristic.getPropertyTextValue("foo", null)).isNull();
  }

  @Test
  public void addProperty() {
    Characteristic characteristic = Characteristic.create();
    characteristic.addProperty(CharacteristicProperty.create("foo"));

    CharacteristicProperty property = characteristic.getProperty("foo");
    assertThat(property).isNotNull();
    assertThat(property.getCharacteristic()).isSameAs(characteristic);
  }

  @Test
  public void shouldCreateByName() {
    Characteristic characteristic = Characteristic.createByName("Foo");

    assertThat(characteristic.getKey()).isEqualTo("FOO");
    assertThat(characteristic.getName()).isEqualTo("Foo");
  }

  @Test
  public void shouldReturnDefaultValues() {
    Characteristic characteristic = Characteristic.create();
    characteristic.setProperty("foo", (String) null);
    characteristic.setProperty("bar", (Double) null);

    assertThat(characteristic.getPropertyTextValue("foo", "foodef")).isEqualTo("foodef");
    assertThat(characteristic.getPropertyTextValue("other", "otherdef")).isEqualTo("otherdef");
    assertThat(characteristic.getPropertyValue("bar", 3.14)).isEqualTo(3.14);
    assertThat(characteristic.getPropertyValue("other", 3.14)).isEqualTo(3.14);
  }
}
