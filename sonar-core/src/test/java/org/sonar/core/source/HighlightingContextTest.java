/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.core.source;

import com.google.common.collect.Multimap;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class HighlightingContextTest {

  private static final String SAMPLE_RULES = "0,8,k;0,52,cppd;54,67,a;69,75,k;106,130,cppd;114,130,k;";
  private HighlightingContext highlightingContext;

  @Before
  public void setUpHighlightingContext() {
    highlightingContext = HighlightingContext.buildFrom(SAMPLE_RULES);
  }

  @Test
  public void should_extract_lower_bounds_from_serialized_rules() throws Exception {

      Multimap<Integer, String> lowerBoundsDefinitions = highlightingContext.getLowerBoundsDefinitions();

      assertThat(lowerBoundsDefinitions.containsEntry(0, "k"));
      assertThat(lowerBoundsDefinitions.containsEntry(0, "cppd"));
      assertThat(lowerBoundsDefinitions.containsEntry(54, "a"));
      assertThat(lowerBoundsDefinitions.containsEntry(69, "k"));
      assertThat(lowerBoundsDefinitions.containsEntry(106, "cppd"));
      assertThat(lowerBoundsDefinitions.containsEntry(114, "k"));
  }

  @Test
  public void should_extract_upper_bounds_from_serialized_rules() throws Exception {

    List<Integer> upperBoundsDefinition = highlightingContext.getUpperBoundsDefinitions();
    assertThat(upperBoundsDefinition).containsExactly(8, 52, 67, 75, 130, 130);
  }
}
