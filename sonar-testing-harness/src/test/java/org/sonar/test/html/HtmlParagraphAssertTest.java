/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.test.html;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.test.html.HtmlFragmentAssert.assertThat;

public class HtmlParagraphAssertTest {

  @Test
  public void withText_matchingText_passes() {
    assertThat("<p>Hello World</p>")
      .hasParagraph()
      .withText("Hello World");
  }

  @Test
  public void withText_nonMatchingText_throwsAssertionError() {
    assertThatThrownBy(() -> 
      assertThat("<p>Hello World</p>")
        .hasParagraph()
        .withText("Different text"))
      .isInstanceOf(AssertionError.class);
  }

  @Test
  public void withLines_matchingLines_passes() {
    assertThat("<p>Line 1<br>Line 2<br>Line 3</p>")
      .hasParagraph()
      .withLines("Line 1", "Line 2", "Line 3");
  }

  @Test
  public void withLines_nonMatchingLines_throwsAssertionError() {
    assertThatThrownBy(() -> 
      assertThat("<p>Line 1<br>Line 2</p>")
        .hasParagraph()
        .withLines("Line 1", "Line 2", "Line 3"))
      .isInstanceOf(AssertionError.class);
  }

  @Test
  public void hasParagraph_chainedParagraphs_passes() {
    assertThat("<p>First</p><p>Second</p><p>Third</p>")
      .hasParagraph()
      .withText("First")
      .hasParagraph()
      .withText("Second")
      .hasParagraph()
      .withText("Third");
  }

  @Test
  public void hasParagraph_withText_convenience() {
    assertThat("<p>First</p><p>Second</p>")
      .hasParagraph()
      .hasParagraph("Second");
  }

  @Test
  public void hasEmptyParagraph_withEmptyParagraph_passes() {
    assertThat("<p>First</p><p></p>")
      .hasParagraph()
      .hasEmptyParagraph();
  }

  @Test
  public void hasEmptyParagraph_withNbsp_passes() {
    assertThat("<p>First</p><p>\u00A0</p>")
      .hasParagraph()
      .hasEmptyParagraph();
  }

  @Test
  public void hasEmptyParagraph_withNonEmptyParagraph_throwsAssertionError() {
    assertThatThrownBy(() -> 
      assertThat("<p>First</p><p>Not empty</p>")
        .hasParagraph()
        .hasEmptyParagraph())
      .isInstanceOf(AssertionError.class);
  }

  @Test
  public void noMoreBlock_whenNoMoreBlocks_passes() {
    assertThat("<p>Only paragraph</p>")
      .hasParagraph()
      .noMoreBlock();
  }

  @Test
  public void noMoreBlock_whenMoreBlocksExist_throwsAssertionError() {
    assertThatThrownBy(() -> 
      assertThat("<p>First</p><p>Second</p>")
        .hasParagraph()
        .noMoreBlock())
      .isInstanceOf(AssertionError.class)
      .hasMessageContaining("there are still some paragraph");
  }

  @Test
  public void hasList_afterParagraph_passes() {
    assertThat("<p>Intro</p><ul><li>Item 1</li><li>Item 2</li></ul>")
      .hasParagraph()
      .hasList()
      .withItemTexts("Item 1", "Item 2");
  }

  @Test
  public void hasList_withItemTexts_convenience() {
    assertThat("<p>Intro</p><ul><li>A</li><li>B</li></ul>")
      .hasParagraph()
      .hasList("A", "B");
  }

  @Test
  public void withLines_withElementsInLine_passes() {
    assertThat("<p>Text with <strong>bold</strong><br>Second line</p>")
      .hasParagraph()
      .withLines("Text with bold", "Second line");
  }

  @Test
  public void withLines_emptyParagraph_returnsEmptyList() {
    assertThat("<p></p>")
      .hasParagraph()
      .withText("");
  }
}
