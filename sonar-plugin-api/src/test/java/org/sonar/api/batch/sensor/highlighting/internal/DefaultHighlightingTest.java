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
package org.sonar.api.batch.sensor.highlighting.internal;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DefaultTextPointer;
import org.sonar.api.batch.fs.internal.DefaultTextRange;
import org.sonar.api.batch.sensor.internal.SensorStorage;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.api.batch.sensor.highlighting.TypeOfText.COMMENT;
import static org.sonar.api.batch.sensor.highlighting.TypeOfText.CPP_DOC;
import static org.sonar.api.batch.sensor.highlighting.TypeOfText.KEYWORD;

public class DefaultHighlightingTest {

  private static final DefaultInputFile INPUT_FILE = new DefaultInputFile("foo", "src/Foo.java")
    .setLines(2)
    .setOriginalLineOffsets(new int[] {0, 50})
    .setLastValidOffset(100);

  private Collection<SyntaxHighlightingRule> highlightingRules;

  @Rule
  public ExpectedException throwable = ExpectedException.none();

  @Before
  public void setUpSampleRules() {

    DefaultHighlighting highlightingDataBuilder = new DefaultHighlighting(mock(SensorStorage.class))
      .onFile(INPUT_FILE)
      .highlight(0, 10, COMMENT)
      .highlight(10, 12, KEYWORD)
      .highlight(24, 38, KEYWORD)
      .highlight(42, 50, KEYWORD)
      .highlight(24, 65, CPP_DOC)
      .highlight(12, 20, COMMENT);

    highlightingDataBuilder.save();

    highlightingRules = highlightingDataBuilder.getSyntaxHighlightingRuleSet();
  }

  @Test
  public void should_register_highlighting_rule() {
    assertThat(highlightingRules).hasSize(6);
  }

  private static TextRange rangeOf(int startLine, int startOffset, int endLine, int endOffset) {
    return new DefaultTextRange(new DefaultTextPointer(startLine, startOffset), new DefaultTextPointer(endLine, endOffset));
  }

  @Test
  public void should_order_by_start_then_end_offset() {
    assertThat(highlightingRules).extracting("range", TextRange.class).containsExactly(rangeOf(1, 0, 1, 10),
      rangeOf(1, 10, 1, 12),
      rangeOf(1, 12, 1, 20),
      rangeOf(1, 24, 2, 15),
      rangeOf(1, 24, 1, 38),
      rangeOf(1, 42, 2, 0));
    assertThat(highlightingRules).extracting("textType").containsOnly(COMMENT, KEYWORD, COMMENT, KEYWORD, CPP_DOC, KEYWORD);
  }

  @Test
  public void should_suport_overlapping() {
    new DefaultHighlighting(mock(SensorStorage.class))
      .onFile(INPUT_FILE)
      .highlight(0, 15, KEYWORD)
      .highlight(8, 12, CPP_DOC)
      .save();
  }

  @Test
  public void should_prevent_boudaries_overlapping() {
    throwable.expect(IllegalStateException.class);
    throwable
      .expectMessage("Cannot register highlighting rule for characters at Range[from [line=1, lineOffset=8] to [line=1, lineOffset=15]] as it overlaps at least one existing rule");

    new DefaultHighlighting(mock(SensorStorage.class))
      .onFile(INPUT_FILE)
      .highlight(0, 10, KEYWORD)
      .highlight(8, 15, KEYWORD)
      .save();
  }

}
