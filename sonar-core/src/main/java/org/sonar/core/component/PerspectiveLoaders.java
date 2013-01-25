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

import org.sonar.api.ServerComponent;
import org.sonar.api.component.Perspective;
import org.sonar.core.graph.GraphDao;
import org.sonar.core.graph.GraphDto;
import org.sonar.core.test.DefaultTestPlan;
import org.sonar.core.test.DefaultTestable;

import javax.annotation.CheckForNull;

public class PerspectiveLoaders implements ServerComponent {

  private final GraphDao dao;

  public PerspectiveLoaders(GraphDao dao) {
    this.dao = dao;
  }

  @CheckForNull
  <T extends Perspective> T as(String componentKey, String perspectiveKey) {
    GraphDto graphDto = dao.selectByComponent(perspectiveKey, componentKey);
    return doAs(perspectiveKey, graphDto);
  }

  @CheckForNull
  <T extends Perspective> T as(long snapshotId, String perspectiveKey) {
    GraphDto graphDto = dao.selectBySnapshot(perspectiveKey, snapshotId);
    return doAs(perspectiveKey, graphDto);
  }

  private <T extends Perspective> T doAs(String perspectiveKey, GraphDto graphDto) {
    T result = null;
    if (graphDto != null) {
      ComponentGraph graph = new GraphReader().read(graphDto.getData(), graphDto.getRootVertexId());
      if (perspectiveKey.equals("testplan")) {
        result = (T) graph.wrap(graph.getRootVertex(), DefaultTestPlan.class);
      } else if (perspectiveKey.equals("testable")) {
        result = (T) graph.wrap(graph.getRootVertex(), DefaultTestable.class);
      }
    }
    return result;
  }
}
