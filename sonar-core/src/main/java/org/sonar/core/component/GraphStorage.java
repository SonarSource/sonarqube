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
package org.sonar.core.component;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
import org.slf4j.LoggerFactory;
import org.sonar.core.graph.GraphDto;
import org.sonar.core.graph.GraphDtoMapper;
import org.sonar.core.graph.GraphWriter;
import org.sonar.core.persistence.BatchSession;
import org.sonar.core.persistence.MyBatis;

public class GraphStorage {
  private final MyBatis myBatis;
  private final ComponentGraph componentGraph;

  public GraphStorage(MyBatis myBatis, ComponentGraph componentGraph) {
    this.myBatis = myBatis;
    this.componentGraph = componentGraph;
  }

  public void save() {
    LoggerFactory.getLogger(GraphStorage.class).info("Persisting graphs of components");
    BatchSession session = myBatis.openBatchSession();
    GraphDtoMapper mapper = session.getMapper(GraphDtoMapper.class);
    try {
      TinkerGraph subGraph = new TinkerGraph();
      GraphWriter writer = new GraphWriter();
      for (Vertex component : componentGraph.getRootVertex().getVertices(Direction.OUT, "component")) {
        Long snapshotId = (Long) component.getProperty("sid");
        if (snapshotId != null) {
          String data = writer.write(componentGraph.getUnderlyingGraph());
          mapper.insert(new GraphDto()
            .setData(data).setFormat("graphson").setPerspective("testplan").setVersion(1)
            .setSnapshotId(snapshotId).setRootVertexId(component.getId().toString()));
          subGraph.clear();
        }
      }
      session.commit();
    } finally {
      session.close();
    }
  }
}
