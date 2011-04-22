/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
