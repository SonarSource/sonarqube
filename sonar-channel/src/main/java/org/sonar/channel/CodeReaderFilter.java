/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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

import java.io.IOException;
import java.io.Reader;

/**
 * This class can be extended to provide filtering capabilities for the CodeReader class. <br>
 * The purpose is to filter the character flow before the CodeReader class passes it to the different channels. It is possible to give
 * several filters to a CodeReader: they will be called one after another, following the declaration order in the CodeReader constructor, to
 * sequentially filter the character flow.
 * 
 * @see CodeReader
 * @see CodeBufferTest#testCodeReaderFilter()
 * @see CodeBufferTest#testSeveralCodeReaderFilter()
 * 
 */
public abstract class CodeReaderFilter<O> {

  private Reader reader;

  private O output;

  private CodeReaderConfiguration configuration;

  public CodeReaderFilter() {
  }

  public CodeReaderFilter(O output) {
    this.output = output;
  }

  /**
   * Returns the reader from which this class reads the character stream.
   * 
   * @return the reader
   */
  public Reader getReader() {
    return reader;
  }

  /**
   * Sets the reader from which this class will read the character stream.
   * 
   * @param reader
   *          the reader
   */
  public void setReader(Reader reader) {
    this.reader = reader;
  }

  /**
   * Returns the output object.
   * 
   * @return the output
   */
  public O getOutput() {
    return output;
  }

  /**
   * Sets the output object
   * 
   * @param output
   *          the output to set
   */
  public void setOutput(O output) {
    this.output = output;
  }

  /**
   * Returns the configuration used for the CodeReader
   * 
   * @return the configuration
   */
  public CodeReaderConfiguration getConfiguration() {
    return configuration;
  }

  /**
   * Sets the configuration that must be used by the CodeReader
   * 
   * @param configuration
   *          the configuration to set
   */
  public void setConfiguration(CodeReaderConfiguration configuration) {
    this.configuration = configuration;
  }

  /**
   * This method implements the filtering logic, that is:
   * <ul>
   * <li>
   * get the characters from the reader,</li>
   * <li>
   * filter the character flow (and grab more characters from the reader if the filtering removes some),</li>
   * <li>
   * and fill the given buffer to its full capacity with the filtered data.</li>
   * </ul>
   * 
   * @param filteredBuffer
   *          the output buffer that must contain the filtered data
   * @param offset
   *          the offset to start reading from the reader
   * @param length
   *          the number of characters to read from the reader
   * @return The number of characters read, or -1 if the end of the stream has been reached
   * @throws IOException
   *           If an I/O error occurs
   */
  public abstract int read(char[] filteredBuffer, int offset, int length) throws IOException;

}
