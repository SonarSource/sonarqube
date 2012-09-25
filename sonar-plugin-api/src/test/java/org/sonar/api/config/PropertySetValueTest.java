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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class PropertySetValueTest {
  @Test
  public void should_get_default_values() {
    PropertySetValue value = PropertySetValue.create(Maps.<String, String>newHashMap());

    assertThat(value.getString("UNKNOWN")).isEmpty();
    assertThat(value.getInt("UNKNOWN")).isZero();
    assertThat(value.getFloat("UNKNOWN")).isZero();
    assertThat(value.getLong("UNKNOWN")).isZero();
    assertThat(value.getDate("UNKNOWN")).isNull();
    assertThat(value.getDateTime("UNKNOWN")).isNull();
    assertThat(value.getStringArray("UNKNOWN")).isEmpty();
  }

  @Test
  public void should_get_values() {
    PropertySetValue value = PropertySetValue.create(ImmutableMap.<String, String>builder()
      .put("age", "12")
      .put("child", "true")
      .put("size", "12.4")
      .put("distance", "1000000000")
      .put("array", "1,2,3,4,5")
      .put("birth", "1975-01-29")
      .put("now", "2012-09-25T10:08:30+0100")
      .build());

    assertThat(value.getString("age")).isEqualTo("12");
    assertThat(value.getInt("age")).isEqualTo(12);
    assertThat(value.getBoolean("child")).isTrue();
    assertThat(value.getFloat("size")).isEqualTo(12.4f);
    assertThat(value.getLong("distance")).isEqualTo(1000000000L);
    assertThat(value.getStringArray("array")).contains("1", "2", "3", "4", "5");
    assertThat(value.getDate("birth")).isNotNull();
    assertThat(value.getDateTime("now")).isNotNull();
  }
}
