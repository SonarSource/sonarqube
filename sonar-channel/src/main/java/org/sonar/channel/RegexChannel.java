/*
 * Copyright (C) 2010 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package org.sonar.channel;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class RegexChannel<OUTPUT> extends Channel<OUTPUT> {

  private final StringBuilder tmpBuilder = new StringBuilder();
  private final Matcher matcher;
  private final String regex;

  public RegexChannel(String regex) {
    matcher = Pattern.compile(regex).matcher("");
    this.regex = regex;
  }

  @Override
  public final boolean consume(CodeReader code, OUTPUT output) {
    try {
      if (code.popTo(matcher, tmpBuilder) > 0) {
        consume(tmpBuilder, output);
        tmpBuilder.delete(0, tmpBuilder.length());
        return true;
      }
      return false;
    } catch (StackOverflowError e) {
      throw new RuntimeException(
          "The regular expression "
              + regex
              + " has led to a stack overflow error. "
              + "This error is certainly due to an inefficient use of alternations. See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5050507",
          e);
    }
  }

  protected abstract void consume(CharSequence token, OUTPUT output);
}
