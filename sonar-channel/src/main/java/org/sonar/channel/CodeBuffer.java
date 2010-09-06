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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.apache.commons.io.IOUtils;

/**
 * The CodeBuffer class provides all the basic features required to manipulate a source code character stream. 
 * Those features are :
 * <ul>
 * <li>Read and consume next source code character : pop()</li>
 * <li>Retrieve last consumed character : lastChar()</li>
 * <li>Read without consuming next source code character : peek()</li>
 * <li>Read without consuming character at the specified index after the cursor</li>
 * <li>Position of the pending cursor : line and column</li>
 * </ul>
 */
public class CodeBuffer implements CharSequence {

  private final Reader code;
  private int lastChar = -1;
  private Cursor cursor;
  private final static int DEFAULT_BUFFER_CAPACITY = 8000;
  private int bufferCapacity;
  private char[] buffer;
  private int bufferPosition = 0;
  private int bufferSize = 0;
  private static final char LF = '\n';
  private static final char CR = '\r';
  private boolean recordingMode = false;
  private StringBuilder recordedCharacters = new StringBuilder();

  public CodeBuffer(Reader code) {
    this(code, DEFAULT_BUFFER_CAPACITY);
  }

  private CodeBuffer(Reader code, int bufferCapacity) {
    this.code = code;
    lastChar = -1;
    cursor = new Cursor();
    this.bufferCapacity = bufferCapacity;
    buffer = new char[bufferCapacity];
    fillBuffer();
  }

  public CodeBuffer(String code) {
    this(new StringReader(code));
  }

  protected CodeBuffer(String code, int bufferCapacity) {
    this(new StringReader(code), bufferCapacity);
  }

  /**
   * Read and consume the next character
   * 
   * @return the next character or -1 if the end of the stream is reached
   */
  public final int pop() {
    if (bufferPosition == bufferSize) {
      fillBuffer();
    }
    if (bufferSize == 0) {
      return -1;
    }
    int character = buffer[bufferPosition++];
    if (character == LF || character == CR) {
      if ((lastChar != LF && lastChar != CR) || lastChar == character || lastChar == LF) {
        cursor.line++;
      }
      cursor.column = 0;
    } else {
      cursor.column++;
    }
    if (recordingMode) {
      recordedCharacters.append((char) character);
    }
    lastChar = character;
    return character;
  }

  private int fillBuffer() {
    try {
      int offset = bufferSize - bufferPosition;
      if (offset != 0) {
        System.arraycopy(buffer, bufferPosition, buffer, 0, bufferSize - bufferPosition);
      }
      bufferPosition = 0;
      int numberOfChars = code.read(buffer, offset, bufferCapacity - offset);
      if (numberOfChars == -1) {
        numberOfChars = 0;
      }
      bufferSize = numberOfChars + offset;
      return offset;
    } catch (IOException e) {
      throw new ChannelException(e.getMessage(), e);
    }
  }

  /**
   * Get the last consumed character
   * 
   * @return the last character or -1 if the no character has been yet consumed
   */
  public final int lastChar() {
    return lastChar;
  }

  /**
   * Read without consuming the next character
   * 
   * @return the next character or -1 if the end of the stream is reached
   */
  public final int peek() {
    return intAt(0);
  }

  /**
   * Close the stream
   */
  public final void close() {
    IOUtils.closeQuietly(code);
  }

  /**
   * @return the current line of the cursor
   */
  public final int getLinePosition() {
    return cursor.line;
  }

  public final Cursor getCursor() {
    return cursor;
  }

  /**
   * @return the current column of the cursor
   */
  public final int getColumnPosition() {
    return cursor.column;
  }

  /**
   * Overrides the current column position
   */
  public final CodeBuffer setColumnPosition(int cp) {
    this.cursor.column = cp;
    return this;
  }

  /**
   * Overrides the current line position
   */
  public final void setLinePosition(int lp) {
    this.cursor.line = lp;
  }

  public final void startRecording() {
    recordingMode = true;
  }

  public final CharSequence stopRecording() {
    recordingMode = false;
    CharSequence result = recordedCharacters;
    recordedCharacters = new StringBuilder();
    return result;
  }

  /**
   * Returns the character at the specified index after the cursor without consuming it
   * 
   * @param index
   *          the index of the character to be returned
   * @return the desired character
   * @see java.lang.CharSequence#charAt(int)
   */
  public final char charAt(int index) {
    return (char) intAt(index);
  }

  protected final int intAt(int index) {
    if (bufferPosition + index > bufferSize - 1) {
      fillBuffer();
    }
    if (bufferPosition + index > bufferSize - 1) {
      return -1;
    }
    return buffer[bufferPosition + index];
  }

  public final int length() {
    return bufferSize;
  }

  public final CharSequence subSequence(int start, int end) {
    throw new UnsupportedOperationException();
  }

  @Override
  public final String toString() {
    StringBuilder result = new StringBuilder();
    result.append("CodeReader(");
    result.append("line:" + cursor.line);
    result.append("|column:" + cursor.column);
    result.append("|cursor value:'" + (char) peek() + "'");
    result.append(")");
    return result.toString();
  }

  public final class Cursor {

    private int line = 1;
    private int column = 0;

    public int getLine() {
      return line;
    }

    public int getColumn() {
      return column;
    }

    public Cursor clone() {
      Cursor clone = new Cursor();
      clone.column = column;
      clone.line = line;
      return clone;
    }
  }
}
