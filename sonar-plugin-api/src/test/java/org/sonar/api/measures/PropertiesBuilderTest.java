/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.api.measures;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import org.junit.Test;

public class PropertiesBuilderTest {
  @Test
  public void buildMeasure() {
    PropertiesBuilder<Integer, Integer> builder = new PropertiesBuilder<>(CoreMetrics.CLASS_COMPLEXITY_DISTRIBUTION);
    Measure measure = builder
        .add(1, 30)
        .add(2, 27)
        .add(4, 50)
        .build();
    assertNotNull(measure);
    assertThat(measure.getData(), is("1=30;2=27;4=50"));
  }

  @Test
  public void sortKeys() {
    PropertiesBuilder<String, String> builder = new PropertiesBuilder<>(CoreMetrics.CLASS_COMPLEXITY_DISTRIBUTION);
    Measure measure = builder
        .add("foo", "fooooo")
        .add("bar", "baaaaar")
        .add("hello", "world")
        .build();
    assertNotNull(measure);
    assertThat(measure.getData(), is("bar=baaaaar;foo=fooooo;hello=world"));
  }

  @Test
  public void valueIsOptional() {
    PropertiesBuilder<String, String> builder = new PropertiesBuilder<>(CoreMetrics.CLASS_COMPLEXITY_DISTRIBUTION);
    Measure measure = builder
        .add("foo", null)
        .add("bar", "bar")
        .add("hello", "world")
        .build();
    assertNotNull(measure);
    assertThat(measure.getData(), is("bar=bar;foo=;hello=world"));
  }

  @Test
  public void clearBeforeBuildingOtherMeasure() {
    PropertiesBuilder<String, String> builder = new PropertiesBuilder<>(CoreMetrics.CLASS_COMPLEXITY_DISTRIBUTION);
    builder
        .add("foo", "foo")
        .add("bar", "bar")
        .add("hello", "world")
        .build();

    builder.clear();
    Measure measure = builder
        .add("1", "1")
        .add("2", "2")
        .add("foo", "other")
        .build();
    assertNotNull(measure);
    assertThat(measure.getData(), is("1=1;2=2;foo=other"));
  }
}
