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

import org.junit.Before;
import org.junit.Test;

import java.util.Deque;

import static org.fest.assertions.Assertions.assertThat;

public class DecorationDataHolderTest {

  private static final String SAMPLE_SYNTAX_HIGHLIGHTING_RULES = "0,8,k;0,52,cppd;54,67,a;69,75,k;106,130,cppd;114,130,k;";
  private static final String SAMPLE_SYMBOLS_REFERENCES = "80,85,80,90,140;";

  private DecorationDataHolder decorationDataHolder;

  @Before
  public void setUpHighlightingContext() {
    decorationDataHolder = new DecorationDataHolder();
    decorationDataHolder.loadSyntaxHighlightingData(SAMPLE_SYNTAX_HIGHLIGHTING_RULES);
    decorationDataHolder.loadSymbolReferences(SAMPLE_SYMBOLS_REFERENCES);
  }

  @Test
  public void should_extract_lower_bounds_from_serialized_rules() throws Exception {

    Deque<TagEntry> tagEntries = decorationDataHolder.getTagEntriesStack();

    assertThat(tagEntries.pop()).isEqualTo(new TagEntry(0, "k"));
    assertThat(tagEntries.pop()).isEqualTo(new TagEntry(0, "cppd"));
    assertThat(tagEntries.pop()).isEqualTo(new TagEntry(54, "a"));
    assertThat(tagEntries.pop()).isEqualTo(new TagEntry(69, "k"));
    assertThat(tagEntries.pop()).isEqualTo(new TagEntry(80, "symbol-80 highlightable"));
    assertThat(tagEntries.pop()).isEqualTo(new TagEntry(90, "symbol-80 highlightable"));
    assertThat(tagEntries.pop()).isEqualTo(new TagEntry(106, "cppd"));
    assertThat(tagEntries.pop()).isEqualTo(new TagEntry(114, "k"));
    assertThat(tagEntries.pop()).isEqualTo(new TagEntry(140, "symbol-80 highlightable"));
  }

  @Test
  public void should_extract_upper_bounds_from_serialized_rules() throws Exception {

    Deque<Integer> upperBoundsDefinition = decorationDataHolder.getClosingTagsStack();

    assertThat(upperBoundsDefinition.pop()).isEqualTo(8);
    assertThat(upperBoundsDefinition.pop()).isEqualTo(52);
    assertThat(upperBoundsDefinition.pop()).isEqualTo(67);
    assertThat(upperBoundsDefinition.pop()).isEqualTo(75);
    assertThat(upperBoundsDefinition.pop()).isEqualTo(85);
    assertThat(upperBoundsDefinition.pop()).isEqualTo(95);
    assertThat(upperBoundsDefinition.pop()).isEqualTo(130);
    assertThat(upperBoundsDefinition.pop()).isEqualTo(130);
    assertThat(upperBoundsDefinition.pop()).isEqualTo(145);
  }
}
