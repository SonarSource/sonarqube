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
public class JavaConstantTokenizer extends Tokenizer {

  private final String tagBefore;
  private final String tagAfter;
  private static final int DOT = '.';
  private static final EndMatcher END_TOKEN_MATCHER = new EndMatcher() {
    @Override
    public boolean match(int endFlag) {
      return !isJavaConstantPart(endFlag);
    }

    private boolean isJavaConstantPart(int character) {
      return Character.isUpperCase(character) || character == '_' || character == '-' || Character.isDigit(character);
    }
  };

  public JavaConstantTokenizer(String tagBefore, String tagAfter) {
    this.tagBefore = tagBefore;
    this.tagAfter = tagAfter;
  }

  private static boolean hasNextToken(CodeReader code) {
    int lastChar = code.lastChar();
    if (isJavaConstantStart(code.peek()) && !Character.isJavaIdentifierPart(lastChar) && !Character.isJavaIdentifierStart(lastChar)
      && lastChar != DOT) {
      String constant = code.peekTo(END_TOKEN_MATCHER);
      int nextCharAfterConstant = code.peek(constant.length() + 1)[constant.length()];
      if (nextCharAfterConstant != 0 && Character.isJavaIdentifierPart(nextCharAfterConstant)) {
        return false;
      }
      return true;
    }
    return false;
  }

  @Override
  public boolean consume(CodeReader code, HtmlCodeBuilder codeBuilder) {
    if (hasNextToken(code)) {
      codeBuilder.appendWithoutTransforming(tagBefore);
      code.popTo(END_TOKEN_MATCHER, codeBuilder);
      codeBuilder.appendWithoutTransforming(tagAfter);
      return true;
    } else {
      return false;
    }
  }

  private static boolean isJavaConstantStart(int character) {
    return Character.isUpperCase(character);
  }

}
