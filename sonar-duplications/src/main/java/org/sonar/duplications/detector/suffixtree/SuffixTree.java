/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.duplications.detector.suffixtree;

import java.util.Objects;

/**
 * Provides algorithm to construct suffix tree.
 * <p>
 * Suffix tree for the string S of length n is defined as a tree such that:
 * <ul>
 * <li>the paths from the root to the leaves have a one-to-one relationship with the suffixes of S,</li>
 * <li>edges spell non-empty strings,</li>
 * <li>and all internal nodes (except perhaps the root) have at least two children.</li>
 * </ul>
 * Since such a tree does not exist for all strings, S is padded with a terminal symbol not seen in the string (usually denoted $).
 * This ensures that no suffix is a prefix of another, and that there will be n leaf nodes, one for each of the n suffixes of S.
 * Since all internal non-root nodes are branching, there can be at most n −  1 such nodes, and n + (n − 1) + 1 = 2n nodes in total.
 * All internal nodes and leaves have incoming edge, so number of edges equal to number of leaves plus number of inner nodes,
 * thus at most 2n - 1.
 * Construction takes O(n) time.
 * </p><p>
 * This implementation was adapted from <a href="http://illya-keeplearning.blogspot.com/search/label/suffix%20tree">Java-port</a> of
 * <a href="http://marknelson.us/1996/08/01/suffix-trees/">Mark Nelson's C++ implementation of Ukkonen's algorithm</a>.
 * </p>
 */
public final class SuffixTree {

  final Text text;

  private final Node root;
  
  private SuffixTree(Text text) {
    this.text = text;
    root = new Node(this, null);
  }
  
  public static SuffixTree create(Text text) {
    SuffixTree tree = new SuffixTree(text);
    Suffix active = new Suffix(tree.root, 0, -1);
    for (int i = 0; i < text.length(); i++) {
      tree.addPrefix(active, i);
    }
    return tree;
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
        if (Objects.equals(symbolAt(edge.getBeginIndex() + span + 1), symbolAt(endIndex))) {
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
    active.incEndIndex();
    // Now the endpoint is the next active point
    active.canonize();
  }

  private void updateSuffixNode(Node node, Node suffixNode) {
    if ((node != null) && (!node.equals(root))) {
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
