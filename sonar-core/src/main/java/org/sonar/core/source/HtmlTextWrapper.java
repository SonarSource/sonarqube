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

import com.google.common.io.Closeables;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Collections;

/**
 * @since 3.6
 */
public class HtmlTextWrapper {

  private static final String OPEN_TABLE_LINE = "<tr><td>";
  private static final String CLOSE_TABLE_LINE = "</td></tr>";

  public static final char CR_END_OF_LINE = '\r';
  public static final char LF_END_OF_LINE = '\n';

  public String wrapTextWithHtml(String text, HighlightingContext context) throws IOException {

    StringBuilder decoratedText = new StringBuilder();

    BufferedReader stringBuffer = null;

    try {
      stringBuffer = new BufferedReader(new StringReader(text));

      CharactersReader charsReader = new CharactersReader(stringBuffer);

      while (charsReader.readNextChar()) {

        if (shouldStartNewLine(charsReader)) {
          decoratedText.append(OPEN_TABLE_LINE);
          if (shouldReopenPendingTags(charsReader)) {
            reopenCurrentSyntaxTags(charsReader, decoratedText);
          }
        }

        int numberOfTagsToClose = getNumberOfTagsToClose(charsReader.getCurrentIndex(), context);
        closeCompletedTags(charsReader, numberOfTagsToClose, decoratedText);

        if (shouldClosePendingTags(charsReader)) {
          closeCurrentSyntaxTags(charsReader, decoratedText);
          decoratedText.append(CLOSE_TABLE_LINE);
        }

        Collection<String> tagsToOpen = getTagsToOpen(charsReader.getCurrentIndex(), context);
        openNewTags(charsReader, tagsToOpen, decoratedText);

        decoratedText.append((char) charsReader.getCurrentValue());
      }
    } catch (IOException exception) {
      String errorMsg = "An exception occurred while highlighting the syntax of one of the project's files";
      LoggerFactory.getLogger(HtmlTextWrapper.class).error(errorMsg);
      throw new IllegalStateException(errorMsg, exception);
    } finally {
      Closeables.closeQuietly(stringBuffer);
    }

    return decoratedText.toString();
  }

  private int getNumberOfTagsToClose(int currentIndex, HighlightingContext context) {
    return Collections.frequency(context.getUpperBoundsDefinitions(), currentIndex);
  }

  private Collection<String> getTagsToOpen(int currentIndex, HighlightingContext context) {
    return context.getLowerBoundsDefinitions().get(currentIndex);
  }

  public boolean shouldClosePendingTags(CharactersReader context) {
    return context.getCurrentValue() == CR_END_OF_LINE
            || (context.getCurrentValue() == LF_END_OF_LINE && context.getPreviousValue() != CR_END_OF_LINE);
  }

  public boolean shouldReopenPendingTags(CharactersReader context) {
    return context.getPreviousValue() == LF_END_OF_LINE && context.getCurrentValue() != LF_END_OF_LINE;
  }

  public boolean shouldStartNewLine(CharactersReader context) {
    return context.getPreviousValue() == LF_END_OF_LINE || context.getCurrentIndex() == 0;
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

