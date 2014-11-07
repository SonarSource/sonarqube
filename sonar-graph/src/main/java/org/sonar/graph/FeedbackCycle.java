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

import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Note: this class has a natural ordering that is inconsistent with equals
 */
public final class FeedbackCycle implements Iterable<FeedbackEdge>, Comparable<FeedbackCycle> {

  private List<FeedbackEdge> orderedFeedbackEdges;
  private int totalOccurrencesOfEdgesInCycle;
  private final Cycle cycle;

  private FeedbackCycle(Cycle cycle) {
    orderedFeedbackEdges = new ArrayList<FeedbackEdge>();
    totalOccurrencesOfEdgesInCycle = 0;
    this.cycle = cycle;
  }

  private void add(FeedbackEdge feedbackEdge) {
    orderedFeedbackEdges.add(feedbackEdge);
  }

  public static List<FeedbackCycle> buildFeedbackCycles(Set<Cycle> cycles) {
    Multiset<Edge> edgesBag = createBagWithAllEdgesOfCycles(cycles);

    List<FeedbackCycle> feedbackCycles = new ArrayList<FeedbackCycle>();
    for (Cycle cycle : cycles) {
      FeedbackCycle feedbackCycle = new FeedbackCycle(cycle);
      int totalOccurrences = 0;
      for (Edge edge : cycle.getEdges()) {
        FeedbackEdge feedbackEdge = new FeedbackEdge(edge, edgesBag.count(edge));
        feedbackCycle.add(feedbackEdge);
        totalOccurrences += feedbackEdge.getOccurences();
      }
      feedbackCycle.setTotalOccurrencesOfEdgesInCycle(totalOccurrences);
      Collections.sort(feedbackCycle.orderedFeedbackEdges);
      feedbackCycles.add(feedbackCycle);
    }
    Collections.sort(feedbackCycles);

    return feedbackCycles;
  }

  private static Multiset<Edge> createBagWithAllEdgesOfCycles(Set<Cycle> cycles) {
    Multiset<Edge> edgesBag = LinkedHashMultiset.create();
    for (Cycle cycle : cycles) {
      for (Edge edge : cycle.getEdges()) {
        edgesBag.add(edge);
      }
    }
    return edgesBag;
  }

  private void setTotalOccurrencesOfEdgesInCycle(int totalOccurrencesOfEdgesInCycle) {
    this.totalOccurrencesOfEdgesInCycle = totalOccurrencesOfEdgesInCycle;
  }

  public int getTotalOccurrencesOfEdgesInCycle() {
    return totalOccurrencesOfEdgesInCycle;
  }

  @Override
  public Iterator<FeedbackEdge> iterator() {
    return orderedFeedbackEdges.iterator();
  }

  @Override
  public int compareTo(FeedbackCycle feedbackCycle) {
    if (getTotalOccurrencesOfEdgesInCycle() < feedbackCycle.getTotalOccurrencesOfEdgesInCycle()) {// NOSONAR this class has a natural
                                                                                                  // ordering that is inconsistent with
                                                                                                  // equals
      return -1;
    }
    if (getTotalOccurrencesOfEdgesInCycle() == feedbackCycle.getTotalOccurrencesOfEdgesInCycle()) {
      if (cycle.size() == feedbackCycle.cycle.size()) {
        return orderedFeedbackEdges.get(0).compareTo(feedbackCycle.orderedFeedbackEdges.get(0));
      }
      return cycle.size() - feedbackCycle.cycle.size();
    }
    return 1;
  }

  public Cycle getCycle() {
    return cycle;
  }
}
