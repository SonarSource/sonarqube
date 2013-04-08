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

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import org.slf4j.LoggerFactory;
import org.sonar.api.scan.source.SyntaxHighlightingRule;
import org.sonar.api.scan.source.SyntaxHighlightingRuleSet;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.List;

public class HtmlTextWrapper {

  private static final String OPEN_TABLE_LINE = "<tr><td>";
  private static final String CLOSE_TABLE_LINE = "</td></tr>";

  public static final char CR_END_OF_LINE = '\r';
  public static final char LF_END_OF_LINE = '\n';

  public String wrapTextWithHtml(String text, SyntaxHighlightingRuleSet syntaxHighlighting) throws IOException {

    List<SyntaxHighlightingRule> highlightingRules = syntaxHighlighting.getSyntaxHighlightingRuleSet();
    StringBuilder decoratedText = new StringBuilder();

    BufferedReader stringBuffer = null;

    try {
      stringBuffer = new BufferedReader(new StringReader(text));

      CharactersReader context = new CharactersReader(stringBuffer);

      while(context.readNextChar()) {

        if(shouldStartNewLine(context)) {
          decoratedText.append(OPEN_TABLE_LINE);
          if(shouldReopenPendingTags(context)) {
            reopenCurrentSyntaxTags(context, decoratedText);
          }
        }

        Collection<SyntaxHighlightingRule> tagsToClose =
                Collections2.filter(highlightingRules, new IndexRuleFilter(context.getCurrentIndex(), false));
        closeCompletedTags(context, tagsToClose, decoratedText);

        if(shouldClosePendingTags(context)) {
          closeCurrentSyntaxTags(context, decoratedText);
          decoratedText.append(CLOSE_TABLE_LINE);
        }

        Collection<SyntaxHighlightingRule> tagsToOpen =
                Collections2.filter(highlightingRules, new IndexRuleFilter(context.getCurrentIndex(), true));
        openNewTags(context, tagsToOpen, decoratedText);

        decoratedText.append((char)context.getCurrentValue());
      }
    } catch (IOException exception) {
      LoggerFactory.getLogger(HtmlTextWrapper.class).error("");
    } finally {
      closeReaderSilently(stringBuffer);
    }

    return decoratedText.toString();
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

  private void closeCompletedTags(CharactersReader context, Collection<SyntaxHighlightingRule> rulesMatchingCurrentIndex,
                                  StringBuilder decoratedText) {
    for (SyntaxHighlightingRule syntaxHighlightingRule : rulesMatchingCurrentIndex) {
      if(context.getCurrentIndex() == syntaxHighlightingRule.getEndPosition()) {
        injectClosingHtml(decoratedText);
        context.removeLastOpenTag();
      }
    }
  }

  private void openNewTags(CharactersReader context, Collection<SyntaxHighlightingRule> rulesMatchingCurrentIndex,
                           StringBuilder decoratedText) {
    for (SyntaxHighlightingRule syntaxHighlightingRule : rulesMatchingCurrentIndex) {
      if(context.getCurrentIndex() == syntaxHighlightingRule.getStartPosition()) {
        injectOpeningHtmlForRule(syntaxHighlightingRule.getTextType(), decoratedText);
        context.registerOpenTag(syntaxHighlightingRule.getTextType());
      }
    }
  }

  private void closeCurrentSyntaxTags(CharactersReader context, StringBuilder decoratedText) {
    for (int i = 0; i < context.getOpenTags().size(); i++) {
      injectClosingHtml(decoratedText);
    }
  }

  private void reopenCurrentSyntaxTags(CharactersReader context, StringBuilder decoratedText) {
    for (String tags : context.getOpenTags()) {
      injectOpeningHtmlForRule(tags, decoratedText);
    }
  }

  private void injectOpeningHtmlForRule(String textType, StringBuilder decoratedText) {
    decoratedText.append("<span class=\"").append(textType).append("\">");
  }

  private void injectClosingHtml(StringBuilder decoratedText) {
    decoratedText.append("</span>");
  }

  private void closeReaderSilently(BufferedReader reader) {
    try {
      if(reader != null) {
        reader.close();
      }
    } catch (IOException e) {
      LoggerFactory.getLogger(HtmlTextWrapper.class).error("Could not close ");
    }
  }

  private class IndexRuleFilter implements Predicate<SyntaxHighlightingRule> {

    private final int characterIndex;
    private final boolean isNewCharRange;

    public IndexRuleFilter(int charIndex, boolean isNewCharRange) {
      this.characterIndex = charIndex;
      this.isNewCharRange = isNewCharRange;
    }

    @Override
    public boolean apply(@Nullable SyntaxHighlightingRule syntaxHighlightingRule) {
      if(syntaxHighlightingRule != null) {
        return (characterIndex == syntaxHighlightingRule.getStartPosition() && isNewCharRange)
                || (characterIndex == syntaxHighlightingRule.getEndPosition() && !isNewCharRange);
      }
      return false;
    }
  }
}

