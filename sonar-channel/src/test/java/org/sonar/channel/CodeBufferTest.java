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
    CodeReader reader = new CodeReader("bar");
    assertThat(reader.lastChar(), is( -1));
    reader.pop();
    assertThat((char) reader.lastChar(), is('b'));
  }

  @Test
  public void testGetColumnAndLinePosition() {
    CodeReader reader = new CodeReader("pa\nc\r\ns\r\n\r\n");
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
    CodeReader reader = new CodeReader("123456");
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
    CodeReader reader = new CodeReader("123456");
    assertEquals('1', reader.charAt(0));
    assertEquals('6', reader.charAt(5));
  }

  @Test
  public void testCharAtIndexOutOfBoundsException() {
    CodeReader reader = new CodeReader("12345");
    assertEquals(reader.charAt(5), (char) -1);
  }
}
