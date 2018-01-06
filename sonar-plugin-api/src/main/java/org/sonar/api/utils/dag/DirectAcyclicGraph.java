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
package org.sonar.api.utils.dag;

import org.sonar.api.utils.SonarException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <a href="http://en.wikipedia.org/wiki/Directed_acyclic_graph">http://en.wikipedia.org/wiki/Directed_acyclic_graph</a>
 *
 * @since 1.10
 */
public class DirectAcyclicGraph {

  private Map<Object, Node> registeredObjects = new HashMap<>();
  private List<Node> nodes = new ArrayList<>();

  public DirectAcyclicGraph(Object... objects) {
    for (Object object : objects) {
      add(object);
    }
  }

  public Node add(Object object, Object... dependencies) {
    Node node = registeredObjects.get(object);
    if (node == null) {
      node = new Node(object);
      nodes.add(node);
      registeredObjects.put(object, node);
    }

    for (Object dependency : dependencies) {
      Node depNode = add(dependency);
      node.addDependency(depNode);
    }
    return node;
  }

  public List sort() {
    sortNodes();

    List<Object> result = new ArrayList<>();
    for (Node node : nodes) {
      result.add(node.getObject());
    }
    return result;
  }

  private List<Node> sortNodes() {
    verify();
    Collections.sort(nodes);
    return nodes;
  }

  private void verify() {
    for (Node node : nodes) {
      node.reset();
    }

    for (Node node : nodes) {
      for (Node dep : node.getDependencies()) {
        if (!nodes.contains(dep)) {
          throw new SonarException("A dependent node (" + dep + ") of "
              + " (" + node + ") was not included in the nodes list.");
        }
      }

      node.resolveOrder();
    }
  }
}
