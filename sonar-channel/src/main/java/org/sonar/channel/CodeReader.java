/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.channel;

import java.io.IOException;
import java.io.Reader;
import java.util.regex.Matcher;

/**
 * The CodeReader class provides some advanced features to read a source code. The most important one is the ability to try consuming the
 * next characters in the stream according to a regular expression.
 */
public class CodeReader extends CodeBuffer {

  private Cursor previousCursor;

  /*
   * Constructor needed to be backward compatible (before using CodeReaderFilter)
   */
  public CodeReader(Reader code) {
    super(code, new CodeReaderConfiguration());
  }

  /*
   * Constructor needed to be backward compatible (before using CodeReaderFilter)
   */
  public CodeReader(String code) {
    super(code, new CodeReaderConfiguration());
  }

  /**
   * Creates a code reader with specific configuration parameters.
   * Note that this constructor will read everything from reader and will close it.
   *
   * @param code
   *          the Reader to read code from
   * @param configuration
   *          the configuration parameters
   */
  public CodeReader(Reader code, CodeReaderConfiguration configuration) {
    super(code, configuration);
  }

  /**
   * Creates a code reader with specific configuration parameters.
   *
   * @param code
   *          the code itself
   * @param configuration
   *          the configuration parameters
   */
  public CodeReader(String code, CodeReaderConfiguration configuration) {
    super(code, configuration);
  }

  /**
   * Read and consume the next character
   *
   * @param appendable
   *          the read character is appended to appendable
   */
  public final void pop(Appendable appendable) {
    try {
      appendable.append((char) pop());
    } catch (IOException e) {
      throw new ChannelException(e.getMessage(), e);
    }
  }

  /**
   * Read without consuming the next characters
   *
   * @param length
   *          number of character to read
   * @return array of characters
   */
  public final char[] peek(int length) {
    char[] result = new char[length];
    int index = 0;
    int nextChar = intAt(index);
    while (nextChar != -1 && index < length) {
      result[index] = (char) nextChar;
      nextChar = intAt(++index);
    }
    return result;
  }

  /**
   * Read without consuming the next characters until a condition is reached (EndMatcher)
   *
   * @param matcher
   *          the EndMatcher used to stop the reading
   * @param appendable
   *          the read characters is appended to appendable
   */
  public final void peekTo(EndMatcher matcher, Appendable appendable) {
    int index = 0;
    char nextChar = charAt(index);
    try {
      while (!matcher.match(nextChar) && nextChar != -1) {
        appendable.append(nextChar);
        nextChar = charAt(++index);
      }
    } catch (IOException e) {
      throw new ChannelException(e.getMessage(), e);
    }
  }

  /**
   * @deprecated in 2.2, use {@link #peekTo(EndMatcher matcher, Appendable appendable)} instead
   */
  @Deprecated
  public final String peekTo(EndMatcher matcher) {
    StringBuilder sb = new StringBuilder();
    peekTo(matcher, sb);
    return sb.toString();
  }

  /**
   * @deprecated in 2.2, use {@link #popTo(Matcher matcher, Appendable appendable)} instead
   */
  @Deprecated
  public final void popTo(EndMatcher matcher, Appendable appendable) {
    previousCursor = getCursor().clone();
    try {
      do {
        appendable.append((char) pop());
      } while (!matcher.match(peek()) && peek() != -1);
    } catch (IOException e) {
      throw new ChannelException(e.getMessage(), e);
    }
  }

  /**
   * Read and consume the next characters according to a given regular expression
   *
   * @param matcher
   *          the regular expression matcher
   * @param appendable
   *          the consumed characters are appended to this appendable
   * @return number of consumed characters or -1 if the next input sequence doesn't match this matcher's pattern
   */
  public final int popTo(Matcher matcher, Appendable appendable) {
    return popTo(matcher, null, appendable);
  }

  /**
   * Read and consume the next characters according to a given regular expression. Moreover the character sequence immediately following the
   * desired characters must also match a given regular expression.
   *
   * @param matcher
   *          the Matcher used to try consuming next characters
   * @param afterMatcher
   *          the Matcher used to check character sequence immediately following the consumed characters
   * @param appendable
   *          the consumed characters are appended to this appendable
   * @return number of consumed characters or -1 if one of the two Matchers doesn't match
   */
  public final int popTo(Matcher matcher, Matcher afterMatcher, Appendable appendable) {
    try {
      matcher.reset(this);
      if (matcher.lookingAt()) {
        if (afterMatcher != null) {
          afterMatcher.reset(this);
          afterMatcher.region(matcher.end(), length());
          if (!afterMatcher.lookingAt()) {
            return -1;
          }
        }
        previousCursor = getCursor().clone();
        for (int i = 0; i < matcher.end(); i++) {
          appendable.append((char) pop());
        }
        return matcher.end();
      }
    } catch (StackOverflowError e) {
      throw new ChannelException("Unable to apply regular expression '" + matcher.pattern().pattern()
          + "' at line " + getCursor().getLine() + " and column " + getCursor().getColumn()
          + ", because it led to a stack overflow error."
          + " This error may be due to an inefficient use of alternations - see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5050507", e);
    } catch (IndexOutOfBoundsException e) {
      return -1;
    } catch (IOException e) {
      throw new ChannelException(e.getMessage(), e);
    }
    return -1;
  }

  public final Cursor getPreviousCursor() {
    return previousCursor;
  }
}
