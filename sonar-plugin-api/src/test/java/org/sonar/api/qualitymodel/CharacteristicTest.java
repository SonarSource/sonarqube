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
    Characteristic characteristic = Characteristic.create();
    characteristic.setProperty("foo", "bar");

    assertThat(characteristic.getProperty("foo"), notNullValue());
    assertThat(characteristic.getPropertyTextValue("foo", null), is("bar"));
    assertThat(characteristic.getPropertyValue("foo", null), nullValue());

    assertThat(characteristic.getProperty("unknown"), nullValue());
    assertThat(characteristic.getPropertyTextValue("unknown", null), nullValue());
  }

  @Test
  public void testDoubleProperties() {
    Characteristic characteristic = Characteristic.create();
    characteristic.setProperty("foo", 3.1);

    assertThat(characteristic.getProperty("foo"), notNullValue());
    assertThat(characteristic.getPropertyValue("foo", null), is(3.1));
    assertThat(characteristic.getPropertyTextValue("foo", null), nullValue());
  }

  @Test
  public void addProperty() {
    Characteristic characteristic = Characteristic.create();
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
  public void shouldReturnDefaultValues() {
    Characteristic characteristic = Characteristic.create();
    characteristic.setProperty("foo", (String)null);
    characteristic.setProperty("bar", (Double)null);

    assertThat(characteristic.getPropertyTextValue("foo", "foodef"), is("foodef"));
    assertThat(characteristic.getPropertyTextValue("other", "otherdef"), is("otherdef"));
    assertThat(characteristic.getPropertyValue("bar", 3.14), is(3.14));
    assertThat(characteristic.getPropertyValue("other", 3.14), is(3.14));
  }

  
}
