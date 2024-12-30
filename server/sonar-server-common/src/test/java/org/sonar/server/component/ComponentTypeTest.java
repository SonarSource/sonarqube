/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.component;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ComponentTypeTest {

  @Test
  public void shouldCreateWithDefaults() {
    ComponentType def = ComponentType.builder("qualifier")
      .build();
    assertThat(def.getQualifier()).isEqualTo("qualifier");
    assertThat(def.getIconPath()).isEqualTo("/images/q/qualifier.png");
    assertThat(def.hasSourceCode()).isFalse();
  }

  @Test
  public void shouldCreate() {
    ComponentType def = ComponentType.builder("qualifier")
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
    ComponentType.builder("qualifier bigger than 10 characters");
  }

  @Test
  public void testEqualsAndHashCode() {
    ComponentType foo1 = ComponentType.builder("FOO").build();
    ComponentType foo2 = ComponentType.builder("FOO").build();
    ComponentType bar = ComponentType.builder("BAR").build();

    assertThat(foo1.equals(foo1)).isTrue();
    assertThat(foo1.equals(foo2)).isTrue();
    assertThat(foo1.equals(bar)).isFalse();

    assertThat(foo1).hasSameHashCodeAs(foo1);
  }

  @Test
  public void getBooleanProperty_is_set() {
    // set with boolean parameter
    ComponentType def = ComponentType.builder("qualifier").setProperty("test", true).build();
    assertThat(def.getBooleanProperty("test")).isTrue();

    def = ComponentType.builder("qualifier").setProperty("test", false).build();
    assertThat(def.getBooleanProperty("test")).isFalse();

    def = ComponentType.builder("qualifier").setProperty("test", "true").build();
    assertThat(def.getBooleanProperty("test")).isTrue();

    def = ComponentType.builder("qualifier").setProperty("test", "false").build();
    assertThat(def.getBooleanProperty("test")).isFalse();
  }

  @Test
  public void getBooleanProperty_is_not_set() {
    ComponentType def = ComponentType.builder("qualifier").build();
    assertThat(def.getBooleanProperty("test")).isFalse();
  }

  @Test
  public void hasProperty() {
    ComponentType def = ComponentType.builder("qualifier").build();
    assertThat(def.hasProperty("foo")).isFalse();

    def = ComponentType.builder("qualifier").setProperty("foo", "bar").build();
    assertThat(def.hasProperty("foo")).isTrue();
  }
}
