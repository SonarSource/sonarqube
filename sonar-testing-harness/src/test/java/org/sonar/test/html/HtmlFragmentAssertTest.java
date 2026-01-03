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

public class HtmlFragmentAssertTest {

  @Test
  public void hasParagraph_withValidParagraph_returnsParagraphAssert() {
    assertThat("<p>Hello World</p>")
      .hasParagraph()
      .withText("Hello World");
  }

  @Test
  public void hasParagraph_withText_verifyText() {
    assertThat("<p>Test paragraph</p>")
      .hasParagraph("Test paragraph");
  }

  @Test
  public void hasParagraph_withNoBlocks_throwsAssertionError() {
    assertThatThrownBy(() -> assertThat("plain text without tags").hasParagraph())
      .isInstanceOf(AssertionError.class)
      .hasMessageContaining("no bloc in fragment");
  }

  @Test
  public void hasParagraph_withNonParagraphBlock_throwsAssertionError() {
    assertThatThrownBy(() -> assertThat("<div>Not a paragraph</div>").hasParagraph())
      .isInstanceOf(AssertionError.class)
      .hasMessageContaining("next block is not a <p>");
  }

  @Test
  public void hasParagraph_withMultipleParagraphs_returnsFirstParagraph() {
    assertThat("<p>First</p><p>Second</p>")
      .hasParagraph()
      .withText("First")
      .hasParagraph()
      .withText("Second");
  }

  @Test
  public void hasParagraph_withNull_throwsAssertionError() {
    assertThatThrownBy(() -> assertThat(null).hasParagraph())
      .isInstanceOf(AssertionError.class);
  }
}
