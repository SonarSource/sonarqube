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
package org.sonar.api.resources;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ResourceTypeTest {

  @Test
  public void shouldCreateWithDefaults() {
    ResourceType def = ResourceType.builder("qualifier")
        .build();
    assertThat(def.getQualifier(), is("qualifier"));
    assertThat(def.getIconPath(), is("/images/q/qualifier.png"));
    assertThat(def.hasSourceCode(), is(false));
  }

  @Test
  public void shouldCreate() {
    ResourceType def = ResourceType.builder("qualifier")
        .setIconPath("/custom-icon.png")
        .availableForFilters()
        .hasSourceCode()
        .build();
    assertThat(def.getQualifier(), is("qualifier"));
    assertThat(def.getIconPath(), is("/custom-icon.png"));
    assertThat(def.isAvailableForFilters(), is(true));
    assertThat(def.hasSourceCode(), is(true));
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

    assertThat(foo1.equals(foo1), is(true));
    assertThat(foo1.equals(foo2), is(true));
    assertThat(foo1.equals(bar), is(false));

    assertThat(foo1.hashCode(), is(foo1.hashCode()));
  }

}
