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

package org.sonar.batch.protocol;

import com.google.protobuf.Message;
import com.google.protobuf.Parser;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An object to iterate over protobuf messages in a file.
 * A ReportStream is opened upon creation and is closed by invoking the close method.
 *
 * Warning, while it extends Iterable, it is not a general-purpose Iterable as it supports only a single Iterator;
 * invoking the iterator method to obtain a second or subsequent iterator throws IllegalStateException.
 *
 * Inspired by {@link java.nio.file.DirectoryStream}
 */
public class ReportStream<R extends Message> implements Closeable, Iterable<R> {

  private final Parser<R> parser;
  private InputStream inputStream;
  private ReportIterator<R> iterator;

  public ReportStream(File file, Parser<R> parser) {
    this.parser = parser;
    this.inputStream = ProtobufUtil.createInputStream(file);
  }

  @Override
  public Iterator<R> iterator() {
    if (this.iterator != null) {
      throw new IllegalStateException("Iterator already obtained");
    } else {
      this.iterator = new ReportIterator<>(inputStream, parser);
      return this.iterator;
    }
  }

  @Override
  public void close() throws IOException {
    inputStream.close();
  }

  public static class ReportIterator<R extends Message> implements Iterator<R> {

    private final Parser<R> parser;
    private InputStream inputStream;
    private R currentMessage;

    public ReportIterator(InputStream inputStream, Parser<R> parser) {
      this.inputStream = inputStream;
      this.parser = parser;
    }

    @Override
    public boolean hasNext() {
      if (currentMessage == null) {
        currentMessage = ProtobufUtil.readInputStream(inputStream, parser);
      }
      return currentMessage != null;
    }

    @Override
    public R next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      R messageToReturn = currentMessage;
      currentMessage = null;
      return messageToReturn;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }
}
