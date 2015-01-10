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

import org.junit.Test;
import org.sonar.api.component.Component;
import org.sonar.api.component.MutablePerspective;
import org.sonar.api.component.Perspective;
import org.sonar.api.component.mock.MockSourceFile;
import org.sonar.core.graph.BeanVertex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GraphPerspectiveBuilderTest {

  @Test
  public void should_return_null_on_null_component() throws Exception {

    String componentKey = "org.foo.Bar";

    Component component = mock(Component.class);
    when(component.key()).thenReturn(componentKey);

    ScanGraph graph = mock(ScanGraph.class);
    when(graph.getComponent(componentKey)).thenReturn(null);

    GraphPerspectiveLoader perspectiveLoader = mock(GraphPerspectiveLoader.class);

    GraphPerspectiveBuilder perspectiveBuilder =
            new GraphPerspectiveBuilder(graph, MutablePerspective.class, null, perspectiveLoader) {};

    Perspective loadedPerspective = perspectiveBuilder.loadPerspective(MutablePerspective.class, component);

    assertThat(loadedPerspective).isNull();
  }

  @Test
  public void should_load_perspective() throws Exception {

    ScanGraph graph = ScanGraph.create();
    MutablePerspective expectedPerspective = mock(MutablePerspective.class);

    ComponentVertex fileComponent = graph.addComponent(MockSourceFile.createMain("org.foo.Bar"));
    GraphPerspectiveLoader perspectiveLoader = mock(GraphPerspectiveLoader.class);
    when(perspectiveLoader.load(fileComponent)).thenReturn(expectedPerspective);

    GraphPerspectiveBuilder perspectiveBuilder =
            new GraphPerspectiveBuilder(graph, MutablePerspective.class, null, perspectiveLoader) {};

    Perspective loadedPerspective = perspectiveBuilder.loadPerspective(MutablePerspective.class, fileComponent);

    assertThat(loadedPerspective).isEqualTo(expectedPerspective);
  }

  @Test
  public void should_create_perspective_when_loaded_one_is_null() throws Exception {

    String perspectiveKey = "perspectiveKey";

    ScanGraph graph = ScanGraph.create();
    ComponentVertex fileComponent = graph.addComponent(MockSourceFile.createMain("org.foo.Bar"));

    GraphPerspectiveLoader<DefaultMutablePerspective> perspectiveLoader =
            new GraphPerspectiveLoader<DefaultMutablePerspective>(perspectiveKey, DefaultMutablePerspective.class) {

              @Override
              public DefaultMutablePerspective load(ComponentVertex component) {
                return null;
              }

              @Override
              protected Class<? extends BeanVertex> getBeanClass() {
                return DefaultMutablePerspective.class;
              }
            };

    GraphPerspectiveBuilder perspectiveBuilder =
            new GraphPerspectiveBuilder(graph, MutablePerspective.class, null, perspectiveLoader) {};

    Perspective loadedPerspective = perspectiveBuilder.loadPerspective(MutablePerspective.class, fileComponent);

    assertThat(loadedPerspective).isNotNull();
    assertThat(loadedPerspective).isInstanceOf(DefaultMutablePerspective.class);
  }

  public static class DefaultMutablePerspective extends BeanVertex implements MutablePerspective {

    @Override
    public Component component() {
      return null;
    }
  }
}
