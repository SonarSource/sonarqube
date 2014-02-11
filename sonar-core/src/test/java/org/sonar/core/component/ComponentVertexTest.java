/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.core.component;

import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
import org.junit.Test;
import org.sonar.api.component.Component;
import org.sonar.api.component.mock.MockSourceFile;
import org.sonar.api.resources.Qualifiers;
import org.sonar.core.graph.BeanGraph;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ComponentVertexTest {
  @Test
  public void should_copy() {
    BeanGraph beanGraph = new BeanGraph(new TinkerGraph());
    ComponentVertex vertex = beanGraph.createVertex(ComponentVertex.class);
    Component file = MockSourceFile.createMain("myproject:org/Foo.java").setName("Foo.java").setQualifier(Qualifiers.FILE)
      .setPath("src/org/Foo.java");

    vertex.copyFrom(file);

    assertThat(vertex.key()).isEqualTo("myproject:org/Foo.java");
    assertThat(vertex.name()).isEqualTo("Foo.java");
    assertThat(vertex.qualifier()).isEqualTo(Qualifiers.FILE);
    assertThat(vertex.path()).isEqualTo("src/org/Foo.java");
  }

  @Test
  public void should_copy_db_ids() {
    BeanGraph beanGraph = new BeanGraph(new TinkerGraph());
    ComponentVertex vertex = beanGraph.createVertex(ComponentVertex.class);
    ResourceComponent component = mock(ResourceComponent.class);
    when(component.resourceId()).thenReturn(123L);
    when(component.snapshotId()).thenReturn(456L);

    vertex.copyFrom(component);

    assertThat(vertex.element().getProperty("rid")).isEqualTo(123L);
    assertThat(vertex.element().getProperty("sid")).isEqualTo(456L);
  }
}
