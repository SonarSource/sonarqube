/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.api.batch.sensor.highlighting.internal;

import java.util.Collection;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.DefaultTextPointer;
import org.sonar.api.batch.fs.internal.DefaultTextRange;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.SensorStorage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.api.batch.sensor.highlighting.TypeOfText.COMMENT;
import static org.sonar.api.batch.sensor.highlighting.TypeOfText.CPP_DOC;
import static org.sonar.api.batch.sensor.highlighting.TypeOfText.KEYWORD;

public class DefaultHighlightingTest {

  private static final InputFile INPUT_FILE = new TestInputFileBuilder("foo", "src/Foo.java")
    .setLines(2)
    .setOriginalLineStartOffsets(new int[] {0, 50})
    .setOriginalLineEndOffsets(new int[] {49, 100})
    .setLastValidOffset(101)
    .build();

  private Collection<SyntaxHighlightingRule> highlightingRules;

  @Rule
  public ExpectedException throwable = ExpectedException.none();

  @Before
  public void setUpSampleRules() {

    DefaultHighlighting highlightingDataBuilder = new DefaultHighlighting(mock(SensorStorage.class))
      .onFile(INPUT_FILE)
      .highlight(0, 10, COMMENT)
      .highlight(1, 10, 1, 12, KEYWORD)
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
    assertThat(highlightingRules).extracting("range", TextRange.class).containsExactly(
      rangeOf(1, 0, 1, 10),
      rangeOf(1, 10, 1, 12),
      rangeOf(1, 12, 1, 20),
      rangeOf(1, 24, 2, 15),
      rangeOf(1, 24, 1, 38),
      rangeOf(1, 42, 2, 0));
    assertThat(highlightingRules).extracting("textType").containsExactly(COMMENT, KEYWORD, COMMENT, CPP_DOC, KEYWORD, KEYWORD);
  }

  @Test
  public void should_support_overlapping() {
    new DefaultHighlighting(mock(SensorStorage.class))
      .onFile(INPUT_FILE)
      .highlight(0, 15, KEYWORD)
      .highlight(8, 12, CPP_DOC)
      .save();
  }

  @Test
  public void should_prevent_start_equal_end() {
    throwable.expect(IllegalArgumentException.class);
    throwable
      .expectMessage("Unable to highlight file");

    new DefaultHighlighting(mock(SensorStorage.class))
      .onFile(INPUT_FILE)
      .highlight(10, 10, KEYWORD)
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
