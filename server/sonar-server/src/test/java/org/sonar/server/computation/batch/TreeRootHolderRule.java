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
package org.sonar.server.computation.batch;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.CrawlerDepthLimit;
import org.sonar.server.computation.component.DepthTraversalTypeAwareCrawler;
import org.sonar.server.computation.component.MutableTreeRootHolder;
import org.sonar.server.computation.component.ReportTreeRootHolder;
import org.sonar.server.computation.component.TreeRootHolder;
import org.sonar.server.computation.component.TypeAwareVisitorAdapter;

import static org.sonar.server.computation.component.ComponentVisitor.Order.POST_ORDER;

public class TreeRootHolderRule implements TestRule, MutableTreeRootHolder, ReportTreeRootHolder {
  private Component root;
  private Map<Integer, Component> componentsByRef = new HashMap<>();

  @Override
  public Statement apply(final Statement statement, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        try {
          statement.evaluate();
        } finally {
          clear();
        }
      }
    };
  }

  private void clear() {
    this.root = null;
  }

  @Override
  public Component getRoot() {
    if (root == null) {
      throw new IllegalStateException("Root has not been set in " + TreeRootHolder.class.getSimpleName());
    }
    return root;
  }

  @Override
  public Component getComponentByRef(int ref) {
    if (root == null) {
      throw new IllegalStateException("Root has not been set in " + TreeRootHolder.class.getSimpleName());
    }

    Component component = componentsByRef.get(ref);
    if (component == null) {
      throw new IllegalArgumentException(String.format("Component '%s' hasn't been found", ref));
    }
    return component;
  }

  public TreeRootHolderRule setRoot(Component newRoot) {
    this.root = Objects.requireNonNull(newRoot);
    if (newRoot.getType().isReportType()) {
      new DepthTraversalTypeAwareCrawler(new TypeAwareVisitorAdapter(CrawlerDepthLimit.FILE, POST_ORDER) {
        @Override
        public void visitAny(Component component) {
          componentsByRef.put(component.getReportAttributes().getRef(), component);
        }
      }).visit(root);
    }
    return this;
  }
}
