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

package org.sonar.server.computation.step;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.List;
import javax.annotation.Nonnull;
import org.sonar.server.computation.component.ComponentVisitor;
import org.sonar.server.computation.container.ComputeEngineContainer;
import org.sonar.server.computation.issue.CloseIssuesOnRemovedComponentsVisitor;
import org.sonar.server.computation.issue.IntegrateIssuesVisitor;
import org.sonar.server.computation.issue.LoadComponentUuidsHavingOpenIssuesVisitor;
import org.sonar.server.computation.measure.MeasureComputersVisitor;
import org.sonar.server.computation.sqale.SqaleMeasuresVisitor;

/**
 * Ordered list of component visitors to be executed by {@link ExecuteVisitorsStep}
 */
public class ComponentVisitors {

  private static final List<Class<? extends ComponentVisitor>> ORDERED_VISITOR_CLASSES = ImmutableList.of(
    LoadComponentUuidsHavingOpenIssuesVisitor.class,
    IntegrateIssuesVisitor.class,
    CloseIssuesOnRemovedComponentsVisitor.class,

    SqaleMeasuresVisitor.class,

    // Must be after all other visitors as it requires measures computed by previous visitors
    MeasureComputersVisitor.class    
  );

  /**
   * List of all {@link ComponentVisitor}, ordered by execution sequence.
   */
  public List<Class<? extends ComponentVisitor>> orderedClasses() {
    return ORDERED_VISITOR_CLASSES;
  }

  private final ComputeEngineContainer computeEngineContainer;

  public ComponentVisitors(ComputeEngineContainer computeEngineContainer) {
    this.computeEngineContainer = computeEngineContainer;
  }

  public Iterable<ComponentVisitor> instances() {
    return Iterables.transform(orderedClasses(), new Function<Class<? extends ComponentVisitor>, ComponentVisitor>() {
      @Override
      public ComponentVisitor apply(@Nonnull Class<? extends ComponentVisitor> input) {
        ComponentVisitor componentVisitor = computeEngineContainer.getComponentByType(input);
        Preconditions.checkState(componentVisitor != null, String.format("Visitor not found: %s", input));
        return componentVisitor;
      }
    });
  }

}
