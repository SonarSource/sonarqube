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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.Reader;
import java.util.regex.Pattern;

import org.junit.Test;

public class CodeBufferTest {

  @Test
  public void testPop() {
    CodeBuffer code = new CodeBuffer("pa");
    assertThat((char) code.pop(), is('p'));
    assertThat((char) code.pop(), is('a'));
    assertThat(code.pop(), is( -1));
  }

  @Test
  public void testPeek() {
    CodeBuffer code = new CodeBuffer("pa");
    assertThat((char) code.peek(), is('p'));
    assertThat((char) code.peek(), is('p'));
    code.pop();
    assertThat((char) code.peek(), is('a'));
    code.pop();
    assertThat(code.peek(), is( -1));
  }

  @Test
  public void testLastCharacter() {
    CodeBuffer reader = new CodeBuffer("bar");
    assertThat(reader.lastChar(), is( -1));
    reader.pop();
    assertThat((char) reader.lastChar(), is('b'));
  }

  @Test
  public void testGetColumnAndLinePosition() {
    CodeBuffer reader = new CodeBuffer("pa\nc\r\ns\r\n\r\n");
    assertThat(reader.getColumnPosition(), is(0));
    assertThat(reader.getLinePosition(), is(1));
    reader.pop(); // p
    reader.pop(); // a
    assertThat(reader.getColumnPosition(), is(2));
    assertThat(reader.getLinePosition(), is(1));
    reader.peek(); // \n
    reader.lastChar(); // a
    assertThat(reader.getColumnPosition(), is(2));
    assertThat(reader.getLinePosition(), is(1));
    reader.pop(); // \n
    assertThat(reader.getColumnPosition(), is(0));
    assertThat(reader.getLinePosition(), is(2));
    reader.pop(); // c
    assertThat(reader.getColumnPosition(), is(1));
    assertThat(reader.getLinePosition(), is(2));
    reader.pop(); // \r
    reader.pop(); // \n
    assertThat(reader.getColumnPosition(), is(0));
    assertThat(reader.getLinePosition(), is(3));
    assertThat((char) reader.pop(), is('s'));
    reader.pop(); // \r
    assertThat(reader.getColumnPosition(), is(0));
    assertThat(reader.getLinePosition(), is(4));
    reader.pop(); // \n
    assertThat(reader.getColumnPosition(), is(0));
    assertThat(reader.getLinePosition(), is(4));
    reader.pop(); // \r
    reader.pop(); // \n
    assertThat(reader.getColumnPosition(), is(0));
    assertThat(reader.getLinePosition(), is(5));
  }

  @Test
  public void testStartAndStopRecording() {
    CodeBuffer reader = new CodeBuffer("123456");
    reader.pop();
    assertEquals("", reader.stopRecording().toString());

    reader.startRecording();
    reader.pop();
    reader.pop();
    reader.peek();
    assertEquals("23", reader.stopRecording().toString());
    assertEquals("", reader.stopRecording().toString());
  }

  @Test
  public void testCharAt() {
    CodeBuffer reader = new CodeBuffer("123456");
    assertEquals('1', reader.charAt(0));
    assertEquals('6', reader.charAt(5));
  }

  @Test
  public void testCharAtIndexOutOfBoundsException() {
    CodeBuffer reader = new CodeBuffer("12345");
    assertEquals(reader.charAt(5), (char) -1);
  }

  @Test
  public void testCodeReaderFilter() throws Exception {
    CodeBuffer code = new CodeBuffer("abcd12efgh34", new ReplaceNumbersFilter());
    // test #charAt
    assertEquals('a', code.charAt(0));
    assertEquals('-', code.charAt(4));
    assertEquals('-', code.charAt(5));
    assertEquals('e', code.charAt(6));
    assertEquals('-', code.charAt(10));
    assertEquals('-', code.charAt(11));
    // test peek and pop
    assertThat((char) code.peek(), is('a'));
    assertThat((char) code.pop(), is('a'));
    assertThat((char) code.pop(), is('b'));
    assertThat((char) code.pop(), is('c'));
    assertThat((char) code.pop(), is('d'));
    assertThat((char) code.peek(), is('-'));
    assertThat((char) code.pop(), is('-'));
    assertThat((char) code.pop(), is('-'));
    assertThat((char) code.pop(), is('e'));
    assertThat((char) code.pop(), is('f'));
    assertThat((char) code.pop(), is('g'));
    assertThat((char) code.pop(), is('h'));
    assertThat((char) code.pop(), is('-'));
    assertThat((char) code.pop(), is('-'));
  }

  @Test
  public void testSeveralCodeReaderFilter() throws Exception {
    CodeBuffer code = new CodeBuffer("abcd12efgh34", new ReplaceNumbersFilter(), new ReplaceCharFilter());
    // test #charAt
    assertEquals('*', code.charAt(0));
    assertEquals('-', code.charAt(4));
    assertEquals('-', code.charAt(5));
    assertEquals('*', code.charAt(6));
    assertEquals('-', code.charAt(10));
    assertEquals('-', code.charAt(11));
    // test peek and pop
    assertThat((char) code.peek(), is('*'));
    assertThat((char) code.pop(), is('*'));
    assertThat((char) code.pop(), is('*'));
    assertThat((char) code.pop(), is('*'));
    assertThat((char) code.pop(), is('*'));
    assertThat((char) code.peek(), is('-'));
    assertThat((char) code.pop(), is('-'));
    assertThat((char) code.pop(), is('-'));
    assertThat((char) code.pop(), is('*'));
    assertThat((char) code.pop(), is('*'));
    assertThat((char) code.pop(), is('*'));
    assertThat((char) code.pop(), is('*'));
    assertThat((char) code.pop(), is('-'));
    assertThat((char) code.pop(), is('-'));
  }

  class ReplaceNumbersFilter extends CodeReaderFilter {

    private Pattern pattern = Pattern.compile("\\d");
    private String REPLACEMENT = "-";

    public int read(Reader in, char[] cbuf, int off, int len) throws IOException {
      char[] tempBuffer = new char[cbuf.length];
      int charCount = in.read(tempBuffer, off, len);
      if (charCount != -1) {
        String filteredString = pattern.matcher(new String(tempBuffer)).replaceAll(REPLACEMENT);
        System.arraycopy(filteredString.toCharArray(), 0, cbuf, 0, tempBuffer.length);
      }
      return charCount;
    }
  }

  class ReplaceCharFilter extends CodeReaderFilter {

    private Pattern pattern = Pattern.compile("[a-zA-Z]");
    private String REPLACEMENT = "*";

    public int read(Reader in, char[] cbuf, int off, int len) throws IOException {
      char[] tempBuffer = new char[cbuf.length];
      int charCount = in.read(tempBuffer, off, len);
      if (charCount != -1) {
        String filteredString = pattern.matcher(new String(tempBuffer)).replaceAll(REPLACEMENT);
        System.arraycopy(filteredString.toCharArray(), 0, cbuf, 0, tempBuffer.length);
      }
      return charCount;
    }
  }

}
