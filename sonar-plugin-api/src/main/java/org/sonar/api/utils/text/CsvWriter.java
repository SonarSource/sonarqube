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
package org.sonar.api.utils.text;

import org.apache.commons.lang.StringEscapeUtils;

import java.io.IOException;
import java.io.Writer;

/**
 * @since 4.2
 */
public class CsvWriter {

  private final Writer writer;
  private static final String VALUE_SEPARATOR = ",";
  private static final String LINE_SEPARATOR = "\r\n";

  private CsvWriter(Writer writer) {
    this.writer = writer;
  }

  public static CsvWriter of(Writer writer) {
    return new CsvWriter(writer);
  }

  public CsvWriter values(String... values) {
    for (int index = 0; index < values.length; index++) {
      if (index > 0) {
        write(VALUE_SEPARATOR);
      }
      String value = values[index];
      if (value != null) {
        write(StringEscapeUtils.escapeCsv(value));
      }
      if (index == values.length - 1) {
        write(LINE_SEPARATOR);
      }
    }
    return this;
  }

  public void close() {
    try {
      writer.close();
    } catch (IOException e) {
      throw new WriterException("Fail to close CSV output", e);
    }
  }

  private void write(String s) {
    try {
      writer.append(s);
    } catch (IOException e) {
      throw new WriterException("Fail to generate CSV with value: " + s, e);
    }
  }
}
