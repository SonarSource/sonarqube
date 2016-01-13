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

import java.util.Arrays;

/**
 * @deprecated since 4.5.2 replace by highlighting mechanism
 */
@Deprecated
public class MultilinesDocTokenizer extends Tokenizer {

  private static final String COMMENT_STARTED_ON_PREVIOUS_LINE = "COMMENT_STARTED_ON_PREVIOUS_LINE";
  private static final String COMMENT_TOKENIZER = "MULTILINE_COMMENT_TOKENIZER";
  private final char[] startToken;
  private final char[] endToken;
  private final String tagBefore;
  private final String tagAfter;

  /**
   * @deprecated endToken is hardcoded to star-slash, whatever the startToken !
   */
  @Deprecated
  public MultilinesDocTokenizer(String startToken, String tagBefore, String tagAfter) {
    this(startToken, "*/", tagBefore, tagAfter);
  }

  public MultilinesDocTokenizer(String startToken, String endToken, String tagBefore, String tagAfter) {
    this.tagBefore = tagBefore;
    this.tagAfter = tagAfter;
    this.startToken = startToken.toCharArray();
    this.endToken = endToken.toCharArray();
  }

  public boolean hasNextToken(CodeReader code, HtmlCodeBuilder codeBuilder) {
    return code.peek() != '\n'
      && code.peek() != '\r'
      && (isCommentStartedOnPreviousLine(codeBuilder) || (code.peek() == startToken[0] && Arrays.equals(code.peek(startToken.length),
        startToken)));
  }

  @Override
  public boolean consume(CodeReader code, HtmlCodeBuilder codeBuilder) {
    if (hasNextToken(code, codeBuilder)) {
      codeBuilder.appendWithoutTransforming(tagBefore);
      code.popTo(new MultilineEndTokenMatcher(code, codeBuilder), codeBuilder);
      codeBuilder.appendWithoutTransforming(tagAfter);
      return true;
    } else {
      return false;
    }
  }

  private class MultilineEndTokenMatcher implements EndMatcher {

    private final CodeReader code;
    private final HtmlCodeBuilder codeBuilder;
    private int commentSize = 0;

    public MultilineEndTokenMatcher(CodeReader code, HtmlCodeBuilder codeBuilder) {
      this.code = code;
      this.codeBuilder = codeBuilder;
    }

    @Override
    public boolean match(int endFlag) {
      commentSize++;
      if (commentSize >= endToken.length + startToken.length || (commentSize >= endToken.length && isCommentStartedOnPreviousLine(codeBuilder))) {
        boolean matches = true;
        for (int i = 1; i <= endToken.length; i++) {
          if (code.charAt(-i) != endToken[endToken.length - i]) {
            matches = false;
            break;
          }
        }
        if (matches) {
          setCommentStartedOnPreviousLine(codeBuilder, Boolean.FALSE);
          return true;
        }
      }

      if (endFlag == '\r' || endFlag == '\n') {
        setCommentStartedOnPreviousLine(codeBuilder, Boolean.TRUE);
        return true;
      }
      return false;
    }
  }

  private boolean isCommentStartedOnPreviousLine(HtmlCodeBuilder codeBuilder) {
    Boolean b = (Boolean) codeBuilder.getVariable(COMMENT_STARTED_ON_PREVIOUS_LINE, Boolean.FALSE);
    return (b == Boolean.TRUE) && (this.equals(codeBuilder.getVariable(COMMENT_TOKENIZER)));
  }

  private void setCommentStartedOnPreviousLine(HtmlCodeBuilder codeBuilder, Boolean b) {
    codeBuilder.setVariable(COMMENT_STARTED_ON_PREVIOUS_LINE, b);
    codeBuilder.setVariable(COMMENT_TOKENIZER, b ? this : null);
  }

}
