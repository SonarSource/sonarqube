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

public final class Suffix {

  private Node originNode;
  private int beginIndex;
  private int endIndex;

  public Suffix(Node originNode, int beginIndex, int endIndex) {
    this.originNode = originNode;
    this.beginIndex = beginIndex;
    this.endIndex = endIndex;
  }

  public boolean isExplicit() {
    return beginIndex > endIndex;
  }

  public boolean isImplicit() {
    return !isExplicit();
  }

  public void canonize() {
    if (isImplicit()) {
      Edge edge = originNode.findEdge(originNode.symbolAt(beginIndex));

      int edgeSpan = edge.getSpan();
      while (edgeSpan <= getSpan()) {
        beginIndex += edgeSpan + 1;
        originNode = edge.getEndNode();
        if (beginIndex <= endIndex) {
          edge = edge.getEndNode().findEdge(originNode.symbolAt(beginIndex));
          edgeSpan = edge.getSpan();
        }
      }
    }
  }

  public int getSpan() {
    return endIndex - beginIndex;
  }

  public Node getOriginNode() {
    return originNode;
  }

  public int getBeginIndex() {
    return beginIndex;
  }

  public void incBeginIndex() {
    beginIndex++;
  }

  public void changeOriginNode() {
    originNode = originNode.getSuffixNode();
  }

  public int getEndIndex() {
    return endIndex;
  }

  public void incEndIndex() {
    endIndex++;
  }

}
