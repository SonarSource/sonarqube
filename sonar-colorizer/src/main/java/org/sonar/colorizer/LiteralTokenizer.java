/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.colorizer;

import org.sonar.channel.CodeReader;
import org.sonar.channel.EndMatcher;

/**
 * @deprecated since 4.5.2 replace by highlighting mechanism
 */
@Deprecated
public class LiteralTokenizer extends Tokenizer {

  private final String tagBefore;
  private final String tagAfter;

  public LiteralTokenizer(String tagBefore, String tagAfter) {
    this.tagBefore = tagBefore;
    this.tagAfter = tagAfter;
  }

  public LiteralTokenizer() {
    this("", "");
  }

  @Override
  public boolean consume(CodeReader code, HtmlCodeBuilder codeBuilder) {
    if (code.peek() == '\'' || code.peek() == '\"') {
      codeBuilder.appendWithoutTransforming(tagBefore);
      int firstChar = code.peek();
      code.popTo(new EndCommentMatcher(firstChar, code), codeBuilder);
      codeBuilder.appendWithoutTransforming(tagAfter);
      return true;
    } else {
      return false;
    }
  }

  private static class EndCommentMatcher implements EndMatcher {

    private final int firstChar;
    private final CodeReader code;
    private StringBuilder literalValue;

    public EndCommentMatcher(int firstChar, CodeReader code) {
      this.firstChar = firstChar;
      this.code = code;
      literalValue = new StringBuilder();
    }

    @Override
    public boolean match(int endFlag) {
      literalValue.append((char) endFlag);
      return code.lastChar() == firstChar && evenNumberOfBackSlashBeforeDelimiter() && literalValue.length() > 1;
    }

    private boolean evenNumberOfBackSlashBeforeDelimiter() {
      int numberOfBackSlashChar = 0;
      for (int index = literalValue.length() - 3; index >= 0; index--) {
        if (literalValue.charAt(index) == '\\') {
          numberOfBackSlashChar++;
        } else {
          break;
        }
      }
      return numberOfBackSlashChar % 2 == 0;
    }
  }
}
