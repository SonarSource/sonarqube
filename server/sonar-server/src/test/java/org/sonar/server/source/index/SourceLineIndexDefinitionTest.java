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
package org.sonar.server.source.index;

import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.process.ProcessProperties;
import org.sonar.server.es.IndexDefinition;
import org.sonar.server.es.NewIndex;

import static org.assertj.core.api.Assertions.assertThat;

public class SourceLineIndexDefinitionTest {

  IndexDefinition.IndexDefinitionContext context = new IndexDefinition.IndexDefinitionContext();

  @Test
  public void define() {
    IndexDefinition def = new SourceLineIndexDefinition(new Settings());
    def.define(context);

    assertThat(context.getIndices()).hasSize(1);
    NewIndex index = context.getIndices().get("sourcelines");
    assertThat(index).isNotNull();
    assertThat(index.getTypes().keySet()).containsOnly("sourceline");

    // no cluster by default
    assertThat(index.getSettings().get("index.number_of_shards")).isEqualTo("1");
    assertThat(index.getSettings().get("index.number_of_replicas")).isEqualTo("0");
  }

  @Test
  public void enable_cluster() {
    Settings settings = new Settings();
    settings.setProperty(ProcessProperties.CLUSTER_ACTIVATE, true);
    IndexDefinition def = new SourceLineIndexDefinition(settings);
    def.define(context);

    NewIndex issuesIndex = context.getIndices().get("sourcelines");
    assertThat(issuesIndex.getSettings().get("index.number_of_shards")).isEqualTo("4");
    assertThat(issuesIndex.getSettings().get("index.number_of_replicas")).isEqualTo("1");
  }

}
