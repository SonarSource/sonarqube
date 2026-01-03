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

public class HtmlListAssertTest {

  @Test
  public void withItemTexts_matchingItems_passes() {
    assertThat("<p>Intro</p><ul><li>Item 1</li><li>Item 2</li><li>Item 3</li></ul>")
      .hasParagraph()
      .hasList()
      .withItemTexts("Item 1", "Item 2", "Item 3");
  }

  @Test
  public void withItemTexts_orderedList_passes() {
    assertThat("<p>Intro</p><ol><li>First</li><li>Second</li></ol>")
      .hasParagraph()
      .hasList()
      .withItemTexts("First", "Second");
  }

  @Test
  public void withItemTexts_nonMatchingItems_throwsAssertionError() {
    assertThatThrownBy(() -> 
      assertThat("<p>Intro</p><ul><li>Item 1</li><li>Item 2</li></ul>")
        .hasParagraph()
        .hasList()
        .withItemTexts("Item 1", "Item 3"))
      .isInstanceOf(AssertionError.class);
  }

  @Test
  public void hasParagraph_afterList_passes() {
    assertThat("<p>Intro</p><ul><li>Item</li></ul><p>Conclusion</p>")
      .hasParagraph()
      .hasList()
      .hasParagraph()
      .withText("Conclusion");
  }

  @Test
  public void hasParagraph_withText_convenience() {
    assertThat("<p>Intro</p><ul><li>Item</li></ul><p>End</p>")
      .hasParagraph()
      .hasList()
      .hasParagraph("End");
  }

  @Test
  public void hasEmptyParagraph_afterList_passes() {
    assertThat("<p>Intro</p><ul><li>Item</li></ul><p></p>")
      .hasParagraph()
      .hasList()
      .hasEmptyParagraph();
  }

  @Test
  public void verifyIsList_withNonList_throwsAssertionError() {
    assertThatThrownBy(() -> 
      assertThat("<p>Intro</p><div>Not a list</div>")
        .hasParagraph()
        .hasList())
      .isInstanceOf(AssertionError.class)
      .hasMessageContaining("next block is neither a <ul> nor a <ol>");
  }
}
