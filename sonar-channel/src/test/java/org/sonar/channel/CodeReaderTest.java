/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.number.OrderingComparisons.lessThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

public class CodeReaderTest {

  @Test
  public void testPopWithAppendable() {
    CodeReader reader = new CodeReader("package org.sonar;");

    StringBuilder sw = new StringBuilder();
    reader.pop(sw);
    assertEquals("p", sw.toString());
    reader.pop(sw);
    assertEquals("pa", sw.toString());

  }

  @Test
  public void testPeekACharArray() {
    CodeReader reader = new CodeReader(new StringReader("bar"));
    char[] chars = reader.peek(2);
    assertThat(chars.length, is(2));
    assertThat(chars[0], is('b'));
    assertThat(chars[1], is('a'));
  }

  @Test
  public void testPeekTo() {
    CodeReader reader = new CodeReader(new StringReader("package org.sonar;"));
    StringBuilder result = new StringBuilder();
    reader.peekTo(new EndMatcher() {

      public boolean match(int endFlag) {
        return 'r' == (char) endFlag;
      }
    }, result);
    assertEquals("package o", result.toString());
    assertThat(reader.peek(), is((int) 'p')); // never called pop()
  }

  @Test
  public void testPopTo() {
    CodeReader reader = new CodeReader(new StringReader("package org.sonar;"));
    StringBuilder result = new StringBuilder();
    reader.popTo(new EndMatcher() {

      public boolean match(int endFlag) {
        return 'r' == (char) endFlag;
      }
    }, result);
    assertThat(result.toString(), is("package o"));
    assertThat(reader.peek(), is((int) 'r'));
    CodeReader.Cursor previousCursor = reader.getPreviousCursor();
    assertThat(previousCursor.getColumn(), is(0));
    assertThat(previousCursor.getLine(), is(1));
  }

  @Test
  public void testPeekToAndReachingTheBufferLimit() {
    CodeReader reader = new CodeReader("word1 word2 word2", 10);
    for (int i = 0; i < 6; i++) {
      reader.pop();
    }
    StringBuilder result = new StringBuilder();
    reader.peekTo(new EndMatcher() {

      public boolean match(int endFlag) {
        return ' ' == (char) endFlag;
      }
    }, result);
    assertEquals("word2", result.toString());
  }

  @Test
  public void testPopToWithRegex() {
    CodeReader reader = new CodeReader(new StringReader("123ABC"));
    StringBuilder token = new StringBuilder();
    assertEquals(3, reader.popTo(Pattern.compile("\\d+").matcher(new String()), token));
    assertEquals("123", token.toString());
    assertEquals( -1, reader.popTo(Pattern.compile("\\d+").matcher(new String()), token));
    assertEquals(3, reader.popTo(Pattern.compile("\\w+").matcher(new String()), token));
    assertEquals("123ABC", token.toString());
    assertEquals( -1, reader.popTo(Pattern.compile("\\w+").matcher(new String()), token));
  }

  @Test
  public void testPopToWithRegexAndFollowingMatcher() {
    Matcher digitMatcher = Pattern.compile("\\d+").matcher(new String());
    Matcher alphabeticMatcher = Pattern.compile("[a-zA-Z]").matcher(new String());
    StringBuilder token = new StringBuilder();
    assertEquals( -1, new CodeReader(new StringReader("123 ABC")).popTo(digitMatcher, alphabeticMatcher, token));
    assertEquals("", token.toString());
    assertEquals(3, new CodeReader(new StringReader("123ABC")).popTo(digitMatcher, alphabeticMatcher, token));
    assertEquals("123", token.toString());
  }
}
