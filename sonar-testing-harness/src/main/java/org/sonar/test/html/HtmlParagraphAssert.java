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
package org.sonar.test.html;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import static java.util.Collections.emptyList;

public class HtmlParagraphAssert extends HtmlBlockAssert<HtmlParagraphAssert> {
  private final Iterator<Element> nextBlocks;

  public HtmlParagraphAssert(Element paragraph, Iterator<Element> nextBlocks) {
    super(paragraph, HtmlParagraphAssert.class);
    this.nextBlocks = nextBlocks;
  }

  static void verifyIsParagraph(Element element) {
    Assertions.assertThat(element.tagName())
      .describedAs(
        "next block is not a <%s> (got <%s>):" + PRINT_FRAGMENT_TEMPLATE,
        "p", element.tagName(), element.toString())
      .isEqualTo("p");
  }

  /**
   * Verify the next block exists, is a paragraph and returns an Assert on this block.
   */
  public HtmlParagraphAssert hasParagraph() {
    return new HtmlParagraphAssert(hasParagraphImpl(), nextBlocks);
  }

  private Element hasParagraphImpl() {
    isNotNull();

    Assertions.assertThat(nextBlocks.hasNext())
      .describedAs("no more bloc")
      .isTrue();

    Element element = nextBlocks.next();
    verifyIsParagraph(element);
    return element;
  }

  /**
   * Convenience method.
   * Sames as {@code hasParagraph().withText(text)}.
   */
  public HtmlParagraphAssert hasParagraph(String text) {
    return hasParagraph()
      .withText(text);
  }

  /**
   * Verifies next paragraph is empty or contains only "&nbsp;"
   */
  public HtmlParagraphAssert hasEmptyParagraph() {
    Element paragraph = hasParagraphImpl();

    Assertions.assertThat(paragraph.text())
      .describedAs(PRINT_FRAGMENT_TEMPLATE, paragraph)
      .isIn("", "\u00A0");

    return new HtmlParagraphAssert(paragraph, nextBlocks);
  }

  /**
   * Verifies there is no more block.
   */
  public void noMoreBlock() {
    isNotNull();

    Assertions.assertThat(nextBlocks.hasNext())
      .describedAs("there are still some paragraph. Next one:" + PRINT_FRAGMENT_TEMPLATE,
        new Object() {
          @Override
          public String toString() {
            return nextBlocks.next().toString();
          }
        })
      .isFalse();
  }

  /**
   * Verifies the current block as the specified text, ignoring lines.
   */
  public HtmlParagraphAssert withText(String text) {
    isNotNull();

    Assertions.assertThat(actual.text())
      .describedAs(PRINT_FRAGMENT_TEMPLATE, actual)
      .isEqualTo(text);

    return this;
  }

  /**
   * Verifies the current block has all and only the specified lines, in order.
   */
  public HtmlParagraphAssert withLines(String firstLine, String... otherLines) {
    isNotNull();

    List<String> actualLines = toLines(actual);
    String[] expectedLines = Stream.concat(
      Stream.of(firstLine),
      Arrays.stream(otherLines))
      .toArray(String[]::new);

    Assertions.assertThat(actualLines)
      .describedAs(PRINT_FRAGMENT_TEMPLATE, actual)
      .containsExactly(expectedLines);

    return this;
  }

  private static List<String> toLines(Element parent) {
    Iterator<Node> iterator = parent.childNodes().iterator();
    if (!iterator.hasNext()) {
      return emptyList();
    }

    List<String> actualLines = new ArrayList<>(parent.childNodeSize());
    StringBuilder currentLine = null;
    while (iterator.hasNext()) {
      Node node = iterator.next();
      if (node instanceof TextNode) {
        if (currentLine == null) {
          currentLine = new StringBuilder(node.toString());
        } else {
          currentLine.append(node.toString());
        }
      } else if (node instanceof Element) {
        Element element = (Element) node;
        if (element.tagName().equals("br")) {
          actualLines.add(currentLine == null ? "" : currentLine.toString());
          currentLine = null;
        } else {
          if (currentLine == null) {
            currentLine = new StringBuilder(element.text());
          } else {
            currentLine.append(element.text());
          }
        }
      } else {
        throw new IllegalStateException("unsupported node " + node.getClass());
      }

      if (!iterator.hasNext()) {
        actualLines.add(currentLine == null ? "" : currentLine.toString());
        currentLine = null;
      }
    }
    return actualLines;
  }

  /**
   * Convenience method.
   * Same as {@code hasList().withItemTexts("foo", "bar")}.
   */
  public HtmlListAssert hasList(String firstItemText, String... otherItemsText) {
    return hasList()
      .withItemTexts(firstItemText, otherItemsText);
  }

  public HtmlListAssert hasList() {
    isNotNull();

    Assertions.assertThat(nextBlocks.hasNext())
      .describedAs("no more block")
      .isTrue();

    Element element = nextBlocks.next();
    HtmlListAssert.verifyIsList(element);

    return new HtmlListAssert(element, nextBlocks);
  }
}
