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
package org.sonar.server.ui;

import org.junit.Test;
import org.sonar.api.resources.ResourceDefinition;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class ResourceDefinitionRepositoryTest {

  @Test
  public void test() {
    ResourceDefinition def1 = ResourceDefinition.builder("1").build();
    ResourceDefinition def2 = ResourceDefinition.builder("2").build();
    ResourceDefinitionRepository repository = new ResourceDefinitionRepository(new ResourceDefinition[] {def1, def2});
    assertThat(repository.getAll(), hasItems(def1, def2));
    assertThat(repository.get("1"), is(def1));
    assertThat(repository.get("unknown"), notNullValue());
  }

}
