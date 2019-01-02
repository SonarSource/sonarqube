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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public final class Search {

  private final SuffixTree tree;
  private final TextSet text;
  private final Collector reporter;

  private final List<Integer> list = new ArrayList<>();
  private final List<Node> innerNodes = new ArrayList<>();
  
  private static final Comparator<Node> DEPTH_COMPARATOR = (o1, o2) -> o2.depth - o1.depth;

  private Search(SuffixTree tree, TextSet text, Collector reporter) {
    this.tree = tree;
    this.text = text;
    this.reporter = reporter;
  }
  
  public static void perform(TextSet text, Collector reporter) {
    new Search(SuffixTree.create(text), text, reporter).compute();
  }

  private void compute() {
    // O(N)
    dfs();

    // O(N * log(N))
    Collections.sort(innerNodes, DEPTH_COMPARATOR);

    // O(N)
    visitInnerNodes();
  }

  /**
   * Depth-first search (DFS).
   */
  private void dfs() {
    Deque<Node> stack = new LinkedList<>();
    stack.add(tree.getRootNode());
    while (!stack.isEmpty()) {
      Node node = stack.removeLast();
      node.startSize = list.size();
      if (node.getEdges().isEmpty()) {
        // leaf
        list.add(node.depth);
        node.endSize = list.size();
      } else {
        if (!node.equals(tree.getRootNode())) {
          // inner node = not leaf and not root
          innerNodes.add(node);
        }
        for (Edge edge : node.getEdges()) {
          Node endNode = edge.getEndNode();
          endNode.depth = node.depth + edge.getSpan() + 1;
          stack.addLast(endNode);
        }
      }
    }
    // At this point all inner nodes are ordered by the time of entering, so we visit them from last to first
    ListIterator<Node> iterator = innerNodes.listIterator(innerNodes.size());
    while (iterator.hasPrevious()) {
      Node node = iterator.previous();
      int max = -1;
      for (Edge edge : node.getEdges()) {
        max = Math.max(edge.getEndNode().endSize, max);
      }
      node.endSize = max;
    }
  }

  /**
   * Each inner-node represents prefix of some suffixes, thus substring of text.
   */
  private void visitInnerNodes() {
    for (Node node : innerNodes) {
      if (containsOrigin(node)) {
        report(node);
      }
    }
  }

  /**
   * TODO Godin: in fact computations here are the same as in {@link #report(Node)},
   * so maybe would be better to remove this duplication,
   * however it should be noted that this check can't be done in {@link Collector#endOfGroup()},
   * because it might lead to creation of unnecessary new objects
   */
  private boolean containsOrigin(Node node) {
    for (int i = node.startSize; i < node.endSize; i++) {
      int start = tree.text.length() - list.get(i);
      int end = start + node.depth;
      if (text.isInsideOrigin(end)) {
        return true;
      }
    }
    return false;
  }

  private void report(Node node) {
    reporter.startOfGroup(node.endSize - node.startSize, node.depth);
    for (int i = node.startSize; i < node.endSize; i++) {
      int start = tree.text.length() - list.get(i);
      int end = start + node.depth;
      reporter.part(start, end);
    }
    reporter.endOfGroup();
  }

  public abstract static class Collector {

    /**
     * Invoked at the beginning of processing for current node.
     * <p>
     * Length - is a depth of node. And nodes are visited in descending order of depth,
     * thus we guaranty that length will not increase between two sequential calls of this method
     * (can be equal or less than previous value).
     * </p>
     *
     * @param size number of parts in group
     * @param length length of each part in group
     */
    abstract void startOfGroup(int size, int length);

    /**
     * Invoked as many times as leaves in the subtree, where current node is root.
     *
     * @param start start position in generalised text
     * @param end end position in generalised text
     */
    abstract void part(int start, int end);

    /**
     * Invoked at the end of processing for current node.
     */
    abstract void endOfGroup();

  }

}
