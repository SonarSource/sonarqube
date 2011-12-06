/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.duplications.detector.suffixtree;

import com.google.common.base.Objects;

/**
 * The implementation of the algorithm for constructing suffix-tree based on
 * <a href="http://illya-keeplearning.blogspot.com/search/label/suffix%20tree">Java-port</a> of
 * <a href="http://marknelson.us/1996/08/01/suffix-trees/">Mark Nelson's C++ implementation of Ukkonen's algorithm</a>.
 */
public final class SuffixTree {

  final Text text;

  private final Node root;

  public static SuffixTree create(Text text) {
    SuffixTree tree = new SuffixTree(text);
    Suffix active = new Suffix(tree.root, 0, -1);
    for (int i = 0; i < text.length(); i++) {
      tree.addPrefix(active, i);
    }
    return tree;
  }

  private SuffixTree(Text text) {
    this.text = text;
    root = new Node(this, null);
  }

  private void addPrefix(Suffix active, int endIndex) {
    Node lastParentNode = null;
    Node parentNode;

    while (true) {
      Edge edge;
      parentNode = active.getOriginNode();

      // Step 1 is to try and find a matching edge for the given node.
      // If a matching edge exists, we are done adding edges, so we break out of this big loop.
      if (active.isExplicit()) {
        edge = active.getOriginNode().findEdge(symbolAt(endIndex));
        if (edge != null) {
          break;
        }
      } else {
        // implicit node, a little more complicated
        edge = active.getOriginNode().findEdge(symbolAt(active.getBeginIndex()));
        int span = active.getSpan();
        if (Objects.equal(symbolAt(edge.getBeginIndex() + span + 1), symbolAt(endIndex))) {
          break;
        }
        parentNode = edge.splitEdge(active);
      }

      // We didn't find a matching edge, so we create a new one, add it to the tree at the parent node position,
      // and insert it into the hash table. When we create a new node, it also means we need to create
      // a suffix link to the new node from the last node we visited.
      Edge newEdge = new Edge(endIndex, text.length() - 1, parentNode);
      newEdge.insert();
      updateSuffixNode(lastParentNode, parentNode);
      lastParentNode = parentNode;

      // This final step is where we move to the next smaller suffix
      if (active.getOriginNode() == root) {
        active.incBeginIndex();
      } else {
        active.changeOriginNode();
      }
      active.canonize();
    }
    updateSuffixNode(lastParentNode, parentNode);
    active.incEndIndex(); // Now the endpoint is the next active point
    active.canonize();
  }

  private void updateSuffixNode(Node node, Node suffixNode) {
    if ((node != null) && (node != root)) {
      node.setSuffixNode(suffixNode);
    }
  }

  public Object symbolAt(int index) {
    return text.symbolAt(index);
  }

  public Node getRootNode() {
    return root;
  }

}
