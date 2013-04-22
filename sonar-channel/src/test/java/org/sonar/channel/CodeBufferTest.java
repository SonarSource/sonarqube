/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.channel;

import static junit.framework.Assert.assertEquals;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.regex.Pattern;

import org.junit.Test;

public class CodeBufferTest {

  private CodeReaderConfiguration defaulConfiguration = new CodeReaderConfiguration();

  @Test
  public void testPop() {
    CodeBuffer code = new CodeBuffer("pa", defaulConfiguration);
    assertThat((char) code.pop(), is('p'));
    assertThat((char) code.pop(), is('a'));
    assertThat(code.pop(), is( -1));
  }

  @Test
  public void testPeek() {
    CodeBuffer code = new CodeBuffer("pa", defaulConfiguration);
    assertThat((char) code.peek(), is('p'));
    assertThat((char) code.peek(), is('p'));
    code.pop();
    assertThat((char) code.peek(), is('a'));
    code.pop();
    assertThat(code.peek(), is( -1));
  }

  @Test
  public void testLastCharacter() {
    CodeBuffer reader = new CodeBuffer("bar", defaulConfiguration);
    assertThat(reader.lastChar(), is( -1));
    reader.pop();
    assertThat((char) reader.lastChar(), is('b'));
  }

  @Test
  public void testGetColumnAndLinePosition() {
    CodeBuffer reader = new CodeBuffer("pa\nc\r\ns\r\n\r\n", defaulConfiguration);
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
    assertThat(reader.getColumnPosition(), is(2));
    assertThat(reader.getLinePosition(), is(3));
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
    CodeBuffer reader = new CodeBuffer("123456", defaulConfiguration);
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
    CodeBuffer reader = new CodeBuffer("123456", defaulConfiguration);
    assertEquals('1', reader.charAt(0));
    assertEquals('6', reader.charAt(5));
  }

  @Test
  public void testCharAtIndexOutOfBoundsException() {
    CodeBuffer reader = new CodeBuffer("12345", defaulConfiguration);
    assertEquals(reader.charAt(5), (char) -1);
  }

  @Test
  public void testReadWithSpecificTabWidth() {
    CodeReaderConfiguration configuration = new CodeReaderConfiguration();
    configuration.setTabWidth(4);
    CodeBuffer reader = new CodeBuffer("pa\n\tc", configuration);
    assertEquals('\n', reader.charAt(2));
    assertEquals('\t', reader.charAt(3));
    assertEquals('c', reader.charAt(4));
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
    reader.pop(); // \t
    assertThat(reader.getColumnPosition(), is(4));
    assertThat(reader.getLinePosition(), is(2));
    reader.pop(); // c
    assertThat(reader.getColumnPosition(), is(5));
    assertThat(reader.getLinePosition(), is(2));
  }

  @Test
  public void testCodeReaderFilter() throws Exception {
    CodeReaderConfiguration configuration = new CodeReaderConfiguration();
    configuration.setCodeReaderFilters(new ReplaceNumbersFilter());
    CodeBuffer code = new CodeBuffer("abcd12efgh34", configuration);
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
  public void theLengthShouldBeTheSameThanTheStringLength() {
    String myCode = "myCode";
    assertThat(new CodeBuffer(myCode, new CodeReaderConfiguration()).length(), is(6));
  }

  @Test
  public void theLengthShouldDecreaseEachTimeTheInputStreamIsConsumed() {
    String myCode = "myCode";
    CodeBuffer codeBuffer = new CodeBuffer(myCode, new CodeReaderConfiguration());
    codeBuffer.pop();
    codeBuffer.pop();
    assertThat(codeBuffer.length(), is(4));
  }

  @Test
  public void testSeveralCodeReaderFilter() throws Exception {
    CodeReaderConfiguration configuration = new CodeReaderConfiguration();
    configuration.setCodeReaderFilters(new ReplaceNumbersFilter(), new ReplaceCharFilter());
    CodeBuffer code = new CodeBuffer("abcd12efgh34", configuration);
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

  @Test
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void testChannelCodeReaderFilter() throws Exception {
    // create a windowing channel that drops the 2 first characters, keeps 6 characters and drops the rest of the line
    CodeReaderConfiguration configuration = new CodeReaderConfiguration();
    configuration.setCodeReaderFilters(new ChannelCodeReaderFilter(new Object(), new WindowingChannel()));
    CodeBuffer code = new CodeBuffer("0123456789\nABCDEFGHIJ", configuration);
    // test #charAt
    assertEquals('2', code.charAt(0));
    assertEquals('7', code.charAt(5));
    assertEquals('\n', code.charAt(6));
    assertEquals('C', code.charAt(7));
    assertEquals('H', code.charAt(12));
    assertEquals( -1, code.intAt(13));
    // test peek and pop
    assertThat((char) code.peek(), is('2'));
    assertThat((char) code.pop(), is('2'));
    assertThat((char) code.pop(), is('3'));
    assertThat((char) code.pop(), is('4'));
    assertThat((char) code.pop(), is('5'));
    assertThat((char) code.pop(), is('6'));
    assertThat((char) code.pop(), is('7'));// and 8 shouldn't show up
    assertThat((char) code.pop(), is('\n'));
    assertThat((char) code.peek(), is('C'));
    assertThat((char) code.pop(), is('C'));
    assertThat((char) code.pop(), is('D'));
    assertThat((char) code.pop(), is('E'));
    assertThat((char) code.pop(), is('F'));
    assertThat((char) code.pop(), is('G'));
    assertThat((char) code.pop(), is('H'));
    assertThat(code.pop(), is( -1));
  }

  /**
   * Backward compatibility with a COBOL plugin: filter returns 0 instead of -1, when end of the stream has been reached.
   */
  @Test(timeout = 1000)
  public void testWrongEndOfStreamFilter() {
    CodeReaderConfiguration configuration = new CodeReaderConfiguration();
    configuration.setCodeReaderFilters(new WrongEndOfStreamFilter());
    new CodeBuffer("foo", configuration);
  }

  class WrongEndOfStreamFilter extends CodeReaderFilter<Object> {
    @Override
    public int read(char[] filteredBuffer, int offset, int length) throws IOException {
      return 0;
    }
  }

  class ReplaceNumbersFilter extends CodeReaderFilter<Object> {

    private Pattern pattern = Pattern.compile("\\d");
    private String REPLACEMENT = "-";

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
      char[] tempBuffer = new char[cbuf.length];
      int charCount = getReader().read(tempBuffer, off, len);
      if (charCount != -1) {
        String filteredString = pattern.matcher(new String(tempBuffer)).replaceAll(REPLACEMENT);
        System.arraycopy(filteredString.toCharArray(), 0, cbuf, 0, tempBuffer.length);
      }
      return charCount;
    }
  }

  class ReplaceCharFilter extends CodeReaderFilter<Object> {

    private Pattern pattern = Pattern.compile("[a-zA-Z]");
    private String REPLACEMENT = "*";

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
      char[] tempBuffer = new char[cbuf.length];
      int charCount = getReader().read(tempBuffer, off, len);
      if (charCount != -1) {
        String filteredString = pattern.matcher(new String(tempBuffer)).replaceAll(REPLACEMENT);
        System.arraycopy(filteredString.toCharArray(), 0, cbuf, 0, tempBuffer.length);
      }
      return charCount;
    }
  }

  @SuppressWarnings("rawtypes")
  class WindowingChannel extends Channel {

    @Override
    public boolean consume(CodeReader code, Object output) {
      int columnPosition = code.getColumnPosition();
      if (code.peek() == '\n') {
        return false;
      }
      if (columnPosition < 2 || columnPosition > 7) {
        code.pop();
        return true;
      }
      return false;
    }
  }
}
