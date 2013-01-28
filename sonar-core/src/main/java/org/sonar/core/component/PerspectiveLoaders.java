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

import com.google.common.collect.Maps;
import org.sonar.api.ServerComponent;
import org.sonar.api.component.Perspective;
import org.sonar.api.test.MutableTestPlan;
import org.sonar.api.test.MutableTestable;
import org.sonar.core.graph.GraphDao;
import org.sonar.core.graph.GraphDto;

import javax.annotation.CheckForNull;

import java.util.Map;

public class PerspectiveLoaders implements ServerComponent {

  private final GraphDao dao;
  private Map<Class<?>, PerspectiveBuilder<?>> builders = Maps.newHashMap();

  public PerspectiveLoaders(GraphDao dao, PerspectiveBuilder[] builders) {
    this.dao = dao;
    for (PerspectiveBuilder builder : builders) {
      // TODO check duplications
      this.builders.put(builder.getPerspectiveClass(), builder);
    }
  }

  @CheckForNull
  public Perspective as(String componentKey, String perspectiveKey) {
    GraphDto graphDto = dao.selectByComponent(perspectiveKey, componentKey);
    return doAs(perspectiveKey, graphDto);
  }

  @CheckForNull
  public Perspective as(long snapshotId, String perspectiveKey) {
    GraphDto graphDto = dao.selectBySnapshot(perspectiveKey, snapshotId);
    return doAs(perspectiveKey, graphDto);
  }

  private Perspective doAs(String perspectiveKey, GraphDto graphDto) {
    Perspective result = null;
    if (graphDto != null) {
      ComponentGraph graph = new GraphReader().read(graphDto.getData(), graphDto.getRootVertexId());
      ComponentWrapper componentWrapper = graph.wrap(graph.getRootVertex(), ComponentWrapper.class);
      if (perspectiveKey.equals("testplan")) {
        result = builders.get(MutableTestPlan.class).load(componentWrapper);
      } else if (perspectiveKey.equals("testable")) {
        result = builders.get(MutableTestable.class).load(componentWrapper);
      }
    }
    return result;
  }
}
