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

/**
 * This class is a special CodeReaderFilter that uses Channels to filter the character stream before it is passed to the main channels
 * declared for the CodeReader.
 * 
 */
public final class ChannelCodeReaderFilter<O> extends CodeReaderFilter<O> {

  @SuppressWarnings("unchecked")
  private Channel<O>[] channels = new Channel[0];

  private CodeReader internalCodeReader;

  /**
   * Creates a CodeReaderFilter that will use the provided Channels to filter the character stream it gets from its reader.
   * 
   * @param channels
   *          the different channels
   */
  public ChannelCodeReaderFilter(Channel<O>... channels) {
    super();
    this.channels = channels;
  }

  /**
   * Creates a CodeReaderFilter that will use the provided Channels to filter the character stream it gets from its reader. And optionally,
   * it can push token to the provided output object.
   * 
   * @param output
   *          the object that may accept tokens
   * @param channels
   *          the different channels
   */
  public ChannelCodeReaderFilter(O output, Channel<O>... channels) {
    super(output);
    this.channels = channels;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setReader(Reader reader) {
    super.setReader(reader);
    internalCodeReader = new CodeReader(reader, getConfiguration());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int read(char[] filteredBuffer, int offset, int length) throws IOException {
    if (internalCodeReader.peek() == -1) {
      return -1;
    }
    int initialOffset = offset;
    while (offset < filteredBuffer.length) {
      if (internalCodeReader.peek() == -1) {
        break;
      }
      boolean consumed = false;
      for (Channel<O> channel : channels) {
        if (channel.consume(internalCodeReader, getOutput())) {
          consumed = true;
          break;
        }
      }
      if ( !consumed) {
        int charRead = internalCodeReader.pop();
        filteredBuffer[offset++] = (char) charRead;
      }
    }
    return offset - initialOffset;
  }

}
