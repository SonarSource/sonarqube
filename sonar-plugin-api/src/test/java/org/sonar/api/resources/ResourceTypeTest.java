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
package org.sonar.api.resources;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class ResourceTypeTest {

  @Test
  public void shouldCreateWithDefaults() {
    ResourceType def = ResourceType.builder("qualifier")
      .build();
    assertThat(def.getQualifier()).isEqualTo("qualifier");
    assertThat(def.getIconPath()).isEqualTo("/images/q/qualifier.png");
    assertThat(def.hasSourceCode()).isFalse();
  }

  @Test
  public void shouldCreate() {
    ResourceType def = ResourceType.builder("qualifier")
      .setIconPath("/custom-icon.png")
      .hasSourceCode()
      .setProperty("supportsMeasureFilters", "true")
      .setProperty("anotherProperty", "foo")
      .build();
    assertThat(def.getQualifier()).isEqualTo("qualifier");
    assertThat(def.getIconPath()).isEqualTo("/custom-icon.png");
    assertThat(def.hasSourceCode()).isTrue();
    assertThat(def.getStringProperty("anotherProperty")).isEqualTo("foo");
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldCheckQualifierLength() {
    ResourceType.builder("qualifier bigger than 10 characters");
  }

  @Test
  public void testEqualsAndHashCode() {
    ResourceType foo1 = ResourceType.builder("FOO").build();
    ResourceType foo2 = ResourceType.builder("FOO").build();
    ResourceType bar = ResourceType.builder("BAR").build();

    assertThat(foo1.equals(foo1)).isTrue();
    assertThat(foo1.equals(foo2)).isTrue();
    assertThat(foo1.equals(bar)).isFalse();

    assertThat(foo1.hashCode()).isEqualTo(foo1.hashCode());
  }

  @Test
  public void getBooleanProperty_is_set() {
    // set with boolean parameter
    ResourceType def = ResourceType.builder("qualifier").setProperty("test", true).build();
    assertThat(def.getBooleanProperty("test")).isTrue();

    def = ResourceType.builder("qualifier").setProperty("test", false).build();
    assertThat(def.getBooleanProperty("test")).isFalse();

    def = ResourceType.builder("qualifier").setProperty("test", "true").build();
    assertThat(def.getBooleanProperty("test")).isTrue();

    def = ResourceType.builder("qualifier").setProperty("test", "false").build();
    assertThat(def.getBooleanProperty("test")).isFalse();
  }

  @Test
  public void getBooleanProperty_is_not_set() {
    ResourceType def = ResourceType.builder("qualifier").build();
    assertThat(def.getBooleanProperty("test")).isFalse();
  }

  @Test
  public void hasProperty() {
    ResourceType def = ResourceType.builder("qualifier").build();
    assertThat(def.hasProperty("foo")).isFalse();

    def = ResourceType.builder("qualifier").setProperty("foo", "bar").build();
    assertThat(def.hasProperty("foo")).isTrue();
  }
}
