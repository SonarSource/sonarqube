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
package org.sonar.batch.phases;

import com.tinkerpop.blueprints.Graph;
import org.sonar.api.component.Perspective;
import org.sonar.batch.index.ScanPersister;
import org.sonar.core.component.ComponentVertex;
import org.sonar.core.component.GraphPerspectiveBuilder;
import org.sonar.core.component.PerspectiveBuilder;
import org.sonar.core.component.ScanGraph;
import org.sonar.core.graph.SubGraph;
import org.sonar.core.graph.graphson.GraphsonMode;
import org.sonar.core.graph.graphson.GraphsonWriter;
import org.sonar.core.graph.jdbc.GraphDto;
import org.sonar.core.graph.jdbc.GraphDtoMapper;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;

import java.io.IOException;
import java.io.StringWriter;

public class GraphPersister implements ScanPersister {
  private final MyBatis myBatis;
  private final ScanGraph projectGraph;
  private final GraphPerspectiveBuilder[] builders;

  public GraphPersister(MyBatis myBatis, ScanGraph projectGraph, GraphPerspectiveBuilder[] builders) {
    this.myBatis = myBatis;
    this.projectGraph = projectGraph;
    this.builders = builders;
  }

  @Override
  public void persist() {
    DbSession session = myBatis.openSession(true);
    GraphDtoMapper mapper = session.getMapper(GraphDtoMapper.class);
    try {
      for (ComponentVertex component : projectGraph.getComponents()) {
        persistComponentGraph(mapper, component);
      }
      session.commit();
    } finally {
      session.close();
    }
  }

  private void persistComponentGraph(GraphDtoMapper mapper, ComponentVertex component) {
    Long snapshotId = (Long) component.element().getProperty("sid");
    if (snapshotId != null) {
      for (PerspectiveBuilder builder : builders) {
        GraphPerspectiveBuilder graphPerspectiveBuilder = (GraphPerspectiveBuilder) builder;
        Perspective perspective = graphPerspectiveBuilder.getPerspectiveLoader().load(component);
        if (perspective != null) {
          serializePerspectiveData(mapper, component, snapshotId, graphPerspectiveBuilder);
        }
      }
    }
  }

  private void serializePerspectiveData(GraphDtoMapper mapper, ComponentVertex component, Long snapshotId,
    GraphPerspectiveBuilder builder) {
    Graph subGraph = SubGraph.extract(component.element(), builder.path());
    String data = write(subGraph);
    mapper.insert(new GraphDto()
      .setData(data)
      .setFormat("graphson")
      .setPerspective(builder.getPerspectiveLoader().getPerspectiveKey())
      .setVersion(1)
      .setResourceId((Long) component.element().getProperty("rid"))
      .setSnapshotId(snapshotId)
      .setRootVertexId(component.element().getId().toString())
      );
  }

  private String write(Graph graph) {
    try (StringWriter output = new StringWriter()) {
      new GraphsonWriter().write(graph, output, GraphsonMode.EXTENDED);
      return output.toString();
    } catch (IOException e) {
      throw new IllegalStateException("Unable to write graph", e);
    }
  }
}
