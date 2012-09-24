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

import org.junit.Test;

import java.util.NoSuchElementException;

import static org.fest.assertions.Assertions.assertThat;

public class PropertySetDefinitionsTest {
  PropertySetDefinitions definitions = new PropertySetDefinitions();

  @Test
  public void should_register_by_name() {
    PropertySet set = new PropertySet();
    PropertySet other = new PropertySet();

    definitions.register("name", set);
    definitions.register("other", other);

    assertThat(definitions.findByName("name")).isSameAs(set);
    assertThat(definitions.findByName("other")).isSameAs(other);
    assertThat(definitions.findAll()).containsOnly(set, other);
  }

  @Test(expected = IllegalStateException.class)
  public void should_fail_to_register_twice() {
    definitions.register("name", new PropertySet());
    definitions.register("name", new PropertySet());
  }

  @Test(expected = NoSuchElementException.class)
  public void should_fail_to_find_unknown_set() {
    definitions.findByName("UNKNOWN");
  }
}
