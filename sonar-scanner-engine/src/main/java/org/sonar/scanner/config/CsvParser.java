/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.scanner.config;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CsvParser {

  public static String[] parseLine(String line) {
    List<String> result = new ArrayList<>();

    AtomicInteger i = new AtomicInteger(0);
    while (true) {
      String cell = parseNextCell(line, i);
      if (cell == null)
        break;
      result.add(cell);
    }

    return result.toArray(new String[0]);
  }

  // returns iterator after delimiter or after end of string
  private static String parseNextCell(String line, AtomicInteger i) {
    if (i.get() >= line.length())
      return null;

    if (line.charAt(i.get()) != '"')
      return parseNotEscapedCell(line, i);
    else
      return parseEscapedCell(line, i);
  }

  // returns iterator after delimiter or after end of string
  private static String parseNotEscapedCell(String line, AtomicInteger i) {
    StringBuilder sb = new StringBuilder();
    while (true) {
      if (i.get() >= line.length()) {
        // return iterator after end of string
        break;
      }
      if (line.charAt(i.get()) == ',') {
        // return iterator after delimiter
        i.incrementAndGet();
        break;
      }
      sb.append(line.charAt(i.get()));
      i.incrementAndGet();
    }
    return sb.toString();
  }

  // returns iterator after delimiter or after end of string
  private static String parseEscapedCell(String line, AtomicInteger i) {
    i.incrementAndGet(); // omit first character (quotation mark)
    StringBuilder sb = new StringBuilder();
    while (true) {
      if (i.get() >= line.length()) {
        break;
      }
      if (line.charAt(i.get()) == '"') {
        i.incrementAndGet(); // we're more interested in the next character
        if (i.get() >= line.length()) {
          // quotation mark was closing cell
          // return iterator after end of string
          break;
        }
        if (line.charAt(i.get()) == ',') {
          // quotation mark was closing cell
          // return iterator after delimiter
          i.incrementAndGet();
          break;
        }
        if (line.charAt(i.get()) == '"') {
          // it was doubled (escaped) quotation mark
          // do nothing -- we've already skipped first quotation mark
        }

      }
      sb.append(line.charAt(i.get()));
      i.incrementAndGet();
    }

    return sb.toString();
  }
}
