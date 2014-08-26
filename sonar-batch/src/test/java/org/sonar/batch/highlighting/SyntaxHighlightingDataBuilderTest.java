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
package org.sonar.batch.highlighting;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Collection;

import static org.fest.assertions.Assertions.assertThat;
import static org.sonar.api.batch.sensor.highlighting.HighlightingBuilder.TypeOfText.CLASSIC_COMMENT;
import static org.sonar.api.batch.sensor.highlighting.HighlightingBuilder.TypeOfText.CPP_DOC;
import static org.sonar.api.batch.sensor.highlighting.HighlightingBuilder.TypeOfText.KEYWORD;

public class SyntaxHighlightingDataBuilderTest {

  private Collection<SyntaxHighlightingRule> highlightingRules;

  @Rule
  public ExpectedException throwable = ExpectedException.none();

  @Before
  public void setUpSampleRules() {

    SyntaxHighlightingDataBuilder highlightingDataBuilder = new SyntaxHighlightingDataBuilder();
    highlightingDataBuilder.registerHighlightingRule(0, 10, CLASSIC_COMMENT);
    highlightingDataBuilder.registerHighlightingRule(10, 12, KEYWORD);
    highlightingDataBuilder.registerHighlightingRule(24, 38, KEYWORD);
    highlightingDataBuilder.registerHighlightingRule(42, 50, KEYWORD);
    highlightingDataBuilder.registerHighlightingRule(24, 65, CPP_DOC);
    highlightingDataBuilder.registerHighlightingRule(12, 20, CLASSIC_COMMENT);

    highlightingRules = highlightingDataBuilder.getSyntaxHighlightingRuleSet();
  }

  @Test
  public void should_register_highlighting_rule() throws Exception {
    assertThat(highlightingRules).hasSize(6);
  }

  @Test
  public void should_order_by_start_then_end_offset() throws Exception {
    assertThat(highlightingRules).onProperty("startPosition").containsOnly(0, 10, 12, 24, 24, 42);
    assertThat(highlightingRules).onProperty("endPosition").containsOnly(10, 12, 20, 38, 65, 50);
    assertThat(highlightingRules).onProperty("textType").containsOnly(CLASSIC_COMMENT, KEYWORD, CLASSIC_COMMENT, KEYWORD, CPP_DOC, KEYWORD);
  }

  @Test
  public void should_suport_overlapping() throws Exception {
    SyntaxHighlightingDataBuilder builder = new SyntaxHighlightingDataBuilder();
    builder.registerHighlightingRule(0, 15, KEYWORD);
    builder.registerHighlightingRule(8, 12, CPP_DOC);
    builder.build();
  }

  @Test
  public void should_prevent_boudaries_overlapping() throws Exception {
    throwable.expect(IllegalStateException.class);
    throwable.expectMessage("Cannot register highlighting rule for characters from 8 to 15 as it overlaps at least one existing rule");

    SyntaxHighlightingDataBuilder builder = new SyntaxHighlightingDataBuilder();
    builder.registerHighlightingRule(0, 10, KEYWORD);
    builder.registerHighlightingRule(8, 15, KEYWORD);
    builder.build();
  }
}
