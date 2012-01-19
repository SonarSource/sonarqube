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

public class ResourceDefinitionTest {

  @Test
  public void shouldCreateWithDefaults() {
    ResourceDefinition def = ResourceDefinition.builder("qualifier")
        .build();
    assertThat(def.getQualifier(), is("qualifier"));
    assertThat(def.getIconPath(), is("/images/q/qualifier.png"));
  }

  @Test
  public void shouldCreate() {
    ResourceDefinition def = ResourceDefinition.builder("qualifier")
        .setIconPath("/custom-icon.png")
        .build();
    assertThat(def.getQualifier(), is("qualifier"));
    assertThat(def.getIconPath(), is("/custom-icon.png"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldCheckQualifierLength() {
    ResourceDefinition.builder("qualifier bigger than 10 characters");
  }

}
