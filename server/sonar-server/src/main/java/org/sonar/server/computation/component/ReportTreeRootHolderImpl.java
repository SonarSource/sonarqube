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
package org.sonar.server.computation.component;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.server.computation.component.ComponentVisitor.Order.POST_ORDER;

public class ReportTreeRootHolderImpl extends TreeRootHolderImpl implements ReportTreeRootHolder {
  private Map<Integer, Component> componentsByRef = new HashMap<>();

  @Override
  public MutableTreeRootHolder setRoot(Component newRoot) {
    super.setRoot(newRoot);
    feedComponentsByRef(newRoot);
    return this;
  }

  private void feedComponentsByRef(Component newRoot) {
    new DepthTraversalTypeAwareCrawler(
      new TypeAwareVisitorAdapter(CrawlerDepthLimit.FILE, POST_ORDER) {
        @Override
        public void visitAny(Component component) {
          componentsByRef.put(component.getReportAttributes().getRef(), component);
        }
      }).visit(newRoot);
  }

  @Override
  public Component getComponentByRef(int ref) {
    // makes sure the root is set, hence componentsByRef is populated
    getRoot();
    Component component = componentsByRef.get(ref);
    checkArgument(component != null, String.format("Component '%s' hasn't been found", ref));
    return component;
  }
}
