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
import org.sonar.api.scan.source.SyntaxHighlightingRule;
import org.sonar.api.scan.source.SyntaxHighlightingRuleSet;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.List;

public class HtmlTextWrapper {

  private static final int END_OF_STREAM = -1;
  private static final char END_OF_LINE = '\n';

  public String wrapTextWithHtml(String text, SyntaxHighlightingRuleSet syntaxHighlighting) throws IOException {

    List<SyntaxHighlightingRule> highlightingRules = syntaxHighlighting.getSyntaxHighlightingRuleSet();
    StringBuilder decoratedText = new StringBuilder();

    BufferedReader stringBuffer = null;

    try {
      stringBuffer = new BufferedReader(new StringReader(text));

      int currentCharValue = stringBuffer.read();
      int currentCharIndex = 0;
      boolean isNewLine = true;

      while(currentCharValue != END_OF_STREAM) {

        if(isNewLine) {
          decoratedText.append("<tr><td>");
          reopenCurrentSyntaxTags(decoratedText);
          isNewLine = false;
        }

        if(currentCharValue == END_OF_LINE) {
          closeCurrentSyntaxTags(decoratedText);
          decoratedText.append("</tr></td>");
          isNewLine = true;
        }

        Collection<SyntaxHighlightingRule> endingRules = Collections2.filter(highlightingRules, new RuleIsMatchingIndex(currentCharIndex, false));
        if(!endingRules.isEmpty()) {
          for (SyntaxHighlightingRule rule : endingRules) {
            injectClosingHtmlForRule(rule, decoratedText);
          }
        }

        Collection<SyntaxHighlightingRule> startingRules = Collections2.filter(highlightingRules, new RuleIsMatchingIndex(currentCharIndex, true));
        if(!startingRules.isEmpty()) {
          for (SyntaxHighlightingRule rule : startingRules) {
            injectOpeningHtmlForRule(rule, decoratedText);
          }
        }
        decoratedText.append((char)currentCharValue);
        currentCharValue = stringBuffer.read();
        currentCharIndex++;
      }
    } catch (Exception Ex) {
      //
    } finally {
      closeReaderSilently(stringBuffer);
    }

    return decoratedText.toString();
  }

  private void closeCurrentSyntaxTags(StringBuilder decoratedText) {
  }

  private void reopenCurrentSyntaxTags(StringBuilder decoratedText) {
  }

  private void injectOpeningHtmlForRule(SyntaxHighlightingRule rule, StringBuilder decoratedText) {
    decoratedText.append("<span class=\"").append(rule.getTextType()).append("\">");
  }

  private void injectClosingHtmlForRule(SyntaxHighlightingRule rule, StringBuilder decoratedText) {
    decoratedText.append("</span>");
  }

  private void closeReaderSilently(BufferedReader reader) {
    try {
      if(reader != null) {
        reader.close();
      }
    } catch (IOException e) {
      //
    }
  }

  private class RuleIsMatchingIndex implements Predicate<SyntaxHighlightingRule> {

    private final int characterIndex;
    private final boolean isStarting;

    public RuleIsMatchingIndex(int charIndex, boolean isStarting) {
      this.isStarting = isStarting;
      this.characterIndex = charIndex;
    }

    @Override
    public boolean apply(@Nullable SyntaxHighlightingRule syntaxHighlightingRule) {
      if(syntaxHighlightingRule != null) {
        return isStarting ? characterIndex == syntaxHighlightingRule.getStartPosition() : characterIndex == syntaxHighlightingRule.getEndPosition();
      }
      return false;
    }
  }
}

