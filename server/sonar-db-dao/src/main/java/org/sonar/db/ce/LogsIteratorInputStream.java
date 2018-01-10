/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.db.ce;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import org.sonar.core.util.CloseableIterator;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * An {@link InputStream} that will read from a {@link CloseableIterator} of {@link String}, inserting {@code \n} between
 * each element of the Iterator.
 */
final class LogsIteratorInputStream extends InputStream {
  private static final int UNSET = -1;
  private static final int END_OF_STREAM = -1;

  private final Charset charset;
  private final byte[] lineFeed;
  private CloseableIterator<String> logsIterator;
  private byte[] buf;
  private int nextChar = UNSET;

  LogsIteratorInputStream(CloseableIterator<String> logsIterator, Charset charset) {
    checkArgument(logsIterator.hasNext(), "LogsIterator can't be empty or already read");
    this.charset = charset;
    this.lineFeed = "\n".getBytes(charset);
    this.logsIterator = logsIterator;
  }

  @Override
  public int read() {
    if (nextChar == UNSET || nextChar >= buf.length) {
      fill();
      if (nextChar == UNSET) {
        return END_OF_STREAM;
      }
    }
    return buf[nextChar++];
  }

  private void fill() {
    if (logsIterator.hasNext()) {
      byte[] line = logsIterator.next().getBytes(charset);
      boolean hasNextLine = logsIterator.hasNext();
      int bufLength = hasNextLine ? (line.length + lineFeed.length) : line.length;
      // empty last line
      if (bufLength == 0) {
        this.buf = null;
        this.nextChar = UNSET;
      } else {
        this.buf = new byte[bufLength];
        System.arraycopy(line, 0, buf, 0, line.length);
        if (hasNextLine) {
          System.arraycopy(lineFeed, 0, buf, line.length, lineFeed.length);
        }
        this.nextChar = 0;
      }
    } else {
      this.buf = null;
      this.nextChar = UNSET;
    }
  }

  @Override
  public void close() throws IOException {
    this.logsIterator.close();
    this.buf = null;

    super.close();
  }
}
