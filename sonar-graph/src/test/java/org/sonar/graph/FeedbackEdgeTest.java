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

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

public class FeedbackEdgeTest {

  @Test
  public void testWeights() {
    FeedbackEdge fEdge = mockFeedbackEdge(14, 2);
    assertThat(fEdge.getWeight(), is(14));
    assertThat(fEdge.getRelativeWeight(), is(7.0));
    assertThat(fEdge.getOccurences(), is(2));
  }

  @Test
  public void testCompareTo() {
    FeedbackEdge feedbackEdge1;
    FeedbackEdge feedbackEdge2;
    FeedbackEdge feedbackEdge3;

    feedbackEdge1 = mockFeedbackEdge(14, 2);
    feedbackEdge2 = mockFeedbackEdge(10, 2);
    assertThat(feedbackEdge1.compareTo(feedbackEdge2), is(1));

    feedbackEdge1 = mockFeedbackEdge(10, 2);
    feedbackEdge2 = mockFeedbackEdge(14, 2);
    assertThat(feedbackEdge1.compareTo(feedbackEdge2), is(-1));

    feedbackEdge1 = mockFeedbackEdge(14, 2);
    feedbackEdge2 = mockFeedbackEdge(14, 2);
    assertThat(feedbackEdge1.compareTo(feedbackEdge2), is(0));

    feedbackEdge1 = mockFeedbackEdge(14, 2);
    feedbackEdge2 = mockFeedbackEdge(13, 2);
    assertThat(feedbackEdge1.compareTo(feedbackEdge2), is(1));

    feedbackEdge1 = mockFeedbackEdge(13, 2);
    feedbackEdge2 = mockFeedbackEdge(14, 2);
    assertThat(feedbackEdge1.compareTo(feedbackEdge2), is(-1));

    feedbackEdge1 = mockFeedbackEdge("A", "B", 14, 2);
    feedbackEdge2 = mockFeedbackEdge("B", "C", 14, 2);
    feedbackEdge3 = mockFeedbackEdge("C", "A", 14, 2);
    assertThat(feedbackEdge1.compareTo(feedbackEdge2), is(-1));
    assertThat(feedbackEdge2.compareTo(feedbackEdge3), is(-1));
    assertThat(feedbackEdge3.compareTo(feedbackEdge1), greaterThan(1));
  }

  private FeedbackEdge mockFeedbackEdge(int weight, int occurrences) {
    return mockFeedbackEdge("from", "to", weight, occurrences);
  }

  private FeedbackEdge mockFeedbackEdge(String from, String to, int weight, int occurrences) {
    Edge edge = new StringEdge(from, to, weight);
    return new FeedbackEdge(edge, occurrences);
  }

}
