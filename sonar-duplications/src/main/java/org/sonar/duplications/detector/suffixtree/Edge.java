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

public final class Edge {

  // can't be changed
  private int beginIndex; 

  private int endIndex;
  private Node startNode;

  // can't be changed, could be used as edge id
  private Node endNode;

  // each time edge is created, a new end node is created
  public Edge(int beginIndex, int endIndex, Node startNode) {
    this.beginIndex = beginIndex;
    this.endIndex = endIndex;
    this.startNode = startNode;
    this.endNode = new Node(startNode, null);
  }

  public Node splitEdge(Suffix suffix) {
    remove();
    Edge newEdge = new Edge(beginIndex, beginIndex + suffix.getSpan(), suffix.getOriginNode());
    newEdge.insert();
    newEdge.endNode.setSuffixNode(suffix.getOriginNode());
    beginIndex += suffix.getSpan() + 1;
    startNode = newEdge.getEndNode();
    insert();
    return newEdge.getEndNode();
  }

  public void insert() {
    startNode.addEdge(beginIndex, this);
  }

  public void remove() {
    startNode.removeEdge(beginIndex);
  }

  /**
   * @return length of this edge in symbols
   */
  public int getSpan() {
    return endIndex - beginIndex;
  }

  public int getBeginIndex() {
    return beginIndex;
  }

  public int getEndIndex() {
    return endIndex;
  }

  public Node getStartNode() {
    return startNode;
  }

  public Node getEndNode() {
    return endNode;
  }

}
