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

import com.google.common.collect.Maps;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
import org.sonar.api.ServerSide;
import org.sonar.api.component.Perspective;
import org.sonar.core.graph.graphson.GraphsonReader;
import org.sonar.core.graph.jdbc.GraphDao;
import org.sonar.core.graph.jdbc.GraphDto;

import javax.annotation.CheckForNull;

import java.io.StringReader;
import java.util.Map;

@ServerSide
public class SnapshotPerspectives {

  private final GraphDao dao;
  private final Map<Class<?>, GraphPerspectiveLoader<?>> loaders = Maps.newHashMap();

  public SnapshotPerspectives(GraphDao dao, GraphPerspectiveLoader[] loaders) {
    this.dao = dao;
    for (GraphPerspectiveLoader loader : loaders) {
      // TODO check duplications
      this.loaders.put(loader.getPerspectiveClass(), loader);
    }
  }

  @CheckForNull
  public <T extends Perspective> T as(Class<T> perspectiveClass, String componentKey) {
    GraphPerspectiveLoader<T> builder = (GraphPerspectiveLoader<T>) loaders.get(perspectiveClass);
    if (builder == null) {
      throw new IllegalStateException();
    }
    GraphDto graphDto = dao.selectByComponent(builder.getPerspectiveKey(), componentKey);
    return doAs(builder, graphDto);
  }

  @CheckForNull
  public <T extends Perspective> T as(Class<T> perspectiveClass, long snapshotId) {
    GraphPerspectiveLoader<T> builder = (GraphPerspectiveLoader<T>) loaders.get(perspectiveClass);
    if (builder == null) {
      throw new IllegalStateException();
    }
    GraphDto graphDto = dao.selectBySnapshot(builder.getPerspectiveKey(), snapshotId);
    return doAs(builder, graphDto);
  }

  private <T extends Perspective> T doAs(GraphPerspectiveLoader<T> loader, GraphDto graphDto) {
    T result = null;
    if (graphDto != null) {
      SnapshotGraph graph = read(graphDto.getData(), graphDto.getRootVertexId());
      result = loader.load(graph.wrap(graph.getComponentRoot(), ComponentVertex.class));
    }
    return result;
  }

  private SnapshotGraph read(String data, String rootVertexId) {
    StringReader input = new StringReader(data);
    try {
      TinkerGraph graph = new TinkerGraph();
      new GraphsonReader().read(input, graph);
      return new SnapshotGraph(graph, rootVertexId);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
