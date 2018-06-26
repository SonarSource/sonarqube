/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.view.index;

import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.server.es.IndexDefinition;
import org.sonar.server.es.NewIndex;

import static org.assertj.core.api.Assertions.assertThat;

public class ViewIndexDefinitionTest {

  IndexDefinition.IndexDefinitionContext underTest = new IndexDefinition.IndexDefinitionContext();

  @Test
  public void define() {
    ViewIndexDefinition def = new ViewIndexDefinition(new MapSettings().asConfig());
    def.define(underTest);

    assertThat(underTest.getIndices()).hasSize(1);
    NewIndex index = underTest.getIndices().get("views");
    assertThat(index).isNotNull();
    assertThat(index.getTypes().keySet()).containsOnly("view");

    assertThat(index.getSettings().get("index.number_of_shards")).isEqualTo("5");
    assertThat(index.getSettings().get("index.number_of_replicas")).isEqualTo("0");
  }
}
