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

import java.util.List;
import java.util.Map;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.server.computation.component.ComponentVisitor;
import org.sonar.server.computation.component.TreeRootHolder;
import org.sonar.server.computation.component.VisitorsCrawler;

public class ExecuteVisitorsStep implements ComputationStep {

  private static final Logger LOGGER = Loggers.get(ExecuteVisitorsStep.class);

  private final TreeRootHolder treeRootHolder;
  private final List<ComponentVisitor> visitors;

  public ExecuteVisitorsStep(TreeRootHolder treeRootHolder, List<ComponentVisitor> visitors) {
    this.treeRootHolder = treeRootHolder;
    this.visitors = visitors;
  }

  @Override
  public String getDescription() {
    return "Execute Component Visitors";
  }

  @Override
  public void execute() {
    VisitorsCrawler visitorsCrawler = new VisitorsCrawler(visitors);
    visitorsCrawler.visit(treeRootHolder.getRoot());
    logVisitorExecutionDurations(visitors, visitorsCrawler);
  }

  private static void logVisitorExecutionDurations(List<ComponentVisitor> visitors, VisitorsCrawler visitorsCrawler) {
    LOGGER.info("  Execution time for each Component visitor:");
    Map<ComponentVisitor, Long> cumulativeDurations = visitorsCrawler.getCumulativeDurations();
    for (ComponentVisitor visitor : visitors) {
      LOGGER.info("  - {} | time={}ms", visitor.getClass().getSimpleName(), cumulativeDurations.get(visitor));
    }
  }
}
