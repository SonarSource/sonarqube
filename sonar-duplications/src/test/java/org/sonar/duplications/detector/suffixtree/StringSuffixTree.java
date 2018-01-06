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

import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;

public class StringSuffixTree {

  private final SuffixTree suffixTree;
  private int numberOfEdges;
  private int numberOfInnerNodes;
  private int numberOfLeaves;

  public static StringSuffixTree create(String text) {
    return new StringSuffixTree(text);
  }

  private StringSuffixTree(String text) {
    suffixTree = SuffixTree.create(new StringText(text));

    Queue<Node> queue = new LinkedList<>();
    queue.add(suffixTree.getRootNode());
    while (!queue.isEmpty()) {
      Node node = queue.remove();
      if (node.getEdges().isEmpty()) {
        numberOfLeaves++;
      } else {
        numberOfInnerNodes++;
        for (Edge edge : node.getEdges()) {
          numberOfEdges++;
          queue.add(edge.getEndNode());
        }
      }
    }
    numberOfInnerNodes--; // without root
  }

  public int getNumberOfEdges() {
    return numberOfEdges;
  }

  public int getNumberOfInnerNodes() {
    return numberOfInnerNodes;
  }

  // FIXME should be renamed getNumberOfLeaves()
  public int getNumberOfLeafs() {
    return numberOfLeaves;
  }

  public int indexOf(String str) {
    return indexOf(suffixTree, new StringText(str));
  }

  public boolean contains(String str) {
    return contains(suffixTree, new StringText(str));
  }

  public SuffixTree getSuffixTree() {
    return suffixTree;
  }

  public static boolean contains(SuffixTree tree, Text str) {
    return indexOf(tree, str) >= 0;
  }

  public static int indexOf(SuffixTree tree, Text str) {
    if (str.length() == 0) {
      return -1;
    }

    int index = -1;
    Node node = tree.getRootNode();

    int i = 0;
    while (i < str.length()) {
      if (node == null) {
        return -1;
      }
      if (i == tree.text.length()) {
        return -1;
      }

      Edge edge = node.findEdge(str.symbolAt(i));
      if (edge == null) {
        return -1;
      }

      index = edge.getBeginIndex() - i;
      i++;

      for (int j = edge.getBeginIndex() + 1; j <= edge.getEndIndex(); j++) {
        if (i == str.length()) {
          break;
        }
        if (!Objects.equals(tree.symbolAt(j), str.symbolAt(i))) {
          return -1;
        }
        i++;
      }
      node = edge.getEndNode();
    }
    return index;
  }

}
