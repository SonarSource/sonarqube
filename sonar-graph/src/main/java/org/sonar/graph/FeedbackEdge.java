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
package org.sonar.graph;

import org.apache.commons.lang.math.NumberUtils;

public class FeedbackEdge implements Comparable<FeedbackEdge> {

  private Edge edge;
  private double relativeWeight;
  private int occurences;
  private final int hashcode;

  public FeedbackEdge(Edge edge, int occurences) {
    this.edge = edge;
    this.hashcode = edge.hashCode();
    this.occurences = occurences;
    this.relativeWeight = (double) edge.getWeight() / occurences;
  }

  protected Edge getEdge() {
    return edge;
  }

  protected int getWeight() {
    return edge.getWeight();
  }

  protected double getRelativeWeight() {
    return relativeWeight;
  }

  protected int getOccurences() {
    return occurences;
  }

  @Override
  public int compareTo(FeedbackEdge feedbackEdge) {
    if (this.getRelativeWeight() < feedbackEdge.getRelativeWeight()) {
      return -1;
    }
    if (NumberUtils.compare(this.getRelativeWeight(), feedbackEdge.getRelativeWeight())==0) {
      return this.getEdge().getFrom().toString().compareTo(feedbackEdge.getEdge().getFrom().toString());
    }
    return 1;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof FeedbackEdge) || this.hashCode() != obj.hashCode()) {
      return false;
    }
    FeedbackEdge otherEdge = (FeedbackEdge) obj;
    return edge.equals(otherEdge.edge);
  }

  @Override
  public int hashCode() {
    return hashcode;
  }
}
