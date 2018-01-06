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
package org.sonar.duplications.detector.suffixtree;

import java.util.Map;
import java.util.HashMap;
import java.util.Collection;

public final class Node {

  private final SuffixTree suffixTree;
  private final Map<Object, Edge> edges;

  /**
   * Node represents string s[i],s[i+1],...,s[j],
   * suffix-link is a link to node, which represents string s[i+1],...,s[j].
   */
  private Node suffixNode;

  /**
   * Number of symbols from the root to this node.
   * <p>
   * Note that this is not equal to number of nodes from root to this node,
   * because in a compact suffix-tree edge can span multiple symbols - see {@link Edge#getSpan()}.
   * </p><p>
   * Depth of {@link #suffixNode} is always equal to this depth minus one.
   * </p> 
   */
  int depth;

  int startSize;
  int endSize;

  public Node(Node node, Node suffixNode) {
    this(node.suffixTree, suffixNode);
  }

  public Node(SuffixTree suffixTree, Node suffixNode) {
    this.suffixTree = suffixTree;
    this.suffixNode = suffixNode;
    edges = new HashMap<>();
  }

  public Object symbolAt(int index) {
    return suffixTree.symbolAt(index);
  }

  public void addEdge(int charIndex, Edge edge) {
    edges.put(symbolAt(charIndex), edge);
  }

  public void removeEdge(int charIndex) {
    edges.remove(symbolAt(charIndex));
  }

  public Edge findEdge(Object ch) {
    return edges.get(ch);
  }

  public Node getSuffixNode() {
    return suffixNode;
  }

  public void setSuffixNode(Node suffixNode) {
    this.suffixNode = suffixNode;
  }

  public Collection<Edge> getEdges() {
    return edges.values();
  }

}
