/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.computation.task.projectanalysis.component;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkState;

public final class TreeComponentProvider extends AbstractComponentProvider {
  private final Component root;
  private final Map<String, Component> componentsByRef = new HashMap<>();

  public TreeComponentProvider(Component root) {
    this.root = root;
    ensureInitialized();
  }

  private static String getRef(Component component) {
    return component.getType().isReportType() ? String.valueOf(component.getReportAttributes().getRef()) : component.getKey();
  }

  @Override
  protected void ensureInitializedImpl() {
    new DepthTraversalTypeAwareCrawler(
      new TypeAwareVisitorAdapter(CrawlerDepthLimit.LEAVES, ComponentVisitor.Order.PRE_ORDER) {
        @Override
        public void visitAny(Component component) {
          String ref = getRef(component);
          checkState(!componentsByRef.containsKey(ref), "Tree contains more than one component with ref " + ref);
          componentsByRef.put(ref, component);
        }
      }).visit(root);
  }

  @Override
  protected void resetImpl() {
    // we can not reset
  }

  @Override
  protected Component getByRefImpl(int componentRef) {
    Component component = componentsByRef.get(String.valueOf(componentRef));
    checkState(component != null, "Can not find Component for ref " + componentRef);
    return component;
  }
}
