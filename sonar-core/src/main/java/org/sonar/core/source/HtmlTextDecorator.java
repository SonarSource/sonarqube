/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.core.source;

import com.google.common.collect.Lists;
import com.google.common.io.Closeables;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.List;

import static org.sonar.core.source.CharactersReader.END_OF_STREAM;

/**
 * @since 3.6
 */
class HtmlTextDecorator {

  static final char CR_END_OF_LINE = '\r';
  static final char LF_END_OF_LINE = '\n';
  static final char HTML_OPENING = '<';
  static final char HTML_CLOSING = '>';
  static final char AMPERSAND = '&';
  static final String ENCODED_HTML_OPENING = "&lt;";
  static final String ENCODED_HTML_CLOSING = "&gt;";
  static final String ENCODED_AMPERSAND = "&amp;";

  List<String> decorateTextWithHtml(String text, DecorationDataHolder decorationDataHolder) {

    StringBuilder currentHtmlLine = new StringBuilder();
    List<String> decoratedHtmlLines = Lists.newArrayList();

    BufferedReader stringBuffer = null;

    try {
      stringBuffer = new BufferedReader(new StringReader(text));

      CharactersReader charsReader = new CharactersReader(stringBuffer);

      while (charsReader.readNextChar()) {

        if (shouldStartNewLine(charsReader)) {
          decoratedHtmlLines.add(currentHtmlLine.toString());
          currentHtmlLine = new StringBuilder();
          if (shouldReopenPendingTags(charsReader)) {
            reopenCurrentSyntaxTags(charsReader, currentHtmlLine);
          }
        }

        int numberOfTagsToClose = getNumberOfTagsToClose(charsReader.getCurrentIndex(), decorationDataHolder);
        closeCompletedTags(charsReader, numberOfTagsToClose, currentHtmlLine);

        if (shouldClosePendingTags(charsReader)) {
          closeCurrentSyntaxTags(charsReader, currentHtmlLine);
        }

        Collection<String> tagsToOpen = getTagsToOpen(charsReader.getCurrentIndex(), decorationDataHolder);
        openNewTags(charsReader, tagsToOpen, currentHtmlLine);

        if (shouldAppendCharToHtmlOutput(charsReader)) {
          char currentChar = (char) charsReader.getCurrentValue();
          currentHtmlLine.append(normalize(currentChar));
        }
      }

      closeCurrentSyntaxTags(charsReader, currentHtmlLine);
      if (currentHtmlLine.length() > 0) {
        decoratedHtmlLines.add(currentHtmlLine.toString());
      }

    } catch (IOException exception) {
      String errorMsg = "An exception occurred while highlighting the syntax of one of the project's files";
      LoggerFactory.getLogger(HtmlTextDecorator.class).error(errorMsg);
      throw new IllegalStateException(errorMsg, exception);
    } finally {
      Closeables.closeQuietly(stringBuffer);
    }

    return decoratedHtmlLines;
  }

  private char[] normalize(char currentChar) {
    char[] normalizedChars;
    if (currentChar == HTML_OPENING) {
      normalizedChars = ENCODED_HTML_OPENING.toCharArray();
    } else if (currentChar == HTML_CLOSING) {
      normalizedChars = ENCODED_HTML_CLOSING.toCharArray();
    } else if (currentChar == AMPERSAND) {
      normalizedChars = ENCODED_AMPERSAND.toCharArray();
    } else {
      normalizedChars = new char[]{currentChar};
    }
    return normalizedChars;
  }

  private boolean shouldAppendCharToHtmlOutput(CharactersReader charsReader) {
    return charsReader.getCurrentValue() != CR_END_OF_LINE && charsReader.getCurrentValue() != LF_END_OF_LINE;
  }

  private int getNumberOfTagsToClose(int currentIndex, DecorationDataHolder dataHolder) {
    int numberOfTagsToClose = 0;

    while (currentIndex == dataHolder.getCurrentClosingTagOffset()) {
      numberOfTagsToClose++;
      dataHolder.nextClosingTagOffset();
    }
    return numberOfTagsToClose;
  }

  private Collection<String> getTagsToOpen(int currentIndex, DecorationDataHolder dataHolder) {
    Collection<String> tagsToOpen = Lists.newArrayList();
    while (dataHolder.getCurrentOpeningTagEntry() != null && currentIndex == dataHolder.getCurrentOpeningTagEntry().getStartOffset()) {
      tagsToOpen.add(dataHolder.getCurrentOpeningTagEntry().getCssClass());
      dataHolder.nextOpeningTagEntry();
    }
    return tagsToOpen;
  }

  private boolean shouldClosePendingTags(CharactersReader charactersReader) {
    return charactersReader.getCurrentValue() == CR_END_OF_LINE
      || (charactersReader.getCurrentValue() == LF_END_OF_LINE && charactersReader.getPreviousValue() != CR_END_OF_LINE)
      || (charactersReader.getCurrentValue() == END_OF_STREAM && charactersReader.getPreviousValue() != LF_END_OF_LINE);
  }

  private boolean shouldReopenPendingTags(CharactersReader charactersReader) {
    return (charactersReader.getPreviousValue() == LF_END_OF_LINE && charactersReader.getCurrentValue() != LF_END_OF_LINE)
      || (charactersReader.getPreviousValue() == CR_END_OF_LINE && charactersReader.getCurrentValue() != CR_END_OF_LINE
      && charactersReader.getCurrentValue() != LF_END_OF_LINE);
  }

  private boolean shouldStartNewLine(CharactersReader charactersReader) {
    return charactersReader.getPreviousValue() == LF_END_OF_LINE
      || (charactersReader.getPreviousValue() == CR_END_OF_LINE && charactersReader.getCurrentValue() != LF_END_OF_LINE);
  }

  private void closeCompletedTags(CharactersReader charactersReader, int numberOfTagsToClose,
                                  StringBuilder decoratedText) {
    for (int i = 0; i < numberOfTagsToClose; i++) {
      injectClosingHtml(decoratedText);
      charactersReader.removeLastOpenTag();
    }
  }

  private void openNewTags(CharactersReader charactersReader, Collection<String> tagsToOpen,
                           StringBuilder decoratedText) {
    for (String tagToOpen : tagsToOpen) {
      injectOpeningHtmlForRule(tagToOpen, decoratedText);
      charactersReader.registerOpenTag(tagToOpen);
    }
  }

  private void closeCurrentSyntaxTags(CharactersReader charactersReader, StringBuilder decoratedText) {
    for (int i = 0; i < charactersReader.getOpenTags().size(); i++) {
      injectClosingHtml(decoratedText);
    }
  }

  private void reopenCurrentSyntaxTags(CharactersReader charactersReader, StringBuilder decoratedText) {
    for (String tags : charactersReader.getOpenTags()) {
      injectOpeningHtmlForRule(tags, decoratedText);
    }
  }

  private void injectOpeningHtmlForRule(String textType, StringBuilder decoratedText) {
    decoratedText.append("<span class=\"").append(textType).append("\">");
  }

  private void injectClosingHtml(StringBuilder decoratedText) {
    decoratedText.append("</span>");
  }
}

