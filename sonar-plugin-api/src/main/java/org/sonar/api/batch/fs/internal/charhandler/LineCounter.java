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
package org.sonar.api.batch.fs.internal.charhandler;

import java.nio.charset.Charset;

import org.sonar.api.CoreProperties;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

public class LineCounter extends CharHandler {
  private static final Logger LOG = Loggers.get(LineCounter.class);
    
  private int lines = 1;
  private int nonBlankLines = 0;
  private boolean blankLine = true;
  boolean alreadyLoggedInvalidCharacter = false;
  private final String filePath;
  private final Charset encoding;

  public LineCounter(String filePath, Charset encoding) {
    this.filePath = filePath;
    this.encoding = encoding;
  }

  @Override
  public void handleAll(char c) {
    if (!alreadyLoggedInvalidCharacter && c == '\ufffd') {
      LOG.warn("Invalid character encountered in file {} at line {} for encoding {}. Please fix file content or configure the encoding to be used using property '{}'.", filePath,
        lines, encoding, CoreProperties.ENCODING_PROPERTY);
      alreadyLoggedInvalidCharacter = true;
    }
  }

  @Override
  public void newLine() {
    lines++;
    if (!blankLine) {
      nonBlankLines++;
    }
    blankLine = true;
  }

  @Override
  public void handleIgnoreEoL(char c) {
    if (!Character.isWhitespace(c)) {
      blankLine = false;
    }
  }

  @Override
  public void eof() {
    if (!blankLine) {
      nonBlankLines++;
    }
  }

  public int lines() {
    return lines;
  }

  public int nonBlankLines() {
    return nonBlankLines;
  }

}
