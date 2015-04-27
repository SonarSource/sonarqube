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
package org.sonar.core.component;

import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
import org.junit.Test;
import org.sonar.api.component.Component;
import org.sonar.api.component.mock.MockSourceFile;
import org.sonar.api.resources.Qualifiers;
import org.sonar.core.graph.BeanGraph;

import static org.assertj.core.api.Assertions.assertThat;

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
}
