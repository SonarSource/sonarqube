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

import static junit.framework.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class CharacteristicTest {

  @Test
  public void testStringProperties() {
    Characteristic characteristic = new Characteristic();
    characteristic.setProperty("foo", "bar");

    assertThat(characteristic.getProperty("foo"), notNullValue());
    assertThat(characteristic.getPropertyValueAsString("foo"), is("bar"));
    assertThat(characteristic.getPropertyValueAsDouble("foo"), nullValue());

    assertThat(characteristic.getProperty("unknown"), nullValue());
    assertThat(characteristic.getPropertyValueAsString("unknown"), nullValue());
  }

  @Test
  public void testDoubleProperties() {
    Characteristic characteristic = new Characteristic();
    characteristic.setProperty("foo", 3.1);

    assertThat(characteristic.getProperty("foo"), notNullValue());
    assertThat(characteristic.getPropertyValueAsDouble("foo"), is(3.1));
    assertThat(characteristic.getPropertyValueAsString("foo"), nullValue());
  }

  @Test
  public void addProperty() {
    Characteristic characteristic = new Characteristic();
    characteristic.addProperty(CharacteristicProperty.create("foo"));

    CharacteristicProperty property = characteristic.getProperty("foo");
    assertThat(property, notNullValue());
    assertTrue(property.getCharacteristic()==characteristic);
  }

  @Test
  public void shouldCreateByName() {
    Characteristic characteristic = Characteristic.createByName("Foo");
    assertThat(characteristic.getKey(), is("FOO"));
    assertThat(characteristic.getName(), is("Foo"));
  }

  @Test
  public void shouldSetNameAsKey() {
    Characteristic characteristic = new Characteristic().setName("Foo", true);
    assertThat(characteristic.getKey(), is("FOO"));
    assertThat(characteristic.getName(), is("Foo"));

    characteristic = new Characteristic().setName(null, true);
    assertThat(characteristic.getKey(), nullValue());
    assertThat(characteristic.getName(), nullValue());
  }
}
