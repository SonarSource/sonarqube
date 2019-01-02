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
package org.sonar.api.ce.measure.test;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestSettingsTest {

  TestSettings underTest = new TestSettings();

  @Test
  public void get_string_value() throws Exception {
    underTest.setValue("key", "value");

    assertThat(underTest.getString("key")).isEqualTo("value");
    assertThat(underTest.getString("unknown")).isNull();
  }

  @Test
  public void get_string_array_value() throws Exception {
    underTest.setValue("key", "value1,value2");

    assertThat(underTest.getStringArray("key")).containsOnly("value1", "value2");
    assertThat(underTest.getStringArray("unknown")).isEmpty();
  }
}
