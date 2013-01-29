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

import com.tinkerpop.blueprints.Graph;
import org.slf4j.LoggerFactory;
import org.sonar.api.component.Perspective;
import org.sonar.core.graph.GraphWriter;
import org.sonar.core.graph.SubGraph;
import org.sonar.core.graph.jdbc.GraphDto;
import org.sonar.core.graph.jdbc.GraphDtoMapper;
import org.sonar.core.persistence.BatchSession;
import org.sonar.core.persistence.MyBatis;

public class ScanGraphStore {
  private final MyBatis myBatis;
  private final ScanGraph projectGraph;
  private final PerspectiveBuilder[] builders;

  public ScanGraphStore(MyBatis myBatis, ScanGraph projectGraph, PerspectiveBuilder[] builders) {
    this.myBatis = myBatis;
    this.projectGraph = projectGraph;
    this.builders = builders;
  }

  public void save() {
    LoggerFactory.getLogger(ScanGraphStore.class).info("Persisting graphs of components");
    BatchSession session = myBatis.openBatchSession();
    GraphDtoMapper mapper = session.getMapper(GraphDtoMapper.class);
    try {
      GraphWriter writer = new GraphWriter();
      for (ComponentVertex component : projectGraph.getComponents()) {
        Long snapshotId = (Long) component.element().getProperty("sid");
        if (snapshotId != null) {
          for (PerspectiveBuilder builder : builders) {
            Perspective perspective = builder.load(component);
            if (perspective != null) {
              Graph subGraph = SubGraph.extract(component.element(), builder.path());
              String data = writer.write(subGraph);
              mapper.insert(new GraphDto()
                .setData(data)
                .setFormat("graphson")
                .setPerspective(builder.getPerspectiveKey())
                .setVersion(1)
                .setResourceId((Long) component.element().getProperty("rid"))
                .setSnapshotId(snapshotId)
                .setRootVertexId(component.element().getId().toString())
              );
            }
          }
        }
      }
      session.commit();
    } finally {
      session.close();
    }
  }
}
