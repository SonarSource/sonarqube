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
package org.sonar.server.ws;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import org.apache.commons.io.IOUtils;

/**
 * Writer that writes only when closing the resource
 */
class CacheWriter extends Writer {
  private final StringWriter bufferWriter;
  private final Writer outputWriter;
  private boolean isClosed;

  CacheWriter(Writer outputWriter) {
    this.bufferWriter = new StringWriter();
    this.outputWriter = outputWriter;
    this.isClosed = false;
  }

  @Override
  public void write(char[] cbuf, int off, int len) {
    bufferWriter.write(cbuf, off, len);
  }

  @Override
  public void flush() {
    bufferWriter.flush();
  }

  @Override
  public void close() throws IOException {
    if (isClosed) {
      return;
    }

    IOUtils.write(bufferWriter.toString(), outputWriter);
    outputWriter.close();
    this.isClosed = true;
  }
}
