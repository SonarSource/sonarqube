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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MinimumFeedbackEdgeSetSolver {

  private final List<FeedbackCycle> feedbackCycles;
  private Set<FeedbackEdge> feedbackEdges;
  private int minimumFeedbackEdgesWeight = Integer.MAX_VALUE;
  private final int cyclesNumber;
  private final int maxNumberCyclesForSearchingMinimumFeedback;
  private static final int DEFAULT_MAXIMUM_NUMBER_OF_LOOPS = 1000000;
  private static final int MAXIMUM_NUMBER_OF_CYCLE_THAT_CAN_BE_HANDLED = 1500;
  private final int maximumNumberOfLoops;

  public int getNumberOfLoops() {
    return numberOfLoops;
  }

  private int numberOfLoops = 0;

  public MinimumFeedbackEdgeSetSolver(Set<Cycle> cycles, int maxCycles) {
    this(cycles, DEFAULT_MAXIMUM_NUMBER_OF_LOOPS, maxCycles);
  }

  public MinimumFeedbackEdgeSetSolver(Set<Cycle> cycles) {
    this(cycles, DEFAULT_MAXIMUM_NUMBER_OF_LOOPS, MAXIMUM_NUMBER_OF_CYCLE_THAT_CAN_BE_HANDLED);
  }

  public MinimumFeedbackEdgeSetSolver(Set<Cycle> cycles, int maximumNumberOfLoops, int maxNumberCyclesForSearchingMinimumFeedback) {
    this.maximumNumberOfLoops = maximumNumberOfLoops;
    this.feedbackCycles = FeedbackCycle.buildFeedbackCycles(cycles);
    this.cyclesNumber = cycles.size();
    this.maxNumberCyclesForSearchingMinimumFeedback = maxNumberCyclesForSearchingMinimumFeedback;
    this.run();
  }

  public int getWeightOfFeedbackEdgeSet() {
    return minimumFeedbackEdgesWeight;
  }

  /**
   * Get edges tagged as feedback.
   */
  public Set<Edge> getEdges() {
    Set<Edge> edges = new LinkedHashSet<Edge>();
    for (FeedbackEdge fe : feedbackEdges) {
      edges.add(fe.getEdge());
    }
    return edges;
  }

  private void run() {
    Set<FeedbackEdge> pendingFeedbackEdges = new LinkedHashSet<FeedbackEdge>();
    if (cyclesNumber < maxNumberCyclesForSearchingMinimumFeedback) {
      searchFeedbackEdges(0, 0, pendingFeedbackEdges);
    } else {
      lightResearchForFeedbackEdges();
    }
  }

  private void lightResearchForFeedbackEdges() {
    feedbackEdges = new LinkedHashSet<FeedbackEdge>();
    for (FeedbackCycle cycle : feedbackCycles) {
      for (FeedbackEdge edge : cycle) {
        feedbackEdges.add(edge);
        break;
      }
    }
    minimumFeedbackEdgesWeight = 0;
    for (FeedbackEdge edge : feedbackEdges) {
      minimumFeedbackEdgesWeight += edge.getWeight();
    }
  }

  private void searchFeedbackEdges(int level, int pendingWeight, Set<FeedbackEdge> pendingFeedbackEdges) {
    if (numberOfLoops++ > maximumNumberOfLoops) {
      return;
    }

    if (pendingWeight >= minimumFeedbackEdgesWeight) {
      return;
    }

    if (level == cyclesNumber) {
      minimumFeedbackEdgesWeight = pendingWeight;
      feedbackEdges = new LinkedHashSet<FeedbackEdge>(pendingFeedbackEdges);
      return;
    }

    FeedbackCycle feedbackCycle = feedbackCycles.get(level);

    if (doesFeedbackEdgesContainAnEdgeOfTheCycle(pendingFeedbackEdges, feedbackCycle)) {
      searchFeedbackEdges(level + 1, pendingWeight, pendingFeedbackEdges);
    } else {
      boolean hasAnEdgeWithOccurrenceOfOneBeenUsed = false;
      for (FeedbackEdge feedbackEdge : feedbackCycle) {
        if (feedbackEdge.getOccurences() == 1) {
          if (hasAnEdgeWithOccurrenceOfOneBeenUsed) {
            continue;
          } else {
            hasAnEdgeWithOccurrenceOfOneBeenUsed = true;
          }
        }
        int edgeWeight = addNewEdge(feedbackEdge, pendingFeedbackEdges);
        pendingWeight += edgeWeight;

        searchFeedbackEdges(level + 1, pendingWeight, pendingFeedbackEdges);
        pendingWeight -= edgeWeight;
        pendingFeedbackEdges.remove(feedbackEdge);
      }
    }
  }

  private boolean doesFeedbackEdgesContainAnEdgeOfTheCycle(Set<FeedbackEdge> pendingFeedbackEdges, FeedbackCycle cycle) {
    boolean contains = false;
    for (FeedbackEdge feedbackEdge : cycle) {
      if (pendingFeedbackEdges.contains(feedbackEdge)) {
        contains = true;
        break;
      }
    }
    return contains;
  }

  private int addNewEdge(FeedbackEdge feedbackEdge, Set<FeedbackEdge> pendingFeedbackEdges) {
    pendingFeedbackEdges.add(feedbackEdge);
    return feedbackEdge.getWeight();
  }
}
