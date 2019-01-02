/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.core.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

/**
 * Read lines from a {@link Reader}
 * @see BufferedReader
 */
public class LineReaderIterator extends CloseableIterator<String> {

  private final BufferedReader reader;

  public LineReaderIterator(Reader reader) {
    if (reader instanceof BufferedReader) {
      this.reader = (BufferedReader) reader;
    } else {
      this.reader = new BufferedReader(reader);
    }
  }

  @Override
  protected String doNext() {
    try {
      return reader.readLine();
    } catch (IOException e) {
      throw new IllegalStateException("Fail to read line", e);
    }
  }

  @Override
  protected void doClose() throws IOException {
    reader.close();
  }
}
