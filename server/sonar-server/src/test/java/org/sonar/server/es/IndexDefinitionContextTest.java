/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.es;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class IndexDefinitionContextTest {

  @Test
  public void create_indices() {
    IndexDefinition.IndexDefinitionContext context = new IndexDefinition.IndexDefinitionContext();

    context.create("issues");
    context.create("measures");
    assertThat(context.getIndices().keySet()).containsOnly("issues", "measures");
  }

  @Test
  public void fail_to_create_twice_the_same_index() {
    IndexDefinition.IndexDefinitionContext context = new IndexDefinition.IndexDefinitionContext();

    context.create("issues");
    try {
      context.create("issues");
      fail();
    } catch (IllegalArgumentException ok) {
      assertThat(ok).hasMessage("Index already exists: issues");
    }
  }
}
