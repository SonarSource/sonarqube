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

package org.sonar.squid.text;

import org.junit.After;

import org.junit.Test;
import org.sonar.squid.text.StringArrayReader.EndOfLineDelimiter;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StringArrayReaderTest {
  StringArrayReader reader;

  @After
  public void closeReader() throws IOException {
    reader.close();
  }

  @Test
  public void read() throws IOException {
    String[] lines = {"import java.util.*;", "//NOSONAR comment",};
    reader = new StringArrayReader(lines);
    assertEquals('i', reader.read());
    assertEquals('m', reader.read());
  }

  @Test
  public void testLFEndOfLineDelimiter() throws IOException {
    String[] lines = {";", ";",};
    reader = new StringArrayReader(lines, EndOfLineDelimiter.LF);
    assertEquals(';', reader.read());
    assertEquals('\n', reader.read());
    assertEquals(';', reader.read());
  }

  @Test
  public void testCREndOfLineDelimiter() throws IOException {
    String[] lines = {";", ";",};
    reader = new StringArrayReader(lines, EndOfLineDelimiter.CR);
    assertEquals(';', reader.read());
    assertEquals('\r', reader.read());
    assertEquals(';', reader.read());
  }

  @Test
  public void testCRPlusLFEndOfLineDelimiter() throws IOException {
    String[] lines = {";", ";",};
    reader = new StringArrayReader(lines, EndOfLineDelimiter.CR_PLUS_LF);
    assertEquals(';', reader.read());
    assertEquals('\r', reader.read());
    assertEquals('\n', reader.read());
    assertEquals(';', reader.read());
  }

  @Test
  public void ready() throws IOException {
    String[] lines = {";", "//NOSONAR",};
    reader = new StringArrayReader(lines);
    assertTrue(reader.ready());
  }

  @Test
  public void markSupported() {
    String[] lines = {};
    reader = new StringArrayReader(lines);
    assertTrue(reader.markSupported());
  }

  @Test
  public void mark() throws IOException {
    String[] lines = {";", "//NOSONAR",};
    reader = new StringArrayReader(lines);
    reader.read(new char[4], 0, 4);
    reader.mark(4);
    reader.read(new char[2], 0, 2);
    reader.reset();
    assertEquals('N', reader.read());
    assertEquals('O', reader.read());
  }

  @Test(expected = IOException.class)
  public void close() throws IOException {
    String[] lines = {";", "//NOSONAR",};
    reader = new StringArrayReader(lines);
    assertTrue(reader.ready());
    reader.close();
    reader.ready();
  }

  @Test
  public void readEndOfArray() throws IOException {
    String[] lines = {";"};
    reader = new StringArrayReader(lines);
    assertEquals(';', reader.read());
    assertEquals(-1, reader.read());
  }

  @Test
  public void readMultipleCharacters() throws IOException {
    String[] lines = {";", "//NOSONAR",};
    reader = new StringArrayReader(lines);
    char[] chars = new char[4];
    assertEquals(4, reader.read(chars, 0, 4));
    assertEquals(";\n//", new String(chars));
  }

  @Test
  public void readMultipleCharactersTillEndOfArray() throws IOException {
    String[] lines = {";", "//NOSONAR",};
    reader = new StringArrayReader(lines);
    char[] chars = new char[11];
    assertEquals(11, reader.read(chars, 0, 11));
    assertEquals(";\n//NOSONAR", new String(chars));
  }

  @Test
  public void readEmptyArray() throws IOException {
    String[] lines = {};
    reader = new StringArrayReader(lines);
    char[] cbuf = new char[10000];
    assertEquals(-1, reader.read(cbuf, 0, 10000));
  }

  @Test
  public void readMultipleCharactersWithEmptyLineAtEnd() throws IOException {
    String[] lines = {";", "//NOSONAR", "", ""};
    reader = new StringArrayReader(lines);
    char[] cbuf = new char[10000];
    assertEquals(13, reader.read(cbuf, 0, 10000));
    assertEquals(";\n//NOSONAR\n\n", new String(cbuf, 0, 13));
  }

  @Test
  public void readOneCharacter() throws IOException {
    String[] lines = {";", "//NOSONAR"};
    reader = new StringArrayReader(lines);
    char[] chars = new char[1];
    assertEquals(1, reader.read(chars, 0, 1));
    assertEquals(";", new String(chars));
  }

  @Test
  public void readBlankLines() throws IOException {
    String[] lines = {"", "", ""};
    reader = new StringArrayReader(lines);
    assertEquals('\n', reader.read());
    assertEquals('\n', reader.read());
    assertEquals(-1, reader.read());
  }

  @Test
  public void skip() throws IOException {
    String[] lines = {"//NOSONAR",};
    reader = new StringArrayReader(lines);
    reader.skip(2);
    assertEquals('N', reader.read());
  }

  @Test
  public void readEOF() throws IOException {
    String[] emptyLines = {};
    reader = new StringArrayReader(emptyLines);
    assertEquals(-1, reader.read());

    String[] lines = {"a"};
    reader = new StringArrayReader(lines);
    assertEquals('a', reader.read());
    assertEquals(-1, reader.read());
  }

}
